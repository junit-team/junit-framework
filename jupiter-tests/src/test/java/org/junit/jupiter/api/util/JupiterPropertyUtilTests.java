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

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.util.JupiterPropertyUtils.createEffectiveClone;

import java.util.Properties;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("cloneProperties Tests")
class JupiterPropertyUtilTests {

	@Test
	void cloneProperties() {
		var properties = new Properties();
		properties.setProperty("a", "a");
		properties.put("a-obj", new Object());

		var clone = createEffectiveClone(properties);

		assertThat(clone.stringPropertyNames()).containsExactly("a");
		assertThat(clone.get("a")).isSameAs(properties.get("a"));

		assertThat(clone.keySet()).containsExactly("a");
		assertThat(clone.getProperty("a")).isEqualTo("a");
	}

	@Test
	void withDefaults() {
		var defaults = new Properties();
		defaults.setProperty("a", "a");

		var properties = new Properties(defaults);
		properties.setProperty("b", "b");

		var clone = createEffectiveClone(properties);

		assertThat(clone.stringPropertyNames()).containsExactly("a", "b");
		assertThat(clone.getProperty("a")).isEqualTo("a");
		assertThat(clone.getProperty("b")).isEqualTo("b");

		assertThat(clone.keySet()).containsExactly("a", "b");
		assertThat(clone.get("a")).isSameAs(defaults.get("a"));
		assertThat(clone.get("b")).isSameAs(properties.get("b"));
	}

	@Test
	void doesNotIncludeShadowedDefaults() {
		var defaults = new Properties();
		defaults.setProperty("a", "a");

		var properties = new Properties(defaults);
		var shadow = new Object();
		properties.put("a", shadow);

		var clone = createEffectiveClone(properties);

		assertThat(clone.stringPropertyNames()).containsExactly("a");
		assertThat(clone.getProperty("a")).isEqualTo("a");

		assertThat(clone.keySet()).containsExactly("a");
		assertThat(clone.get("a")).isEqualTo("a");
	}

	@Test
	void doesNotIncludeOverridenDefaultValue() {
		var defaults = new Properties();
		defaults.setProperty("a", "a");

		var properties = new Properties(defaults);
		properties.put("a", "b");

		var clone = createEffectiveClone(properties);

		assertThat(clone.getProperty("a")).isEqualTo("b");
		clone.remove("a");
		// properties.getProperty("a") would return the default value "a" here.
		assertThat(clone.getProperty("a")).isNull();
		assertThat(clone.get("a")).isNull();
	}

	@Test
	void doesIncludeDefaultOverridingValue() {
		var defaults = new Properties();
		defaults.setProperty("a", "a");

		var properties = new Properties(defaults);
		properties.put("a", "b");

		var clone = createEffectiveClone(properties);

		assertThat(clone.stringPropertyNames()).containsExactly("a");
		assertThat(clone.getProperty("a")).isEqualTo("b");

		assertThat(clone.keySet()).containsExactly("a");
		assertThat(clone.get("a")).isSameAs("b");
	}

}
