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
import static org.junit.jupiter.api.util.JupiterPropertyUtils.cloneWithoutDefaults;
import static org.junit.platform.commons.support.AnnotationSupport.findRepeatableAnnotations;
import static org.junit.platform.commons.support.AnnotationSupport.isAnnotated;
import static org.junit.platform.commons.util.CollectionUtils.forEachInReverseOrder;

import java.lang.reflect.AnnotatedElement;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Properties;
import java.util.Set;

import org.junit.jupiter.api.extension.AfterAllCallback;
import org.junit.jupiter.api.extension.AfterEachCallback;
import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.BeforeEachCallback;
import org.junit.jupiter.api.extension.ExtensionConfigurationException;
import org.junit.jupiter.api.extension.ExtensionContext;

/**
 * {@code Extension} which provides support for the following annotations.
 *
 * <ul>
 * <li>{@link SetSystemProperty @SetSystemProperty}</li>
 * <li>{@link ClearSystemProperty @ClearSystemProperty}</li>
 * <li>{@link RestoreSystemProperties @RestoreSystemProperties}</li>
 * </ul>
 *
 * @since 6.1
 */
final class SystemPropertiesExtension
		implements BeforeEachCallback, AfterEachCallback, BeforeAllCallback, AfterAllCallback {

	@Override
	public void beforeAll(ExtensionContext context) {
		applyForAllContexts(context);
	}

	@Override
	public void beforeEach(ExtensionContext context) {
		applyForAllContexts(context);
	}

	private void applyForAllContexts(ExtensionContext context) {
		var allContexts = findAllExtensionContexts(context);
		var modification = DeferredPropertyModification.create(allContexts);

		// Please do not refactor out the common parts.
		findFirstRestoreAnnotationContext(allContexts) //
				.ifPresentOrElse(
					// Do a complete backup of the properties
					restoreAnnotationContext -> {
						var properties = System.getProperties();
						storeCompleteBackup(context, properties);
						var clonedProperties = cloneWithoutDefaults(restoreAnnotationContext, properties);
						modification.applyTo(clonedProperties);
						System.setProperties(clonedProperties);
					},
					// Backup only the modified properties
					() -> {
						var properties = System.getProperties();
						storePartialBackup(context, modification.createInverseApplyTo(properties));
						modification.applyTo(properties);
					});
	}

	private void storeCompleteBackup(ExtensionContext context, Properties backup) {
		getStore(context).put(createStoreKey(context, BackupType.COMPLETE), backup);
	}

	private void storePartialBackup(ExtensionContext context, DeferredPropertyModification backup) {
		getStore(context).put(createStoreKey(context, BackupType.PARTIAL), backup);
	}

	@Override
	public void afterEach(ExtensionContext context) {
		restoreForAllContexts(context);
	}

	@Override
	public void afterAll(ExtensionContext context) {
		restoreForAllContexts(context);
	}

	private void restoreForAllContexts(ExtensionContext context) {
		// Try a complete restore first
		findCompleteBackup(context) //
				.ifPresentOrElse(System::setProperties, //
						// A complete backup is not available, so use partial backup
					() -> findPartialBackup(context) //
							.ifPresent(backup -> backup.applyTo(System.getProperties())));
	}

	private Optional<Properties> findCompleteBackup(ExtensionContext context) {
		var key = createStoreKey(context, BackupType.COMPLETE);
		var backup = getStore(context).get(key, Properties.class);
		return Optional.ofNullable(backup);
	}

	private Optional<DeferredPropertyModification> findPartialBackup(ExtensionContext context) {
		var key = createStoreKey(context, BackupType.PARTIAL);
		var backup = getStore(context).get(key, DeferredPropertyModification.class);
		return Optional.ofNullable(backup);
	}

	private StoreKey createStoreKey(ExtensionContext context, BackupType type) {
		return new StoreKey(context.getUniqueId(), type);
	}

	private ExtensionContext.Store getStore(ExtensionContext context) {
		return context.getStore(ExtensionContext.Namespace.create(getClass()));
	}

	private record StoreKey(String id, BackupType type) {
	}

	private enum BackupType {
		/**
		 * Store entry is for a partial backup object.
		 */
		PARTIAL,
		/**
		 * Store entry is for a complete backup object.
		 */
		COMPLETE
	}

	/**
	 * A sequence of deferred modification applied to a
	 * {@link Properties} object represented as a single modification.
	 */
	private static class DeferredPropertyModification {
		private static final Object REMOVED = new Object();
		private final Map<String, Object> changes = new HashMap<>();

		void clearProperty(String key) {
			changes.put(key, REMOVED);
		}

		void setProperty(String key, Object value) {
			changes.put(key, value);
		}

		void applyTo(Properties properties) {
			changes.forEach((key, value) -> {
				// For consistency don't use Properties::setProperty here
				if (REMOVED.equals(value)) {
					properties.remove(key);
				}
				else {
					properties.put(key, value);
				}
			});
		}

		/**
		 * Creates the inverse of calling {@link #applyTo(Properties)} such
		 * that {@code modification.applyTo(properties); inverse.applyTo(properties);}
		 * has no observable effect.
		 */
		DeferredPropertyModification createInverseApplyTo(Properties properties) {
			DeferredPropertyModification inverse = new DeferredPropertyModification();
			changes.keySet().forEach(key -> {
				// Do not use Properties::getProperty here, since this would
				// prevent backing up non-string values.
				Object backup = properties.get(key);
				if (backup == null) {
					inverse.clearProperty(key);
				}
				else {
					inverse.setProperty(key, backup);
				}
			});
			return inverse;
		}

		static DeferredPropertyModification create(List<ExtensionContext> allContexts) {
			var modification = new DeferredPropertyModification();
			// we have to apply the annotations from the outermost to the innermost context.
			forEachInReverseOrder(allContexts, currentContext -> currentContext.getElement().ifPresent(element -> {
				var entriesToClear = findEntriesToClear(element);
				var entriesToSet = findEntriesToSet(element);

				if (entriesToClear.isEmpty() && entriesToSet.isEmpty()) {
					return;
				}

				requireNoClearAndSetSameEntries(element, entriesToClear, entriesToSet.keySet());
				entriesToClear.forEach(modification::clearProperty);
				entriesToSet.forEach(modification::setProperty);
			}));
			return modification;
		}

		private static Set<String> findEntriesToClear(AnnotatedElement element) {
			return findRepeatableAnnotations(element, ClearSystemProperty.class).stream() //
					.map(ClearSystemProperty::key) //
					// already distinct due to findRepeatableAnnotations
					.collect(toSet());
		}

		private static Map<String, String> findEntriesToSet(AnnotatedElement element) {
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

		private static void requireNoClearAndSetSameEntries(AnnotatedElement element, Set<String> entriesToClear,
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
	}

	private static List<ExtensionContext> findAllExtensionContexts(ExtensionContext context) {
		var contexts = new ArrayList<ExtensionContext>();
		do {
			contexts.add(context);
			context = context.getParent().orElse(null);
		} while (context != null);
		return contexts;
	}

	private static Optional<ExtensionContext> findFirstRestoreAnnotationContext(List<ExtensionContext> contexts) {
		return contexts.stream() //
				.filter(context -> isAnnotated(context.getElement(), RestoreSystemProperties.class)) //
				.findFirst();
	}
}
