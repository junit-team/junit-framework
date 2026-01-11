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
import static org.apiguardian.api.API.Status.STABLE;

import java.lang.annotation.Annotation;
import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Executable;
import java.lang.reflect.Parameter;
import java.util.List;
import java.util.Optional;

import org.apiguardian.api.API;

/**
 * {@code ParameterContext} encapsulates the <em>context</em> in which an
 * {@link #getDeclaringExecutable Executable} will be invoked for a given
 * {@link #getParameter Parameter}.
 *
 * <p>A {@code ParameterContext} is used to support parameter resolution via
 * a {@link ParameterResolver}.
 *
 * @since 5.0
 * @see ParameterResolver
 * @see java.lang.reflect.Parameter
 * @see java.lang.reflect.Executable
 * @see java.lang.reflect.Method
 * @see java.lang.reflect.Constructor
 */
@API(status = STABLE, since = "5.0")
public interface ParameterContext extends AnnotatedElementContext {

	/**
	 * Get the {@link Parameter} for this context.
	 *
	 * <h4>WARNING</h4>
	 * <p>When searching for annotations on the parameter in this context,
	 * favor {@link #isAnnotated(Class)}, {@link #findAnnotation(Class)}, and
	 * {@link #findRepeatableAnnotations(Class)} over methods in the
	 * {@link Parameter} API due to a bug in {@code javac} on JDK versions prior
	 * to JDK 9.
	 *
	 * @return the parameter; never {@code null}
	 * @see #getIndex()
	 */
	Parameter getParameter();

	/**
	 * Get the index of the {@link Parameter} for this context within the
	 * parameter list of the {@link #getDeclaringExecutable Executable} that
	 * declares the parameter.
	 *
	 * @return the index of the parameter
	 * @see #getParameter()
	 * @see Executable#getParameters()
	 */
	int getIndex();

	/**
	 * Get the {@link Executable} (i.e., the {@link java.lang.reflect.Method} or
	 * {@link java.lang.reflect.Constructor}) that declares the {@code Parameter}
	 * for this context.
	 *
	 * @return the declaring {@code Executable}; never {@code null}
	 * @see Parameter#getDeclaringExecutable()
	 */
	default Executable getDeclaringExecutable() {
		return getParameter().getDeclaringExecutable();
	}

	/**
	 * Get the target on which the {@link #getDeclaringExecutable Executable}
	 * that declares the {@link #getParameter Parameter} for this context will
	 * be invoked, if available.
	 *
	 * @return an {@link Optional} containing the target on which the
	 * {@code Executable} will be invoked; never {@code null} but will be
	 * <em>empty</em> if the {@code Executable} is a constructor or a
	 * {@code static} method.
	 */
	Optional<Object> getTarget();

	/**
	 * {@inheritDoc}
	 * @since 5.10
	 */
	@API(status = MAINTAINED, since = "5.13.3")
	@Override
	default AnnotatedElement getAnnotatedElement() {
		return getParameter();
	}

	/**
	 * {@inheritDoc}
	 * @since 5.1.1
	 */
	@API(status = STABLE, since = "5.10")
	@Override
	default boolean isAnnotated(Class<? extends Annotation> annotationType) {
		return AnnotatedElementContext.super.isAnnotated(annotationType);
	}

	/**
	 * {@inheritDoc}
	 * @since 5.1.1
	 */
	@API(status = STABLE, since = "5.10")
	@Override
	default <A extends Annotation> Optional<A> findAnnotation(Class<A> annotationType) {
		return AnnotatedElementContext.super.findAnnotation(annotationType);
	}

	/**
	 * {@inheritDoc}
	 * @since 5.1.1
	 */
	@API(status = STABLE, since = "5.10")
	@Override
	default <A extends Annotation> List<A> findRepeatableAnnotations(Class<A> annotationType) {
		return AnnotatedElementContext.super.findRepeatableAnnotations(annotationType);
	}

}
