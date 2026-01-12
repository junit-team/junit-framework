/*
 * Copyright 2015-2026 the original author or authors.
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v2.0 which
 * accompanies this distribution and is available at
 *
 * https://www.eclipse.org/legal/epl-v20.html
 */

package org.junit.jupiter.api.util;

import static java.util.stream.Collectors.toSet;
import static org.junit.platform.commons.support.AnnotationSupport.findRepeatableAnnotations;
import static org.junit.platform.commons.support.AnnotationSupport.isAnnotated;
import static org.junit.platform.commons.util.CollectionUtils.forEachInReverseOrder;

import java.lang.reflect.AnnotatedElement;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Properties;
import java.util.Set;
import java.util.stream.Stream;

import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.extension.AfterAllCallback;
import org.junit.jupiter.api.extension.AfterEachCallback;
import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.BeforeEachCallback;
import org.junit.jupiter.api.extension.ExtensionConfigurationException;
import org.junit.jupiter.api.extension.ExtensionContext;

/**
 * @since 6.1
 */
final class SystemPropertyExtension
		implements BeforeEachCallback, AfterEachCallback, BeforeAllCallback, AfterAllCallback {

	/**
	 * Prepare for entering a context that must be restorable.
	 *
	 * <p>The context is prepared by pre-emptively swapping out the current
	 * System properties with a snapshot. The original system properties are
	 * restored after the test.
	 *
	 * @return The original {@link System#getProperties} object
	 */
	Properties prepareToEnterRestorableContext(ExtensionContext context) {
		var current = System.getProperties();
		var clone = JupiterPropertyUtils.cloneWithoutDefaults(context, current);
		System.setProperties(clone);
		return current;
	}

	/**
	 * Prepare to exit a restorable context.
	 *
	 * <p>The entry environment will be restored to the state passed in as {@code Properties}.
	 *
	 * @param properties a non-null {@code Properties} that contains all entries of the entry environment.
	 */
	void prepareToExitRestorableContext(Properties properties) {
		System.setProperties(properties);
	}

	@Override
	public void beforeAll(ExtensionContext context) {
		applyForAllContexts(context);
	}

	@Override
	public void beforeEach(ExtensionContext context) {
		applyForAllContexts(context);
	}

	private void applyForAllContexts(ExtensionContext originalContext) {
		var allContexts = findAllExtensionContexts(originalContext);

		var restoreAnnotationContext = findFirstRestoreAnnotationContext(allContexts);
		restoreAnnotationContext.ifPresent(annotatedElementContext -> {
			var properties = this.prepareToEnterRestorableContext(annotatedElementContext);
			storeCompleteBackup(originalContext, properties);
		});

		// we have to apply the annotations from the outermost to the innermost context.
		forEachInReverseOrder(allContexts,
			currentContext -> clearAndSetEntries(currentContext, originalContext, restoreAnnotationContext.isEmpty()));
	}

	private Optional<ExtensionContext> findFirstRestoreAnnotationContext(List<ExtensionContext> contexts) {
		return contexts.stream() //
				.filter(context -> isAnnotated(context.getElement(), RestoreSystemProperties.class)) //
				.findFirst();
	}

	private void clearAndSetEntries(ExtensionContext currentContext, ExtensionContext originalContext,
			boolean doIncrementalBackup) {
		currentContext.getElement().ifPresent(element -> {
			var entriesToClear = findEntriesToClear(element);
			var entriesToSet = findEntriesToSet(element);
			preventClearAndSetSameEntries(element, entriesToClear, entriesToSet.keySet());

			if (entriesToClear.isEmpty() && entriesToSet.isEmpty())
				return;

			// Only backup original values if we didn't already do bulk storage of the original state
			if (doIncrementalBackup) {
				storeIncrementalBackup(originalContext, entriesToClear, entriesToSet.keySet());
			}

			// For consistency don't use Properties::setProperty or System.setProperty here
			var properties = System.getProperties();
			entriesToClear.forEach(properties::remove);
			properties.putAll(entriesToSet);
		});
	}

	private Set<String> findEntriesToClear(AnnotatedElement element) {
		return findRepeatableAnnotations(element, ClearSystemProperty.class).stream() //
				.map(ClearSystemProperty::key) //
				// already distinct due to findRepeatableAnnotations
				.collect(toSet());
	}

	private Map<String, String> findEntriesToSet(AnnotatedElement element) {
		var entries = new HashMap<String, String>();
		var duplicatePropertyNames = new HashSet<String>();

		findRepeatableAnnotations(element, SetSystemProperty.class) //
				.forEach(annotation -> {
					var key = annotation.key();
					if (entries.put(key, annotation.value()) != null) {
						duplicatePropertyNames.add(key);
					}
				});

		requireUniqueEntries(element, duplicatePropertyNames);
		return entries;
	}

	private void preventClearAndSetSameEntries(AnnotatedElement element, Set<String> entriesToClear,
			Set<String> entriesToSet) {
		requireUniqueEntries(element, //
			entriesToClear.stream() //
					.filter(entriesToSet::contains) //
					.collect(toSet()));
	}

	private static void requireUniqueEntries(AnnotatedElement annotatedElement, Set<String> duplicatePropertNames) {
		if (duplicatePropertNames.isEmpty()) {
			return;
		}
		throw new ExtensionConfigurationException(
			"SystemPropertyExtension was configured to set/clear %s [%s] more than once by [%s]." //
					.formatted( //
						duplicatePropertNames.size() == 1 ? "property" : "properties", //
						String.join(", ", duplicatePropertNames), //
						annotatedElement //
					));
	}

	private void storeIncrementalBackup(ExtensionContext context, Collection<String> entriesToClear,
			Collection<String> entriesToSet) {
		var backup = new EntriesBackup(entriesToClear, entriesToSet);
		getStore(context).put(getStoreKey(context, BackupType.INCREMENTAL), backup);
	}

	private void storeCompleteBackup(ExtensionContext context, Properties backup) {
		getStore(context).put(getStoreKey(context, BackupType.COMPLETE), backup);
	}

	/**
	 * Restore the complete original state of the entries as they were prior to this {@code ExtensionContext},
	 * if the complete state was initially stored in a before all/each event.
	 *
	 * @param context The {@code ExtensionContext} which may have a bulk backup stored.
	 * @return true if a complete backup exists and was used to restore, false if not.
	 */
	private boolean restoreOriginalCompleteBackup(ExtensionContext context) {
		var backup = getCompleteBackup(context);
		if (backup != null) {
			prepareToExitRestorableContext(backup);
			return true;
		}
		// No complete backup - false will let the caller know to continue w/ an incremental restore
		return false;
	}

	private @Nullable Properties getCompleteBackup(ExtensionContext context) {
		var key = getStoreKey(context, BackupType.COMPLETE);
		return getStore(context).get(key, Properties.class);
	}

	@Override
	public void afterEach(ExtensionContext context) {
		restoreForAllContexts(context);
	}

	@Override
	public void afterAll(ExtensionContext context) {
		restoreForAllContexts(context);
	}

	private void restoreForAllContexts(ExtensionContext originalContext) {
		// Try a complete restore first
		if (!restoreOriginalCompleteBackup(originalContext)) {
			// A complete backup is not available, so restore incrementally from innermost to outermost
			findAllExtensionContexts(originalContext).forEach(__ -> restoreOriginalIncrementalBackup(originalContext));
		}
	}

	private void restoreOriginalIncrementalBackup(ExtensionContext originalContext) {
		var backup = getIncrementalBackup(originalContext);
		if (backup != null) {
			backup.restoreBackup();
		}
	}

	private static List<ExtensionContext> findAllExtensionContexts(ExtensionContext context) {
		var contexts = new ArrayList<ExtensionContext>();
		do {
			contexts.add(context);
			context = context.getParent().orElse(null);
		} while (context != null);
		return contexts;
	}

	private @Nullable EntriesBackup getIncrementalBackup(ExtensionContext originalContext) {
		var key = getStoreKey(originalContext, BackupType.INCREMENTAL);
		return getStore(originalContext).get(key, EntriesBackup.class);
	}

	private ExtensionContext.Store getStore(ExtensionContext context) {
		return context.getStore(ExtensionContext.Namespace.create(getClass()));
	}

	private StoreKey getStoreKey(ExtensionContext context, BackupType type) {
		return new StoreKey(context.getUniqueId(), type);
	}

	private record StoreKey(String uniqueId, BackupType type) {
	}

	private enum BackupType {
		/**
		 * Store entry is for an incremental backup object.
		 */
		INCREMENTAL,
		/**
		 * Store entry is for a complete backup object.
		 */
		COMPLETE
	}

	private static final class EntriesBackup {

		private final Set<String> entriesToClear = new HashSet<>();
		private final Map<String, Object> entriesToSet = new HashMap<>();

		EntriesBackup(Collection<String> entriesToClear, Collection<String> entriesToSet) {
			var properties = System.getProperties();
			Stream.concat(entriesToClear.stream(), entriesToSet.stream()).forEach(entry -> {
				// Do not use Properties::getProperty or System.getProperty here,
				// this would prevent backing up non-string values.
				Object backup = properties.get(entry);
				if (backup == null)
					this.entriesToClear.add(entry);
				else
					this.entriesToSet.put(entry, backup);
			});
		}

		void restoreBackup() {
			// We can't use Properties::setProperty or System.setProperty here,
			// this would prevent restoring non-string values.
			var properties = System.getProperties();
			entriesToClear.forEach(properties::remove);
			properties.putAll(entriesToSet);
		}

	}
}
