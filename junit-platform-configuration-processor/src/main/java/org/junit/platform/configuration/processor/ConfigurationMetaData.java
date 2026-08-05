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

class ConfigurationMetaData {

	//	private final List<Group> groups = new ArrayList<>();
	private final List<Property> properties = new ArrayList<>();
	//	private final List<Hint> hints = new ArrayList<>();

	public List<Property> getProperties() {
		return properties;
	}

	void addProperty(Property property) {
		properties.add(property);
	}

	//
	//	record Group( //
	//			String name, //
	//			@Nullable String type, //
	//			@Nullable String description, //
	//			@Nullable String sourceType, //
	//			@Nullable String sourceMethod //
	//	) {
	//
	//	}

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
			String level, //
			@Nullable String reason, //
			@Nullable String replacement, //
			@Nullable String since //
	) {

	}

	//	record Hint( //
	//			String name, //
	//			List<ValueHint> values, //
	//			List<ValueProvider> providers //
	//	) {
	//	}
	//
	//	record ValueHint( //
	//			Object value, //
	//			          @Nullable String description //
	//	) {
	//
	//	}
	//
	//	record ValueProvider( //
	//			String name, //
	//			Object parameters //
	//	) {
	//
	//	}
}
