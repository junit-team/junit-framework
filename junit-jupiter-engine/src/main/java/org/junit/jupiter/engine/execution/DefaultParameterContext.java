/*
 * Copyright 2015-2026 the original author or authors.
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v2.0 which
 * accompanies this distribution and is available at
 *
 * https://www.eclipse.org/legal/epl-v20.html
 */

package org.junit.jupiter.engine.execution;

import java.lang.annotation.Annotation;
import java.lang.reflect.Parameter;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.extension.ParameterContext;
import org.junit.platform.commons.util.AnnotationUtils;
import org.junit.platform.commons.util.Preconditions;
import org.junit.platform.commons.util.ToStringBuilder;

/**
 * @since 5.0
 */
record DefaultParameterContext(Parameter parameter, int index, Optional<Object> target) implements ParameterContext {

	DefaultParameterContext {
		Preconditions.condition(index >= 0, "index must be greater than or equal to zero");
		Preconditions.notNull(parameter, "parameter must not be null");
		Preconditions.notNull(target, "target must not be null");
	}

	@Override
	public Parameter getParameter() {
		return this.parameter;
	}

	@Override
	public int getIndex() {
		return this.index;
	}

	@Override
	public Optional<Object> getTarget() {
		return this.target;
	}

	@Override
	public boolean isAnnotated(Class<? extends Annotation> annotationType) {
		return AnnotationUtils.isAnnotated(this.parameter, this.index, annotationType);
	}

	@Override
	public <A extends Annotation> Optional<A> findAnnotation(Class<A> annotationType) {
		return AnnotationUtils.findAnnotation(this.parameter, this.index, annotationType);
	}

	@Override
	public <A extends Annotation> List<A> findRepeatableAnnotations(Class<A> annotationType) {
		return AnnotationUtils.findRepeatableAnnotations(this.parameter, this.index, annotationType);
	}

	@Override
	public String toString() {
		// @formatter:off
		return new ToStringBuilder(this)
				.append("parameter", this.parameter)
				.append("index", this.index)
				.append("target", this.target)
				.toString();
		// @formatter:on
	}

}
