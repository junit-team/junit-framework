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
import static org.junit.platform.configuration.processor.AnnotationMirrorUtil.getValuesMap;

import java.util.Map;
import java.util.regex.Pattern;

import javax.annotation.processing.RoundEnvironment;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.util.Elements;

import org.jspecify.annotations.Nullable;
import org.junit.platform.commons.util.Preconditions;
import org.junit.platform.configuration.api.ConfigurationParameter;
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
				.map(element -> new ProcessingContext( //
					element, //
					getEnclosingTypeElement(element), //
					getAnnotationMirror(element, ConfigurationParameter.class))) //
				.map(this::createProperty) //
				.forEach(metaData::addProperty);
	}

	private record ProcessingContext(VariableElement element, TypeElement enclosingType,
			AnnotationMirror annotationMirror) {

	}

	private Property createProperty(ProcessingContext context) {
		var name = processName(context);
		var description = processDescription(context);
		var sourceType = processSourceType(context);
		var defaults = processDefaults(context);
		var deprecation = processDeprecation(context);
		var defaultType = defaults == null ? null : defaults.defaultType();
		var defaultValue = defaults == null ? null : defaults.value();
		var type = processType(context, defaultType);
		return new Property(name, type, description, sourceType, defaultValue, deprecation);
	}

	private String processName(ProcessingContext context) {
		// TODO: Report preconditions problems with processingEnvironment().getMessager().printMessage() instead.
		var enclosingTypeElement = context.enclosingType();
		var element = context.element();
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

	private @Nullable String processType(ProcessingContext context, @Nullable String defaultType) {
		var parameter = context.annotationMirror();
		var type = getStringValue(parameter, "type");
		if (type == null || type.equals(Void.class.getName())) {
			return defaultType;
		}
		return type;
	}

	private @Nullable String processDescription(ProcessingContext context) {
		var docComment = elementUtils.getDocComment(context.element());
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

	private String processSourceType(ProcessingContext context) {
		return context.enclosingType().getQualifiedName().toString();
	}

	private TypeElement getEnclosingTypeElement(VariableElement element) {
		var enclosingElement = element.getEnclosingElement();
		Preconditions.condition(enclosingElement instanceof TypeElement, //
			() -> "[%s] did not have an enclosing type element" //
					.formatted(element.getSimpleName()));
		return (TypeElement) enclosingElement;
	}

	private @Nullable Default processDefaults(ProcessingContext context) {
		var element = context.element();
		var parameter = context.annotationMirror();
		var defaults = getAnnotationValue(parameter, "defaultValue");
		if (defaults == null) {
			return null;
		}

		var defaultValues = getValuesMap(defaults);
		Preconditions.condition(defaultValues.size() == 1, //
			() -> "Field [%s.%s] must have exactly one default value" //
					.formatted(context.enclosingType(), element.getSimpleName()));

		Preconditions.condition(defaultValues.size() == 1, //
			() -> "Field [%s.%s] must have exactly one default value" //
					.formatted(context.enclosingType(), element.getSimpleName()));

		var entry = defaultValues.entrySet().iterator().next();
		var value = entry.getValue();
		Preconditions.condition(value.size() == 1, //
			() -> "Field [%s.%s] must have exactly one default value" //
					.formatted(context.enclosingType(), element.getSimpleName()));

		var defaultValue = value.get(0);
		var defaultName = entry.getKey();
		var defaultType = requireNonNull(NAME_TO_TYPE_NAME.get(defaultName));
		return new Default(defaultType, defaultValue);
	}

	private record Default(String defaultType, Object value) {

	}

	private ConfigurationMetaData.@Nullable Deprecation processDeprecation(ProcessingContext context) {
		var deprecation = getAnnotationValue(context.annotationMirror(), "deprecation");
		if (deprecation != null) {
			var reason = getStringValue(deprecation, "reason");
			var replacement = getStringValue(deprecation, "replacement");
			var since = getStringValue(deprecation, "since");
			if (reason != null || replacement != null || since != null) {
				return new ConfigurationMetaData.Deprecation(null, reason, replacement, since);
			}
		}
		// Fallback, look for @Deprecated
		if (getAnnotationMirror(context.element(), Deprecated.class) != null) {
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
