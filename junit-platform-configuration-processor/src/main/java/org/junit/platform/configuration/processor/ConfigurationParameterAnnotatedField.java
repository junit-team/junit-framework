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
import static org.junit.platform.configuration.processor.AnnotationMirrorUtil.toMap;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.Element;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.DeclaredType;

import org.jspecify.annotations.Nullable;
import org.junit.platform.configuration.api.ConfigurationParameter;

final class ConfigurationParameterAnnotatedField {
	private final VariableElement element;
	private final TypeElement enclosingType;
	private final AnnotationMirror annotationMirror;
	private final Map<String, Object> values;

	ConfigurationParameterAnnotatedField(TypeElement enclosingType, VariableElement element) {
		this.element = element;
		this.enclosingType = enclosingType;
		this.annotationMirror = requireNonNull(getAnnotationMirror(element, ConfigurationParameter.class));
		this.values = toMap(annotationMirror);
	}

	Element element() {
		return element;
	}

	AnnotationMirror annotationMirror() {
		return annotationMirror;
	}

	@Nullable
	Object constantValue() {
		return element.getConstantValue();
	}

	boolean isStatic() {
		return element.getModifiers().contains(Modifier.STATIC);
	}

	boolean isFinal() {
		return element.getModifiers().contains(Modifier.FINAL);
	}

	boolean isDeprecated() {
		return getAnnotationMirror(element, Deprecated.class) != null;
	}

	String enclosingTypeName() {
		return enclosingType.getQualifiedName().toString();
	}

	Map<String, Object> values() {
		return values;
	}

	@SuppressWarnings("unchecked")
	Map<String, String> deprecation() {
		return (Map<String, String>) values().getOrDefault("deprecation", Collections.emptyMap());
	}

	@SuppressWarnings("unchecked")
	Map<String, List<Object>> defaultValues() {
		return (Map<String, List<Object>>) values().getOrDefault("defaultValue", Collections.emptyMap());
	}

	@Nullable
	DeclaredType type() {
		return (DeclaredType) values().get("type");
	}

	@SuppressWarnings("unchecked")
	Map<String, Object> hints() {
		return (Map<String, Object>) values().getOrDefault("hints", Collections.emptyMap());
	}

	@SuppressWarnings("unchecked")
	List<Map<String, String>> hintsValue() {
		return (List<Map<String, String>>) hints().getOrDefault("value", Collections.emptyList());
	}

	boolean hintsPermitsAdditionalValues() {
		return (boolean) hints().getOrDefault("permitsAdditionalValues", false);
	}
}
