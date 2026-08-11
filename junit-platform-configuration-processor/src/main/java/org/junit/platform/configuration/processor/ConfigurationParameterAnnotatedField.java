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

import static org.junit.platform.configuration.processor.AnnotationMirrorUtil.getAnnotationMirror;
import static org.junit.platform.configuration.processor.AnnotationMirrorUtil.getStringValuesMap;
import static org.junit.platform.configuration.processor.AnnotationMirrorUtil.getValuesMap;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.Element;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.util.Elements;

import org.jspecify.annotations.Nullable;

final class ConfigurationParameterAnnotatedField {
	private final VariableElement element;
	private final TypeElement enclosingType;
	private final AnnotationMirror annotationMirror;
	private final Elements elementUtils;

	ConfigurationParameterAnnotatedField(VariableElement element, Elements elementUtils, TypeElement enclosingType,
			AnnotationMirror annotationMirror) {
		this.element = element;
		this.elementUtils = elementUtils;
		this.enclosingType = enclosingType;
		this.annotationMirror = annotationMirror;
	}

	Element element() {
		return element;
	}

	AnnotationMirror annotationMirror() {
		return annotationMirror;
	}

	Map<String, String> deprecationValues() {
		var deprecation = getAnnotationMirror(annotationMirror, "deprecation");
		if (deprecation == null) {
			return Collections.emptyMap();
		}
		return getStringValuesMap(deprecation);
	}

	Map<String, List<Object>> defaultValues() {
		var defaultValue = getAnnotationMirror(annotationMirror, "defaultValue");
		if (defaultValue == null) {
			return Collections.emptyMap();
		}
		return getValuesMap(defaultValue);
	}

	@Nullable
	String typeValue() {
		return getStringValuesMap(annotationMirror).get("type");
	}

	@Nullable
	Object constantValue() {
		return element.getConstantValue();
	}

	String name() {
		return "%s.%s".formatted(enclosingType.getQualifiedName(), element.getSimpleName());
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

	@Nullable
	String docComment() {
		return elementUtils.getDocComment(element);
	}

	String enclosingTypeName() {
		return enclosingType.getQualifiedName().toString();
	}
}
