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

import java.util.List;
import java.util.Map;
import java.util.function.Function;

import org.junit.platform.configuration.processor.ConfigurationMetaData.Deprecation;
import org.junit.platform.configuration.processor.ConfigurationMetaData.Deprecation.Level;
import org.junit.platform.configuration.processor.ConfigurationMetaData.Hint;
import org.junit.platform.configuration.processor.ConfigurationMetaData.Parameters;
import org.junit.platform.configuration.processor.ConfigurationMetaData.Property;
import org.junit.platform.configuration.processor.ConfigurationMetaData.ValueHint;
import org.junit.platform.configuration.processor.ConfigurationMetaData.ValueProvider;

import jakarta.json.Json;
import jakarta.json.JsonArray;
import jakarta.json.JsonBuilderFactory;
import jakarta.json.JsonObject;
import jakarta.json.JsonObjectBuilder;
import jakarta.json.JsonValue;

final class JsonConverter {
	private final Map<String, ?> config = Map.of();
	private final JsonBuilderFactory factory = Json.createBuilderFactory(config);

	JsonObject toJsonObject(ConfigurationMetaData metaData) {
		var builder = factory.createObjectBuilder();

		var properties = metaData.properties();
		if (!properties.isEmpty()) {
			builder.add("properties", toJsonArray(properties, this::toJsonObject));
		}
		var hints = metaData.hints();
		if (!hints.isEmpty()) {
			builder.add("hints", toJsonArray(hints, this::toJsonObject));
		}

		return builder.build();
	}

	private JsonObject toJsonObject(Property property) {
		var builder = factory.createObjectBuilder();
		builder.add("name", property.name());

		var type = property.type();
		if (type != null) {
			builder.add("type", type);
		}

		var description = property.description();
		if (description != null) {
			builder.add("description", description);
		}

		var sourceType = property.sourceType();
		if (sourceType != null) {
			builder.add("sourceType", sourceType);
		}

		var defaultValue = property.defaultValue();
		if (defaultValue != null) {
			addObjectValue(builder, "defaultValue", defaultValue);
		}

		var deprecation = property.deprecation();
		if (deprecation != null) {
			builder.add("deprecation", toJsonObject(deprecation));
		}

		return builder.build();
	}

	private JsonObject toJsonObject(Deprecation deprecation) {
		var builder = factory.createObjectBuilder();

		var level = deprecation.level();
		if (level != null) {
			builder.add("level", toJsonValue(level));
		}

		var reason = deprecation.reason();
		if (reason != null) {
			builder.add("reason", reason);
		}

		var replacement = deprecation.replacement();
		if (replacement != null) {
			builder.add("replacement", replacement);
		}

		var since = deprecation.since();
		if (since != null) {
			builder.add("since", since);
		}

		return builder.build();
	}

	private String toJsonValue(Level level) {
		return level.value();
	}

	private JsonObject toJsonObject(Hint hint) {
		var builder = factory.createObjectBuilder();
		builder.add("name", hint.name());

		var values = hint.values();
		if (values != null) {
			builder.add("values", toJsonArray(values, this::toJsonObject));
		}

		var providers = hint.providers();
		if (providers != null) {
			builder.add("providers", toJsonArray(providers, this::toJsonObject));
		}
		return builder.build();
	}

	private JsonObject toJsonObject(ValueHint valueHint) {
		var builder = factory.createObjectBuilder();
		addObjectValue(builder, "value", valueHint.value());

		var description = valueHint.description();
		if (description != null) {
			builder.add("description", description);
		}
		return builder.build();
	}

	private JsonObject toJsonObject(ValueProvider valueProvider) {
		var builder = factory.createObjectBuilder();
		builder.add("name", valueProvider.name());

		var parameters = valueProvider.parameters();
		if (parameters != null) {
			builder.add("parameters", toJsonObject(parameters));
		}
		return builder.build();
	}

	private JsonObject toJsonObject(Parameters parameters) {
		var builder = factory.createObjectBuilder();
		builder.add("target", parameters.target());
		return builder.build();
	}

	private <T> JsonArray toJsonArray(List<T> properties, Function<T, JsonValue> converter) {
		var builder = factory.createArrayBuilder();
		properties.forEach(element -> builder.add(converter.apply(element)));
		return builder.build();
	}

	private void addObjectValue(JsonObjectBuilder builder, String name, Object value) {
		if (value instanceof Short v) {
			builder.add(name, v);
		}
		else if (value instanceof Byte v) {
			builder.add(name, "%02X".formatted(v));
		}
		else if (value instanceof Integer v) {
			builder.add(name, v);
		}
		else if (value instanceof Long v) {
			builder.add(name, v);
		}
		else if (value instanceof Float v) {
			builder.add(name, v);
		}
		else if (value instanceof Double v) {
			builder.add(name, v);
		}
		else if (value instanceof Character v) {
			builder.add(name, String.valueOf(v));
		}
		else if (value instanceof Boolean v) {
			builder.add(name, v);
		}
		else if (value instanceof String v) {
			builder.add(name, v);
		}
		else {
			throw new IllegalArgumentException(
				"Field [%s] should be a convertable primitive but was %s".formatted(name, value.getClass()));
		}
	}
}
