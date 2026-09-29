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
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.stream.Collectors;

import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.AnnotationValue;
import javax.lang.model.element.Element;
import javax.lang.model.element.ExecutableElement;

import org.jspecify.annotations.Nullable;

class AnnotationMirrorUtil {

	private AnnotationMirrorUtil() {
		/* no-op */
	}

	static @Nullable AnnotationMirror getAnnotationMirror(Element element, Class<? extends Annotation> annotationType) {
		var elementName = annotationType.getName();
		return element.getAnnotationMirrors().stream() //
				.filter(annotation -> elementName.equals(annotation.getAnnotationType().toString())) //
				.findFirst() //
				.orElse(null);
	}

	static Map<String, Object> toMap(AnnotationMirror annotationMirror) {
		return annotationMirror.getElementValues().entrySet() //
				.stream() //
				.collect(Collectors.toMap(AnnotationMirrorUtil::getSimpleName, AnnotationMirrorUtil::getValueFrom));
	}

	private static Object getValueFrom(Entry<? extends ExecutableElement, ? extends AnnotationValue> entry) {
		var value = entry.getValue().getValue();
		return getValueFrom(value);
	}

	private static Object getValueFrom(Object value) {
		if (value instanceof AnnotationMirror annotationMirror) {
			return toMap(annotationMirror);
		}
		if (value instanceof List<?> list) {
			return list.stream().map(AnnotationMirrorUtil::getValueFrom).toList();
		}
		if (value instanceof AnnotationValue annotationValue) {
			return annotationValue.getValue();
		}
		return value;
	}

	private static String getSimpleName(Entry<? extends ExecutableElement, ? extends AnnotationValue> entry) {
		return entry.getKey().getSimpleName().toString();
	}
}
