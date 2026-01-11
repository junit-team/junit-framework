/*
 * Copyright 2015-2026 the original author or authors.
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v2.0 which
 * accompanies this distribution and is available at
 *
 * https://www.eclipse.org/legal/epl-v20.html
 */

package org.junit.jupiter.engine.extension;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.lang.reflect.Method;
import java.lang.reflect.Parameter;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestReporter;
import org.junit.jupiter.api.extension.ParameterContext;
import org.junit.platform.commons.support.ReflectionSupport;

/**
 * @since 5.0
 */
class TestReporterParameterResolverTests {

	TestReporterParameterResolver resolver = new TestReporterParameterResolver();

	@SuppressWarnings("DataFlowIssue")
	@Test
	void supports() {
		Parameter parameter1 = findParameterOfMethod("methodWithTestReporterParameter", TestReporter.class);
		assertTrue(this.resolver.supportsParameter(parameterContext(parameter1), null));

		Parameter parameter2 = findParameterOfMethod("methodWithoutTestReporterParameter", String.class);
		assertFalse(this.resolver.supportsParameter(parameterContext(parameter2), null));
	}

	@Test
	void resolve() {
		Parameter parameter = findParameterOfMethod("methodWithTestReporterParameter", TestReporter.class);

		TestReporter testReporter = this.resolver.resolveParameter(parameterContext(parameter), mock());
		assertNotNull(testReporter);
	}

	private Parameter findParameterOfMethod(String methodName, Class<?>... parameterTypes) {
		Method method = ReflectionSupport.findMethod(Sample.class, methodName, parameterTypes).orElseThrow();
		return method.getParameters()[0];
	}

	private static ParameterContext parameterContext(Parameter parameter) {
		ParameterContext parameterContext = mock();
		when(parameterContext.getParameter()).thenReturn(parameter);
		return parameterContext;
	}

	static class Sample {

		void methodWithTestReporterParameter(TestReporter reporter) {
		}

		void methodWithoutTestReporterParameter(String nothing) {
		}

	}

}
