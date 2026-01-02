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

import java.util.Properties;
import java.util.function.Function;

import org.jspecify.annotations.Nullable;

/**
 * @since 6.1
 */
final class SystemPropertyExtension extends
		AbstractEntryBasedExtension<String, String, ClearSystemProperty, SetSystemProperty, RestoreSystemProperties> {

	@Override
	protected Function<ClearSystemProperty, String> clearKeyMapper() {
		return ClearSystemProperty::key;
	}

	@Override
	protected Function<SetSystemProperty, String> setKeyMapper() {
		return SetSystemProperty::key;
	}

	@Override
	protected Function<SetSystemProperty, String> setValueMapper() {
		return SetSystemProperty::value;
	}

	@Override
	protected void clearEntry(String key) {
		System.clearProperty(key);
	}

	@Override
	protected @Nullable String getEntry(String key) {
		return System.getProperty(key);
	}

	@Override
	protected void setEntry(String key, String value) {
		System.setProperty(key, value);
	}

	/**
	 * This implementation uses the "Preemptive swap" strategy.
	 *
	 * <p>Since {@link Properties} allows a wrapped default instance and Object values,
	 * cloning is difficult:</p>
	 *
	 * <ul>
	 * <li>It is difficult to tell which values are defaults and which are "top level",
	 * thus a clone might contain the same effective values, but be flattened without defaults.</li>
	 * <li>Object values in a wrapped default instance cannot be accessed without reflection.</li>
	 * </ul>
	 *
	 * <p>The "Preemptive swap" strategy ensure that the original Properties are restored, however
	 * complex they were. Any artifacts resulting from a flattened default structure are limited
	 * to the context of the test.</p>
	 *
	 * <p>See {@link AbstractEntryBasedExtension#prepareToEnterRestorableContext} for more details.</p>
	 *
	 * @return The original {@link System#getProperties} object
	 */
	@Override
	protected Properties prepareToEnterRestorableContext() {
		Properties current = System.getProperties();
		Properties clone = createEffectiveClone(current);

		System.setProperties(clone);

		return current;
	}

	@Override
	protected void prepareToExitRestorableContext(Properties properties) {
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

}
