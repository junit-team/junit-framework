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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.junit.jupiter.api.util.JupiterPropertyUtils.cloneWithoutDefaults;
import static org.mockito.Mockito.when;

import java.io.Serial;
import java.util.Optional;
import java.util.Properties;

import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtensionConfigurationException;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoSettings;

/**
 * Tests for {@link JupiterPropertyUtils}.
 */
@MockitoSettings
class JupiterPropertyUtilsTests {

	@Mock
	ExtensionContext context;

	@Test
	void cloneProperties() {
		var properties = new Properties();
		properties.setProperty("a", "a");
		var value = new Object();
		properties.put("a-obj", value);

		var clone = cloneWithoutDefaults(context, properties);

		assertThat(clone.stringPropertyNames()).containsExactly("a");
		assertThat(clone.get("a")).isSameAs(properties.get("a"));

		assertThat(clone.keySet()).containsExactly("a", "a-obj");
		assertThat(clone.getProperty("a")).isEqualTo("a");
		assertThat(clone.get("a-obj")).isSameAs(value);
	}

	@Test
	void withDefaults() {
		var defaults = new Properties();
		defaults.setProperty("a", "a");

		var properties = new Properties(defaults);
		properties.setProperty("X", "X");

		when(context.getElement()).thenReturn(Optional.of(JupiterPropertyUtilsTests.class));

		assertThatExceptionOfType(ExtensionConfigurationException.class) //
				.isThrownBy(() -> cloneWithoutDefaults(context, properties))//
				.withMessage("""
						SystemPropertiesExtension was configured to restore the system properties by [%s]. \
						However, it was not possible to create an accurate snapshot of the system properties \
						using Properties::clone, because default properties were present: [a]""",
					JupiterPropertyUtilsTests.class);
	}

	@Test
	void withDefaultsAndSubclass() {
		var defaults = new CustomProperties();
		defaults.setProperty("a", "a");

		var properties = new CustomProperties(defaults);
		properties.setProperty("b", "b");

		var clone = cloneWithoutDefaults(context, properties);

		assertThat(clone.stringPropertyNames()).containsExactly("a", "b");
		assertThat(clone.getProperty("a")).isEqualTo("a");
		assertThat(clone.getProperty("b")).isEqualTo("b");
	}

	static final class CustomProperties extends Properties {

		@Serial
		private static final long serialVersionUID = 1L;

		CustomProperties() {
			this(null);
		}

		CustomProperties(@Nullable CustomProperties defaults) {
			super(defaults);
		}

		@Override
		public synchronized Object clone() {
			CustomProperties clone = (CustomProperties) super.clone();
			clone.defaults = this.defaults == null ? null : (Properties) this.defaults.clone();
			return clone;
		}
	}

}
