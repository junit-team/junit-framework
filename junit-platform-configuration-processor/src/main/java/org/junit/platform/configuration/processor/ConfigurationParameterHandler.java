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

import static java.util.Objects.requireNonNull;
import static org.junit.platform.configuration.processor.AnnotationMirrorUtil.getAnnotationMirror;

import java.util.Map;
import java.util.regex.Pattern;

import javax.annotation.processing.RoundEnvironment;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.util.Elements;

import org.jspecify.annotations.Nullable;
import org.junit.platform.commons.util.Preconditions;
import org.junit.platform.configuration.api.ConfigurationParameter;
import org.junit.platform.configuration.processor.ConfigurationMetaData.Deprecation;
import org.junit.platform.configuration.processor.ConfigurationMetaData.Property;

final class ConfigurationParameterHandler {

	private static final Map<String, String> NAME_TO_TYPE_NAME = Map.of( //
		"shortValue", Short.class.getName(), //
		"byteValue", Byte.class.getName(), //
		"intValue", Integer.class.getName(), //
		"longValue", Long.class.getName(), //
		"floatValue", Float.class.getName(), //
		"doubleValue", Double.class.getName(), //
		"charValue", Character.class.getName(), //
		"booleanValue", Boolean.class.getName(), //
		"stringValue", String.class.getName(), //
		"classValue", Class.class.getName() //
	);

	private final ConfigurationMetaData metaData;
	private final Elements elementUtils;

	ConfigurationParameterHandler(ConfigurationMetaData metaData, Elements elementUtils) {
		this.metaData = metaData;
		this.elementUtils = elementUtils;
	}

	void process(RoundEnvironment roundEnv) {
		roundEnv.getElementsAnnotatedWith(ConfigurationParameter.class).stream() //
				.filter(VariableElement.class::isInstance) //
				.map(VariableElement.class::cast) //
				.map(this::createConfigurationParameterAnnotatedField) //
				.map(this::createProperty) //
				.forEach(metaData::addProperty);
	}

	private ConfigurationParameterAnnotatedField createConfigurationParameterAnnotatedField(VariableElement element) {
		var enclosingElement = element.getEnclosingElement();
		Preconditions.condition(enclosingElement instanceof TypeElement, //
			() -> "[%s] did not have an enclosing typeValue element".formatted(element.getSimpleName().toString()));
		var enclosingTypeElement = (TypeElement) enclosingElement;
		var configurationParameter = requireNonNull(getAnnotationMirror(element, ConfigurationParameter.class));
		return new ConfigurationParameterAnnotatedField(element, elementUtils, enclosingTypeElement,
			configurationParameter);
	}

	private Property createProperty(ConfigurationParameterAnnotatedField field) {
		var name = processName(field);
		var description = processDescription(field);
		var sourceType = processSourceType(field);
		var defaults = processDefaults(field);
		var deprecation = processDeprecation(field);
		var defaultType = defaults == null ? null : defaults.defaultType();
		var defaultValue = defaults == null ? null : defaults.value();
		var type = processType(field, defaultType);
		return new Property(name, type, description, sourceType, defaultValue, deprecation);
	}

	private String processName(ConfigurationParameterAnnotatedField field) {
		// TODO: Report preconditions problems with processingEnvironment().getMessager().printMessage() instead.
		Preconditions.condition(field.isStatic(), //
			() -> "Field [%s] must be declared static".formatted(field.name()));
		Preconditions.condition(field.isFinal(), //
			() -> "Field [%s] must be declared final".formatted(field.name()));
		var constantValue = field.constantValue();
		Preconditions.condition(constantValue instanceof String, //
			() -> "Field [%s] must have a constant string value".formatted(field.name()));
		return (String) constantValue;
	}

	private @Nullable String processType(ConfigurationParameterAnnotatedField field, @Nullable String defaultType) {
		var type = field.typeValue();
		return type == null ? defaultType : type;
	}

	private @Nullable String processDescription(ConfigurationParameterAnnotatedField field) {
		var docComment = field.docComment();
		if (docComment == null) {
			return null;
		}
		// TODO: Creating patterns over and over is not very efficient
		var matcher = Pattern.compile("<p>|<h\\d>").matcher(docComment);
		var firstParagraph = !matcher.find() ? docComment : docComment.substring(0, matcher.start());
		return firstParagraph //
				// Replace newlines with space
				.replaceAll("[\n\r]", " ") //
				// Merge multiple spaces
				.replaceAll(" +", " ") //
				// Replace the `: {@value}` conventional syntax.
				.replaceAll(": \\{@value}\\.?", ".") //
				.trim();
	}

	private String processSourceType(ConfigurationParameterAnnotatedField field) {
		return field.enclosingTypeName();
	}

	private @Nullable Default processDefaults(ConfigurationParameterAnnotatedField field) {
		var defaultValues = field.defaultValues();
		if (defaultValues.isEmpty()) {
			return null;
		}

		Preconditions.condition(defaultValues.size() == 1, //
			() -> "Field [%s] must have exactly one default value".formatted(field.name()));

		Preconditions.condition(defaultValues.size() == 1, //
			() -> "Field [%s] must have exactly one default value".formatted(field.name()));

		var entry = defaultValues.entrySet().iterator().next();
		var value = entry.getValue();
		Preconditions.condition(value.size() == 1, //
			() -> "Field [%s] must have exactly one default value".formatted(field.name()));

		var defaultValue = value.get(0);
		var defaultName = entry.getKey();
		var defaultType = requireNonNull(NAME_TO_TYPE_NAME.get(defaultName));
		return new Default(defaultType, defaultValue);
	}

	private record Default(String defaultType, Object value) {

	}

	private @Nullable Deprecation processDeprecation(ConfigurationParameterAnnotatedField field) {
		var values = field.deprecationValues();
		if (!values.isEmpty()) {
			return new Deprecation(null, values.get("reason"), values.get("replacement"), values.get("since"));
		}
		// Fallback, look for @Deprecated
		if (field.isDeprecated()) {
			return new Deprecation(null, null, null, null);
		}
		return null;
	}

}
