/*
 * Copyright 2015-2026 the original author or authors.
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v2.0 which
 * accompanies this distribution and is available at
 *
 * https://www.eclipse.org/legal/epl-v20.html
 */

package org.junit.jupiter.engine.discovery;

import java.lang.annotation.Annotation;
import java.lang.reflect.AnnotatedElement;
import java.util.List;
import java.util.Optional;

import org.junit.platform.commons.support.AnnotationSupport;
import org.junit.platform.commons.util.Preconditions;
import org.junit.platform.engine.TestDescriptor;

/**
 * Abstract base class for wrappers for test descriptors based on annotated
 * elements.
 *
 * @since 5.8
 */
abstract class AbstractAnnotatedDescriptorWrapper<E extends AnnotatedElement> {

	private final TestDescriptor testDescriptor;
	private final E annotatedElement;

	AbstractAnnotatedDescriptorWrapper(TestDescriptor testDescriptor, E annotatedElement) {
		this.testDescriptor = testDescriptor;
		this.annotatedElement = annotatedElement;
	}

	E getAnnotatedElement() {
		return this.annotatedElement;
	}

	TestDescriptor getTestDescriptor() {
		return this.testDescriptor;
	}

	public final String getDisplayName() {
		return this.testDescriptor.getDisplayName();
	}

	public final boolean isAnnotated(Class<? extends Annotation> annotationType) {
		Preconditions.notNull(annotationType, "annotationType must not be null");
		return AnnotationSupport.isAnnotated(getAnnotatedElement(), annotationType);
	}

	public final <A extends Annotation> Optional<A> findAnnotation(Class<A> annotationType) {
		Preconditions.notNull(annotationType, "annotationType must not be null");
		return AnnotationSupport.findAnnotation(getAnnotatedElement(), annotationType);
	}

	public final <A extends Annotation> List<A> findRepeatableAnnotations(Class<A> annotationType) {
		Preconditions.notNull(annotationType, "annotationType must not be null");
		return AnnotationSupport.findRepeatableAnnotations(getAnnotatedElement(), annotationType);
	}

}
