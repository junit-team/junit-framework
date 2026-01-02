/*
 * Copyright 2015-2025 the original author or authors.
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v2.0 which
 * accompanies this distribution and is available at
 *
 * https://www.eclipse.org/legal/epl-v20.html
 */

package org.junit.jupiter.api.util;

import static java.util.stream.Collectors.joining;
import static java.util.stream.Collectors.toMap;
import static org.junit.jupiter.api.util.SystemPropertyExtensionUtils.findAllContexts;
import static org.junit.platform.commons.support.AnnotationSupport.findAnnotation;

import java.lang.annotation.Annotation;
import java.lang.reflect.AnnotatedElement;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
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
import org.junit.platform.commons.support.AnnotationSupport;

/**
 * @since 6.1
 */
final class SystemPropertyExtension
		implements BeforeEachCallback, AfterEachCallback, BeforeAllCallback, AfterAllCallback {

	/**
	 * Prepare for entering a context that must be restorable.
	 *
	 * <p>Since {@link Properties} allows a wrapped default instance and Object values,
	 * cloning is difficult:
	 *
	 * <ul>
	 * <li>It is difficult to tell which values are defaults and which are "top level",
	 * thus a clone might contain the same effective values, but be flattened without defaults.</li>
	 * <li>Object values in a wrapped default instance cannot be accessed without reflection.</li>
	 * </ul>
	 *
	 * <p>The "Preemptive swap" strategy ensure that the original Properties are restored, however
	 * complex they were. Any artifacts resulting from a flattened default structure are limited
	 * to the context of the test.
	 *
	 * @return The original {@link System#getProperties} object
	 */
	Properties prepareToEnterRestorableContext() {
		Properties current = System.getProperties();
		Properties clone = createEffectiveClone(current);

		System.setProperties(clone);

		return current;
	}

	/**
	 * Prepare to exit a restorable context.
	 *
	 * <p>The entry environment will be restored to the state passed in as {@code Properties}.
	 * The {@code Properties} entries must follow the rules for entries of this environment,
	 * e.g., environment variables contain only Strings while System {@code Properties} may contain Objects.</p>
	 *
	 * @param properties a non-null {@code Properties} that contains all entries of the entry environment.
	 */
	void prepareToExitRestorableContext(Properties properties) {
		System.setProperties(properties);
	}

	/**
	 * A clone of the String values of the passed {@code Properties}, including defaults.
	 *
	 * <p>The clone will have the same effective values, but may not use the same nested
	 * structure as the original. Object values, which are technically possible,
	 * are not included in the clone.</p>
	 *
	 * @param original {@code Properties} to be cloned.
	 * @return A new {@code Properties} instance containing the same effective entries as the original.
	 */
	static Properties createEffectiveClone(Properties original) {
		Properties clone = new Properties();

		// This implementation is used because:
		// System.getProperties() returns the actual Properties object, not a copy.
		// Clone doesn't include nested defaults, but propertyNames() does.
		original.propertyNames().asIterator().forEachRemaining(k -> {
			String v = original.getProperty(k.toString());

			if (v != null) {
				// v will be null if the actual value was an object
				clone.put(k, original.getProperty(k.toString()));
			}
		});

		return clone;
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
		if (isRestoreAnnotationPresent(originalContext)) {
			var properties = this.prepareToEnterRestorableContext();
			storeCompleteBackup(originalContext, properties);
		}

		/*
		 * We cannot use PioneerAnnotationUtils#findAllEnclosingRepeatableAnnotations(ExtensionContext, Class) or the
		 * like as clearing and setting might interfere. Therefore, we have to apply the extension from the outermost
		 * to the innermost ExtensionContext.
		 */
		List<ExtensionContext> contexts = findAllContexts(originalContext);
		Collections.reverse(contexts);
		contexts.forEach(currentContext -> clearAndSetEntries(currentContext, originalContext,
			!isRestoreAnnotationPresent(originalContext)));
	}

	private boolean isRestoreAnnotationPresent(ExtensionContext originalContext) {
		// TODO: Can we do this with more grace?
		return findAnnotation(originalContext.getElement(), RestoreSystemProperties.class).isPresent()
				|| findAnnotation(originalContext.getRequiredTestClass(), RestoreSystemProperties.class,
					originalContext.getEnclosingTestClasses()).isPresent();
	}

	private void clearAndSetEntries(ExtensionContext currentContext, ExtensionContext originalContext,
			boolean doIncrementalBackup) {
		currentContext.getElement().ifPresent(element -> {
			Set<String> entriesToClear;
			Map<String, String> entriesToSet;

			try {
				entriesToClear = findEntriesToClear(element);
				entriesToSet = findEntriesToSet(element);
				preventClearAndSetSameEntries(entriesToClear, entriesToSet.keySet());
			}
			catch (IllegalStateException ex) {
				throw new ExtensionConfigurationException("Don't clear/set the same entry more than once.", ex);
			}

			if (entriesToClear.isEmpty() && entriesToSet.isEmpty())
				return;

			// Only backup original values if we didn't already do bulk storage of the original state
			if (doIncrementalBackup) {
				storeIncrementalBackup(originalContext, entriesToClear, entriesToSet.keySet());
			}

			entriesToClear.forEach(System::clearProperty);
			entriesToSet.forEach(System::setProperty);
		});
	}

	private Set<String> findEntriesToClear(AnnotatedElement element) {
		return findAnnotations(element, ClearSystemProperty.class) //
				.map(ClearSystemProperty::key) //
				.collect(SystemPropertyExtensionUtils.distinctToSet());
	}

	private Map<String, String> findEntriesToSet(AnnotatedElement element) {
		return findAnnotations(element, SetSystemProperty.class) //
				.collect(toMap(SetSystemProperty::key, SetSystemProperty::value));
	}

	private <A extends Annotation> Stream<A> findAnnotations(AnnotatedElement element, Class<A> clazz) {
		return AnnotationSupport.findRepeatableAnnotations(element, clazz).stream();
	}

	private void preventClearAndSetSameEntries(Collection<String> entriesToClear, Collection<String> entriesToSet) {
		String duplicateEntries = entriesToClear.stream().filter(entriesToSet::contains).map(Object::toString).collect(
			joining(", "));
		if (!duplicateEntries.isEmpty())
			throw new IllegalStateException(
				"Cannot clear and set the following entries at the same time: " + duplicateEntries);
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
			findAllContexts(originalContext).forEach(__ -> restoreOriginalIncrementalBackup(originalContext));
		}
	}

	private void restoreOriginalIncrementalBackup(ExtensionContext originalContext) {
		var backup = getIncrementalBackup(originalContext);
		if (backup != null) {
			backup.restoreBackup();
		}
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
		private final Map<String, String> entriesToSet = new HashMap<>();

		EntriesBackup(Collection<String> entriesToClear, Collection<String> entriesToSet) {
			Stream.concat(entriesToClear.stream(), entriesToSet.stream()).forEach(entry -> {
				String backup = System.getProperty(entry);
				if (backup == null)
					this.entriesToClear.add(entry);
				else
					this.entriesToSet.put(entry, backup);
			});
		}

		void restoreBackup() {
			entriesToClear.forEach(System::clearProperty);
			entriesToSet.forEach(System::setProperty);
		}

	}
}
