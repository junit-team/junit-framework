/*
 * Copyright 2015-2026 the original author or authors.
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v2.0 which
 * accompanies this distribution and is available at
 *
 * https://www.eclipse.org/legal/epl-v20.html
 */

package org.junit.jupiter.params;

import java.lang.annotation.Annotation;
import java.lang.reflect.AnnotatedElement;

import org.junit.jupiter.params.provider.Arguments;

/**
 * @since 5.13
 */
interface ParameterizedDeclarationContext<C> {

	Class<?> getTestClass();

	Annotation getAnnotation();

	AnnotatedElement getAnnotatedElement();

	String getDisplayNamePattern();

	boolean quoteTextArguments();

	boolean isAutoClosingArguments();

	boolean isAllowingZeroInvocations();

	ArgumentCountValidationMode getArgumentCountValidationMode();

	default String getAnnotationName() {
		return getAnnotation().annotationType().getSimpleName();
	}

	ResolverFacade getResolverFacade();

	C createInvocationContext(ParameterizedInvocationNameFormatter formatter, Arguments arguments, int invocationIndex);

}
