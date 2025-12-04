/*
 * Copyright 2015-2025 the original author or authors.
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v2.0 which
 * accompanies this distribution and is available at
 *
 * https://www.eclipse.org/legal/epl-v20.html
 */

package org.junit.jupiter.params;

import java.lang.reflect.Method;

import org.junit.jupiter.params.provider.Arguments;
import org.junit.platform.commons.util.Preconditions;

/**
 * Encapsulates access to the parameters of a parameterized test method and
 * caches the converters and aggregators used to resolve them.
 *
 * @since 5.3
 */
class ParameterizedTestContext implements ParameterizedDeclarationContext<ParameterizedTestInvocationContext> {

	private final Class<?> testClass;
	private final Method method;
	private final ParameterizedTest annotation;
	private final ResolverFacade resolverFacade;

	ParameterizedTestContext(Class<?> testClass, Method method, ParameterizedTest annotation) {
		this.testClass = testClass;
		this.method = Preconditions.notNull(method, "method must not be null");
		this.annotation = Preconditions.notNull(annotation, "annotation must not be null");
		this.resolverFacade = ResolverFacade.create(method, annotation);
	}

	@Override
	public Class<?> getTestClass() {
		return this.testClass;
	}

	@Override
	public ParameterizedTest getAnnotation() {
		return this.annotation;
	}

	@Override
	public Method getAnnotatedElement() {
		return this.method;
	}

	@Override
	public String getDisplayNamePattern() {
		return this.annotation.name();
	}

	@Override
	public boolean quoteTextArguments() {
		return this.annotation.quoteTextArguments();
	}

	@Override
	public boolean isAutoClosingArguments() {
		return this.annotation.autoCloseArguments();
	}

	@Override
	public boolean isAllowingZeroInvocations() {
		return this.annotation.allowZeroInvocations();
	}

	@Override
	public ArgumentCountValidationMode getArgumentCountValidationMode() {
		return this.annotation.argumentCountValidation();
	}

	@Override
	public ResolverFacade getResolverFacade() {
		return this.resolverFacade;
	}

	@Override
	public ParameterizedTestInvocationContext createInvocationContext(ParameterizedInvocationNameFormatter formatter,
			Arguments arguments, int invocationIndex) {
		return new ParameterizedTestInvocationContext(this, formatter, arguments, invocationIndex);
	}

}
