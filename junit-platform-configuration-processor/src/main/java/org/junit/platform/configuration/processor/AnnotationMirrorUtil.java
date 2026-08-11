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

import static java.util.stream.Collectors.toMap;

import java.lang.annotation.Annotation;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;

import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.AnnotationValue;
import javax.lang.model.element.Element;
import javax.lang.model.element.ExecutableElement;

import org.jspecify.annotations.Nullable;

class AnnotationMirrorUtil {

	private AnnotationMirrorUtil() {
		/* no-op */
	}

	static AnnotationMirror getAnnotationMirror(Element element, Class<? extends Annotation> annotationType) {
		var annotationTypeName = annotationType.getName();
		return element.getAnnotationMirrors().stream() //
				.filter(annotation -> annotationTypeName.equals(annotation.getAnnotationType().toString())) //
				.findFirst() //
				.orElseThrow();
	}

	static @Nullable String getStringValue(AnnotationMirror annotation, String elementName) {
		return findElementBy(annotation, elementName) //
				.map(Object::toString) //
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
				.map(Entry::getValue) //
				.map(AnnotationValue::getValue) //
				.findFirst();
	}

	static Map<String, List<Object>> getValuesMap(AnnotationMirror defaults) {
		return defaults.getElementValues().entrySet() //
				.stream() //
				.collect(toMap(AnnotationMirrorUtil::getSimpleName, AnnotationMirrorUtil::getValues));
	}

	private static List<Object> getValues(Entry<? extends ExecutableElement, ? extends AnnotationValue> entry) {
		if (entry.getValue().getValue() instanceof List<?> values) {
			return values.stream().filter(AnnotationValue.class::isInstance) //
					.map(AnnotationValue.class::cast) //
					.map(AnnotationValue::getValue) //
					.toList();
		}
		return Collections.emptyList();
	}

	private static String getSimpleName(Entry<? extends ExecutableElement, ? extends AnnotationValue> entry) {
		return entry.getKey().getSimpleName().toString();
	}
}
