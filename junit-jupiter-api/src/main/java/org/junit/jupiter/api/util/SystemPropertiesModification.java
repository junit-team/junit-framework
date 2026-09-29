/*
 * Copyright 2026 the original author or authors.
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
import static org.junit.platform.commons.util.CollectionUtils.forEachInReverseOrder;

import java.lang.reflect.AnnotatedElement;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import org.junit.jupiter.api.extension.ExtensionConfigurationException;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.platform.commons.util.ToStringBuilder;

/**
 * A sequence of modifications applied to a {@link Properties}
 * object represented as a single modification.
 */
class SystemPropertiesModification {
	private static final Object REMOVED = new Object();
	private final Map<String, Object> changes = new HashMap<>();

	private SystemPropertiesModification() {
		/* no-op */
	}

	private void clearProperty(String key) {
		changes.put(key, REMOVED);
	}

	private void setProperty(String key, Object value) {
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
	SystemPropertiesModification createInverseApplyTo(Properties properties) {
		SystemPropertiesModification inverse = new SystemPropertiesModification();
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

	@Override
	public String toString() {
		var builder = new ToStringBuilder(SystemPropertiesModification.class);
		this.changes.forEach((key, value) -> {
			if (REMOVED.equals(value)) {
				builder.append(key, null);
			}
			else {
				builder.append(key, value);
			}
		});
		return builder.toString();
	}

	static SystemPropertiesModification create(List<ExtensionContext> allContexts) {
		var modification = new SystemPropertiesModification();
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
