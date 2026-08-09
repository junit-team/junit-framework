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
import static org.junit.platform.configuration.processor.AnnotationMirrorUtil.getAnnotationValue;
import static org.junit.platform.configuration.processor.AnnotationMirrorUtil.getStringValue;
import static org.junit.platform.configuration.processor.AnnotationMirrorUtil.getStringValuesMap;

import java.util.Map;
import java.util.regex.Pattern;

import javax.annotation.processing.RoundEnvironment;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.util.Elements;

import org.jspecify.annotations.Nullable;
import org.junit.platform.commons.util.Preconditions;
import org.junit.platform.configuration.api.ConfigurationParameter;
import org.junit.platform.configuration.processor.ConfigurationMetaData.CommaSeparatedList;
import org.junit.platform.configuration.processor.ConfigurationMetaData.Property;

final class ConfigurationParameterHandler {

	private static final Map<String, String> NAME_TO_TYPE_NAME = Map.of( //
		"shorts", Short.class.getName(), //
		"bytes", Byte.class.getName(), //
		"ints", Integer.class.getName(), //
		"longs", Long.class.getName(), //
		"floats", Float.class.getName(), //
		"doubles", Double.class.getName(), //
		"chars", Character.class.getName(), //
		"booleans", Boolean.class.getName(), //
		"strings", String.class.getName(), //
		"classes", Class.class.getName() //
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
				.map(this::createProperty) //
				.forEach(metaData::addProperty);
	}

	private Property createProperty(VariableElement element) {
		var name = processName(element);
		var description = processDescription(element);
		var sourceType = processSourceType(element);
		var defaults = processDefaults(element);
		var deprecation = processDeprecation(element);
		var defaultType = defaults == null ? null : defaults.defaultType();
		var defaultValue = defaults == null ? null : defaults.value();
		var type = processType(element, defaultType);
		return new Property(name, type, description, sourceType, defaultValue, deprecation);
	}

	private String processName(VariableElement element) {
		// TODO: Report preconditions problems with processingEnvironment().getMessager().printMessage() instead.
		var enclosingTypeElement = getEnclosingTypeElement(element);
		Preconditions.condition(isStatic(element), //
			() -> "Field [%s.%s] must be declared static" //
					.formatted(enclosingTypeElement.getQualifiedName(), element.getSimpleName()));
		Preconditions.condition(isFinal(element), //
			() -> "Field [%s.%s] must be declared final" //
					.formatted(enclosingTypeElement.getQualifiedName(), element.getSimpleName()));
		var constantValue = element.getConstantValue();
		Preconditions.condition(constantValue instanceof String, //
			() -> "Field [%s.%s] must have a constant string value" //
					.formatted(enclosingTypeElement.getQualifiedName(), element.getSimpleName()));
		return (String) constantValue;
	}

	private @Nullable String processType(VariableElement element, @Nullable String defaultType) {
		var parameter = getAnnotationMirror(element, ConfigurationParameter.class);
		if (parameter == null) {
			return defaultType;
		}

		var type = getStringValue(parameter, "type");
		if (type == null || type.equals(Void.class.getName())) {
			return defaultType;
		}

		return type;
	}

	private @Nullable String processDescription(VariableElement element) {
		var docComment = elementUtils.getDocComment(element);
		return docComment == null ? null : cleanupDocComment(docComment);
	}

	private static String cleanupDocComment(String docComment) {
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

	private String processSourceType(VariableElement element) {
		var enclosingTypeElement = getEnclosingTypeElement(element);
		return enclosingTypeElement.getQualifiedName().toString();
	}

	private TypeElement getEnclosingTypeElement(VariableElement element) {
		var enclosingElement = element.getEnclosingElement();
		Preconditions.condition(enclosingElement instanceof TypeElement, //
			() -> "[%s] did not have an enclosing type element" //
					.formatted(element.getSimpleName()));
		return (TypeElement) enclosingElement;
	}

	private @Nullable Default processDefaults(VariableElement element) {
		var parameter = getAnnotationMirror(element, ConfigurationParameter.class);
		if (parameter == null) {
			return null;
		}

		var defaults = getAnnotationValue(parameter, "defaultValue");
		if (defaults == null) {
			return null;
		}

		var defaultValues = getStringValuesMap(defaults);
		Preconditions.condition(defaultValues.size() == 1, () -> {
			var enclosingTypeElement = getEnclosingTypeElement(element);
			return "Field [%s.%s] must have exactly one (set of) default value(s)".formatted(enclosingTypeElement,
				element.getSimpleName());
		});

		return defaultValues.entrySet().stream() //
				.map(entry -> new Default( //
					requireNonNull(NAME_TO_TYPE_NAME.get(entry.getKey())),
					new CommaSeparatedList<>(entry.getValue()))).findFirst() //
				.orElse(null);
	}

	private record Default(String defaultType, CommaSeparatedList<String> value) {

	}

	private ConfigurationMetaData.@Nullable Deprecation processDeprecation(VariableElement element) {
		var parameter = getAnnotationMirror(element, ConfigurationParameter.class);
		if (parameter == null) {
			return null;
		}
		var deprecation = getAnnotationValue(parameter, "deprecation");
		if (deprecation != null) {
			var reason = getStringValue(deprecation, "reason");
			var replacement = getStringValue(deprecation, "replacement");
			var since = getStringValue(deprecation, "since");
			if (reason != null || replacement != null || since != null) {
				return new ConfigurationMetaData.Deprecation(null, reason, replacement, since);
			}
		}
		// Fallback, look for @Deprecated
		if (getAnnotationMirror(element, Deprecated.class) != null) {
			return new ConfigurationMetaData.Deprecation(null, null, null, null);
		}
		return null;
	}

	private static boolean isStatic(VariableElement variableElement) {
		return variableElement.getModifiers().contains(Modifier.STATIC);
	}

	private static boolean isFinal(VariableElement variableElement) {
		return variableElement.getModifiers().contains(Modifier.FINAL);
	}
}
