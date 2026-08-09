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

import java.lang.annotation.Annotation;
import java.util.Map;
import java.util.Optional;

import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.AnnotationValue;
import javax.lang.model.element.Element;

import org.jspecify.annotations.Nullable;

final class AnnotationMirrorUtil {

	private AnnotationMirrorUtil() {
		/* no-op */
	}

	static @Nullable AnnotationMirror getAnnotationMirror(Element element, Class<? extends Annotation> annotationType) {
		var annotationTypeName = annotationType.getName();
		for (AnnotationMirror annotation : element.getAnnotationMirrors()) {
			if (annotationTypeName.equals(annotation.getAnnotationType().toString())) {
				return annotation;
			}
		}
		return null;
	}

	static @Nullable String getStringValue(AnnotationMirror annotation, String elementName) {
		return findElementBy(annotation, elementName) //
				.filter(String.class::isInstance) //
				.map(String.class::cast) //
				.filter(s -> !s.isEmpty()) //
				.orElse(null);
	}

	static @Nullable AnnotationMirror getAnnotationValue(AnnotationMirror annotation, String elementName) {
		return findElementBy(annotation, elementName) //
				.filter(AnnotationMirror.class::isInstance) //
				.map(AnnotationMirror.class::cast) //
				.orElse(null);
	}

	private static Optional<Object> findElementBy(AnnotationMirror annotation, String elementName) {
		return annotation.getElementValues().entrySet() //
				.stream() //
				.filter((element) -> element.getKey().getSimpleName().toString().equals(elementName)) //
				.map(Map.Entry::getValue) //
				.map(AnnotationValue::getValue) //
				.findFirst();
	}
}
