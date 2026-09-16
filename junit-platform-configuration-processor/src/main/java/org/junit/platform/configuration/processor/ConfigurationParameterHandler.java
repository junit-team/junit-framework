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

import java.util.List;
import java.util.regex.Pattern;

import javax.annotation.processing.Messager;
import javax.annotation.processing.RoundEnvironment;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.util.Elements;
import javax.lang.model.util.Types;

import org.jspecify.annotations.Nullable;
import org.junit.platform.configuration.api.ConfigurationParameter;
import org.junit.platform.configuration.processor.ConfigurationMetadata.Deprecation;
import org.junit.platform.configuration.processor.ConfigurationMetadata.Hint;
import org.junit.platform.configuration.processor.ConfigurationMetadata.Parameters;
import org.junit.platform.configuration.processor.ConfigurationMetadata.Property;
import org.junit.platform.configuration.processor.ConfigurationMetadata.ValueHint;
import org.junit.platform.configuration.processor.ConfigurationMetadata.ValueProvider;

final class ConfigurationParameterHandler {

	private final ConfigurationMetadata metaData;
	private final Elements elementUtils;
	private final Messager messager;
	private final Types typeUtils;

	ConfigurationParameterHandler(ConfigurationMetadata metaData, Elements elementUtils, Messager messager,
			Types typeUtils) {
		this.metaData = metaData;
		this.elementUtils = elementUtils;
		this.messager = messager;
		this.typeUtils = typeUtils;
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
			annotationMirror, typeUtils);
		if (!field.isStatic() || !field.isFinal() || !(field.constantValue() instanceof String name)) {
			messager.printMessage(ERROR,
				"@ConfigurationParameter annotated field must be static, final, and have constant string value",
				element);
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

		var hint = processHint(name, field, defaults);
		if (hint != null) {
			metaData.addHint(hint);
		}
	}

	private @Nullable String processType(ConfigurationParameterAnnotatedField field, @Nullable String defaultType) {
		var value = field.typeTypeElement();
		if (value == null) {
			return defaultType;
		}
		return value.getQualifiedName().toString();
	}

	private @Nullable String processDescription(ConfigurationParameterAnnotatedField field) {
		var docComment = field.docComment();
		return extractFirstParagraph(docComment);
	}

	private static @Nullable String extractFirstParagraph(@Nullable String docComment) {
		if (docComment == null) {
			return null;
		}
		// matches either a new paragraph, header or Javadoc tag without content (e.g. @see).
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
			return null;
		}
		var entry = defaultValues.entrySet().iterator().next();
		var values = entry.getValue();
		if (values.size() != 1) {
			messager.printMessage(ERROR, "@ConfigurationParameter must have exactly one default value", field.element(),
				field.annotationMirror());
			return null;
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
			return new Deprecation(values.get("reason"), values.get("replacement"), values.get("since"));
		}
		// Fallback, look for @Deprecated
		if (field.isDeprecated()) {
			return new Deprecation(null, null, null);
		}
		return null;
	}

	private @Nullable Hint processHint(String name, ConfigurationParameterAnnotatedField field,
			@Nullable Default defaults) {

		// Derive hint from ConfigurationParameter.type value
		var typeElement = field.typeTypeElement();
		if (typeElement != null) {
			var typeElementKind = typeElement.getKind();
			var typeElementName = typeElement.getQualifiedName().toString();
			if (typeElementKind == ElementKind.ENUM) {
				return new Hint(name, processEnumValues(typeElement), null);
			}
			// It is not possible to determine hints for abstract classes
			if (typeElementKind == ElementKind.INTERFACE) {
				return new Hint(name, null, processClassValues(typeElementName));
			}
			if (Boolean.class.getName().equals(typeElementName)) {
				return new Hint(name, processBooleanValues(), null);
			}
		}

		// Derive hints from ConfigurationParameter.defaultValue if available.
		if (defaults == null) {
			return null;
		}
		var defaultType = defaults.defaultType();
		if (Boolean.class.getName().equals(defaultType)) {
			return new Hint(name, processBooleanValues(), null);
		}
		if (Class.class.getName().equals(defaultType)) {
			if (typeElement == null) {
				messager.printMessage(ERROR,
					"@ConfigurationParameter must declare a type when the default value is a classValue",
					field.element());
				return null;
			}
			// TODO: Works for abstract classes, but only when there is a default.
			// TODO: Consider limiting the allowed type values to primitives, enums and interfaces.
			var typeElementName = typeElement.getQualifiedName().toString();
			return new Hint(name, null, processClassValues(typeElementName));
		}

		return null;
	}

	private static List<ValueProvider> processClassValues(String typeElementName) {
		var parameters = new Parameters(typeElementName);
		var valueprovider = new ValueProvider("class-reference", parameters);
		return List.of(valueprovider);
	}

	private static List<ValueHint> processBooleanValues() {
		return List.of( //
			new ValueHint(true, null), //
			new ValueHint(false, null) //
		);
	}

	private List<ValueHint> processEnumValues(TypeElement typeElement) {
		return typeElement.getEnclosedElements().stream() //
				.filter(element -> element.getKind() == ElementKind.ENUM_CONSTANT) //
				.map(
					element -> new ValueHint(element.getSimpleName().toString(), processDescription(element))).toList();
	}

	private @Nullable String processDescription(Element element) {
		var docComment = elementUtils.getDocComment(element);
		return extractFirstParagraph(docComment);
	}
}
