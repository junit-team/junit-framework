/*
 * Copyright 2026 the original author or authors.
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v2.0 which
 * accompanies this distribution and is available at
 *
 * https://www.eclipse.org/legal/epl-v20.html
 */

package org.junit.platform.suite.engine;

import static org.junit.platform.commons.support.AnnotationSupport.findAnnotation;

import java.lang.annotation.Annotation;
import java.lang.reflect.AnnotatedElement;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

import org.jspecify.annotations.Nullable;
import org.junit.platform.commons.support.ReflectionSupport;

final class SuiteAnnotationSupport {

	// Should only ever contain the result of loading org.junit.jupiter.api.Disabled.
	private static final Map<String, Optional<Class<? extends Annotation>>> cache = new ConcurrentHashMap<>(1);

	static Optional<? extends Annotation> findAnnotationByName(@Nullable AnnotatedElement element,
			String annotationTypeName) {
		return annotationForName(annotationTypeName) //
				.flatMap(annotationClass -> findAnnotation(element, annotationClass));
	}

	private static Optional<? extends Class<? extends Annotation>> annotationForName(String annotationTypeName) {
		return cache.computeIfAbsent(annotationTypeName, SuiteAnnotationSupport::tryToLoadAnnotation);
	}

	@SuppressWarnings("unchecked")
	private static Optional<Class<? extends Annotation>> tryToLoadAnnotation(String s) {
		return ReflectionSupport.tryToLoadClass(s) //
				.toOptional() //
				.filter(Annotation.class::isAssignableFrom) //
				.map(aClass -> (Class<? extends Annotation>) aClass);
	}

	private SuiteAnnotationSupport() {
		/* no-op */
	}

}
