/*
 * Copyright 2026 the original author or authors.
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v2.0 which
 * accompanies this distribution and is available at
 *
 * https://www.eclipse.org/legal/epl-v20.html
 */

package org.junit.platform.configuration.processor;

import java.util.ArrayList;
import java.util.List;

import org.jspecify.annotations.Nullable;

/**
 * See <a href="https://docs.spring.io/spring-boot/specification/configuration-metadata/format.html">Spring Boot - Specifications - Configuration Metadata - Metadata Format</a>.
 */
final class ConfigurationMetaData {

	private final List<Property> properties = new ArrayList<>();

	List<Property> properties() {
		return properties;
	}

	void addProperty(Property property) {
		properties.add(property);
	}

	record Property( //
			String name, //
			@Nullable String type, //
			@Nullable String description, //
			@Nullable String sourceType, //
			@Nullable Object defaultValue, //
			@Nullable Deprecation deprecation //
	) {

	}

	record Deprecation( //
			@Nullable Level level, //
			@Nullable String reason, //
			@Nullable String replacement, //
			@Nullable String since //
	) {

		enum Level {
			WARNING("warning"), ERROR("error");

			private final String value;

			Level(String value) {
				this.value = value;
			}

			String value() {
				return value;
			}
		}
	}

}
