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

import java.io.Writer;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

import org.jspecify.annotations.Nullable;
import org.junit.platform.configuration.processor.ConfigurationMetaData.Deprecation;
import org.junit.platform.configuration.processor.ConfigurationMetaData.Deprecation.Level;
import org.junit.platform.configuration.processor.ConfigurationMetaData.OneOrMany.Many;
import org.junit.platform.configuration.processor.ConfigurationMetaData.OneOrMany.One;
import org.junit.platform.configuration.processor.ConfigurationMetaData.Property;

import jakarta.json.Json;
import jakarta.json.JsonArray;
import jakarta.json.JsonArrayBuilder;
import jakarta.json.JsonBuilderFactory;
import jakarta.json.JsonObject;
import jakarta.json.JsonValue;

class JsonWriter {
	private final Map<String, ?> config = Map.of();
	private final JsonBuilderFactory factory = Json.createBuilderFactory(config);

	void writeValue(Writer out, ConfigurationMetaData metaData) {
		var value = toJsonObject(metaData);
		Json.createWriter(out).write(value);
	}

	private @Nullable JsonObject toJsonObject(ConfigurationMetaData metaData) {
		var builder = factory.createObjectBuilder();

		var properties = metaData.properties();
		if (!properties.isEmpty()) {
			builder.add("properties", toJsonArray(properties, this::toJsonObject));
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
			if (defaultValue instanceof One<String> one) {
				builder.add("defaultValue", toJsonValue(one));
			}
			else if (defaultValue instanceof Many<String> many) {
				builder.add("defaultValue", toJsonValue(many));
			}
		}

		var deprecation = property.deprecation();
		if (deprecation != null) {
			builder.add("deprecation", toJsonObject(deprecation));
		}

		return builder.build();
	}

	private String toJsonValue(One<String> one) {
		return one.value();
	}

	private JsonArrayBuilder toJsonValue(Many<String> many) {
		return factory.createArrayBuilder(many.values());
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

	private <T> JsonArray toJsonArray(List<T> properties, Function<T, JsonValue> converter) {
		var builder = factory.createArrayBuilder();
		properties.forEach(element -> builder.add(converter.apply(element)));
		return builder.build();
	}
}
