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
import static javax.tools.Diagnostic.Kind.ERROR;
import static org.junit.platform.configuration.processor.AnnotationMirrorUtil.getAnnotationMirror;

import java.util.regex.Pattern;

import javax.annotation.processing.Messager;
import javax.annotation.processing.RoundEnvironment;
import javax.lang.model.element.Element;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.util.Elements;

import org.jspecify.annotations.Nullable;
import org.junit.platform.configuration.api.ConfigurationParameter;
import org.junit.platform.configuration.processor.ConfigurationMetaData.Deprecation;
import org.junit.platform.configuration.processor.ConfigurationMetaData.Property;

final class ConfigurationParameterHandler {

	private final ConfigurationMetaData metaData;
	private final Elements elementUtils;
	private final Messager messager;

	ConfigurationParameterHandler(ConfigurationMetaData metaData, Elements elementUtils, Messager messager) {
		this.metaData = metaData;
		this.elementUtils = elementUtils;
		this.messager = messager;
	}

	void process(RoundEnvironment roundEnv) {
		roundEnv.getElementsAnnotatedWith(ConfigurationParameter.class).forEach(this::processElement);
	}

	private void processElement(Element element) {
		if (!(element instanceof VariableElement variableElement)) {
			messager.printMessage(ERROR, "@ConfigurationParameter annotated element was not a field", element);
			return;
		}
		if (!(variableElement.getEnclosingElement() instanceof TypeElement enclosingTypeElement)) {
			messager.printMessage(ERROR, "@ConfigurationParameter annotated element did not have an enclosing element",
				element);
			return;
		}
		var annotationMirror = requireNonNull(getAnnotationMirror(element, ConfigurationParameter.class));
		var field = new ConfigurationParameterAnnotatedField(variableElement, elementUtils, enclosingTypeElement,
			annotationMirror);
		if (!field.isStatic() || !field.isFinal() || !(field.constantValue() instanceof String name)) {
			messager.printMessage(ERROR,
				"@ConfigurationParameter annotated field must static, final, and have constant string value", element);
			return;
		}
		var description = processDescription(field);
		var sourceType = processSourceType(field);
		var defaults = processDefaults(field);
		var deprecation = processDeprecation(field);
		var defaultType = defaults == null ? null : defaults.defaultType();
		var defaultValue = defaults == null ? null : defaults.value();
		var type = processType(field, defaultType);
		var property = new Property(name, type, description, sourceType, defaultValue, deprecation);
		metaData.addProperty(property);
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
		var matcher = Pattern.compile("<p>|<h\\d>|[^{]@[a-z]+").matcher(docComment);
		var firstParagraph = !matcher.find() ? docComment : docComment.substring(0, matcher.start());
		return firstParagraph //
				// Replace newlines with space
				.replaceAll("[\n\r]", " ") //
				// Merge multiple spaces
				.replaceAll(" +", " ") //
				// Replace the `: {@value}` conventional syntax.
				.replaceAll(": \\{@value}\\.?", ".") //
				// Replace the `{@code example}` syntax.
				.replaceAll("\\{@code (.+?)}", "$1") //
				// Replace the `{@link(plain) reference}` syntax.
				.replaceAll("\\{@link(?:plain)? ([^ ]+?)}", "$1") //
				// Replace the `{@link(plain) reference plain}` syntax.
				.replaceAll("\\{@link(?:plain)? [^ ]+ (.+?)}", "$1") //
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
		if (defaultValues.size() != 1) {
			messager.printMessage(ERROR, "@ConfigurationParameter must have exactly one default value", field.element(),
				field.annotationMirror());
		}
		var entry = defaultValues.entrySet().iterator().next();
		var values = entry.getValue();
		if (values.size() != 1) {
			messager.printMessage(ERROR, "@ConfigurationParameter must have exactly one default value", field.element(),
				field.annotationMirror());
		}
		var key = entry.getKey();
		var value = values.get(0);
		return switch (key) {
			case "shortValue" -> new Default(Short.class.getName(), value);
			case "byteValue" -> new Default(Byte.class.getName(), value);
			case "intValue" -> new Default(Integer.class.getName(), value);
			case "longValue" -> new Default(Long.class.getName(), value);
			case "floatValue" -> new Default(Float.class.getName(), value);
			case "doubleValue" -> new Default(Double.class.getName(), value);
			case "charValue" -> new Default(Character.class.getName(), value);
			case "booleanValue" -> new Default(Boolean.class.getName(), value);
			case "stringValue" -> new Default(String.class.getName(), value);
			case "classValue" -> new Default(Class.class.getName(), value.toString());
			default -> throw new IllegalStateException("Unexpected value: " + key);
		};
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
