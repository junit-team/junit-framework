/*
 * Copyright 2015-2026 the original author or authors.
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v2.0 which
 * accompanies this distribution and is available at
 *
 * https://www.eclipse.org/legal/epl-v20.html
 */

package org.junit.jupiter.api.extension;

import static org.apiguardian.api.API.Status.MAINTAINED;

import java.lang.annotation.Annotation;
import java.lang.reflect.AnnotatedElement;
import java.util.List;
import java.util.Optional;

import org.apiguardian.api.API;
import org.junit.platform.commons.support.AnnotationSupport;

/**
 * {@code AnnotatedElementContext} encapsulates the <em>context</em> in which an
 * {@link #getAnnotatedElement() AnnotatedElement} is declared.
 *
 * <p>For example, an {@code AnnotatedElementContext} is used in
 * {@link org.junit.jupiter.api.io.TempDirFactory TempDirFactory} to allow inspecting
 * the field or parameter the {@link org.junit.jupiter.api.io.TempDir TempDir}
 * annotation is declared on.
 *
 * <p>This interface is not intended to be implemented by clients.
 *
 * @since 5.10
 */
@API(status = MAINTAINED, since = "5.13.3")
public interface AnnotatedElementContext {

	/**
	 * Get the {@link AnnotatedElement} for this context.
	 *
	 * <h4>WARNING</h4>
	 * <p>When searching for annotations on the annotated element in this context,
	 * favor {@link #isAnnotated(Class)}, {@link #findAnnotation(Class)}, and
	 * {@link #findRepeatableAnnotations(Class)} over methods in the
	 * {@link AnnotatedElement} API due to a bug in {@code javac} on JDK versions prior
	 * to JDK 9.
	 *
	 * @return the annotated element; never {@code null}
	 */
	AnnotatedElement getAnnotatedElement();

	/**
	 * Determine if an annotation of {@code annotationType} is either
	 * <em>present</em> or <em>meta-present</em> on the {@link AnnotatedElement} for
	 * this context.
	 *
	 * <p><strong>Note:</strong> This method does not find repeatable annotations.
	 * To check for repeatable annotations, use {@link #findRepeatableAnnotations(Class)}
	 * and verify that the returned list is not empty.
	 *
	 * <h4>WARNING</h4>
	 * <p>Favor the use of this method over directly invoking
	 * {@link AnnotatedElement#isAnnotationPresent(Class)} due to a bug in {@code javac}
	 * on JDK versions prior to JDK 9.
	 *
	 * @param annotationType the annotation type to search for; never {@code null}
	 * @return {@code true} if the annotation is present or meta-present
	 * @see #findAnnotation(Class)
	 * @see #findRepeatableAnnotations(Class)
	 */
	default boolean isAnnotated(Class<? extends Annotation> annotationType) {
		return AnnotationSupport.isAnnotated(getAnnotatedElement(), annotationType);
	}

	/**
	 * Find the first annotation of {@code annotationType} that is either
	 * <em>present</em> or <em>meta-present</em> on the {@link AnnotatedElement} for
	 * this context.
	 *
	 * <h4>WARNING</h4>
	 * <p>Favor the use of this method over directly invoking annotation lookup
	 * methods in the {@link AnnotatedElement} API due to a bug in {@code javac} on JDK
	 * versions prior to JDK 9.
	 *
	 * @param <A> the annotation type
	 * @param annotationType the annotation type to search for; never {@code null}
	 * @return an {@code Optional} containing the annotation; never {@code null} but
	 * potentially empty
	 * @see #isAnnotated(Class)
	 * @see #findRepeatableAnnotations(Class)
	 */
	default <A extends Annotation> Optional<A> findAnnotation(Class<A> annotationType) {
		return AnnotationSupport.findAnnotation(getAnnotatedElement(), annotationType);
	}

	/**
	 * Find all <em>repeatable</em> {@linkplain Annotation annotations} of
	 * {@code annotationType} that are either <em>present</em> or
	 * <em>meta-present</em> on the {@link AnnotatedElement} for this context.
	 *
	 * <h4>WARNING</h4>
	 * <p>Favor the use of this method over directly invoking annotation lookup
	 * methods in the {@link AnnotatedElement} API due to a bug in {@code javac} on JDK
	 * versions prior to JDK 9.
	 *
	 * @param <A> the annotation type
	 * @param annotationType the repeatable annotation type to search for; never
	 * {@code null}
	 * @return the list of all such annotations found; neither {@code null} nor
	 * mutable, but potentially empty
	 * @see #isAnnotated(Class)
	 * @see #findAnnotation(Class)
	 * @see java.lang.annotation.Repeatable
	 */
	default <A extends Annotation> List<A> findRepeatableAnnotations(Class<A> annotationType) {
		return AnnotationSupport.findRepeatableAnnotations(getAnnotatedElement(), annotationType);
	}

}
