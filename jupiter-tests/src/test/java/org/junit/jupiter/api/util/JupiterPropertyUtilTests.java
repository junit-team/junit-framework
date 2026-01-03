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
		properties.setProperty("a-shadowed", "a");
		properties.put("a-obj", new Object());

		var clone = createEffectiveClone(properties);

		assertThat(clone.stringPropertyNames()).isEqualTo(properties.stringPropertyNames());
		assertThat(clone.keySet()).isEqualTo(properties.keySet());

		assertThat(clone.getProperty("a")).isEqualTo("a");
		assertThat(clone.getProperty("a-obj")).isNull();

		assertThat(clone.get("a")).isSameAs(properties.get("a"));
		assertThat(clone.get("a-obj")).isSameAs(properties.get("a-obj"));
	}

	@Test
	void clonePropertiesWithDefaults() {
		var defaults = new Properties();
		defaults.setProperty("a", "a");

		var properties = new Properties(defaults);
		properties.setProperty("b", "b");
		properties.put("b-obj", new Object());

		var clone = createEffectiveClone(properties);

		assertThat(clone.stringPropertyNames()).isEqualTo(properties.stringPropertyNames());
		assertThat(clone.keySet()).isEqualTo(properties.keySet());

		assertThat(clone.getProperty("a")).isEqualTo("a");
		assertThat(clone.getProperty("b")).isEqualTo("b");
		assertThat(clone.getProperty("a-obj")).isNull();

		assertThat(clone.get("a")).isNull();
		assertThat(clone.get("b")).isSameAs(properties.get("b"));
		assertThat(clone.get("b-obj")).isSameAs(properties.get("b-obj"));
	}

	@Test
	void clonePropertiesWithOverridenDefaults() {
		var defaults = new Properties();
		defaults.setProperty("a", "a");

		var properties = new Properties(defaults);
		properties.put("a", "b");

		var clone = createEffectiveClone(properties);

		assertThat(clone.stringPropertyNames()).isEqualTo(properties.stringPropertyNames());
		assertThat(clone.keySet()).isEqualTo(properties.keySet());

		assertThat(clone.getProperty("a")).isEqualTo("b");
		assertThat(clone.get("a")).isSameAs("b");
	}

	@Test
	void clonePropertiesWithShadowedDefaults() {
		var defaults = new Properties();
		defaults.setProperty("a", "a");

		var properties = new Properties(defaults);
		var shadow = new Object();
		properties.put("a", shadow);

		var clone = createEffectiveClone(properties);

		assertThat(clone.stringPropertyNames()).isEqualTo(properties.stringPropertyNames());
		assertThat(clone.keySet()).isEqualTo(properties.keySet());

		assertThat(clone.getProperty("a")).isEqualTo("a");
		assertThat(clone.get("a")).isSameAs(shadow);
	}

	@Test
	void cloneOverridenDefaults() {
		var defaults = new Properties();
		defaults.setProperty("a", "a");

		var properties = new Properties(defaults);
		properties.put("a", "b");

		var clone = createEffectiveClone(properties);

		assertThat(clone.stringPropertyNames()).isEqualTo(properties.stringPropertyNames());
		assertThat(clone.keySet()).isEqualTo(properties.keySet());

		assertThat(clone.getProperty("a")).isEqualTo("b");
		clone.remove("a");
		assertThat(clone.getProperty("a")).isEqualTo("a");
		assertThat(clone.get("a")).isNull();
	}

}
