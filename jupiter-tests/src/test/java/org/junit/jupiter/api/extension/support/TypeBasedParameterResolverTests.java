/*
 * Copyright 2015-2025 the original author or authors.
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v2.0 which
 * accompanies this distribution and is available at
 *
 * https://www.eclipse.org/legal/epl-v20.html
 */

package org.junit.jupiter.api.extension.support;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.platform.commons.test.PreconditionAssertions.assertPreconditionViolationFor;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.ParameterContext;
import org.junit.jupiter.api.extension.ParameterResolutionException;
import org.junit.jupiter.api.extension.ParameterResolver;
import org.junit.platform.commons.support.ReflectionSupport;

/**
 * @since 5.6
 */
class TypeBasedParameterResolverTests {

	private final ParameterResolver basicTypeBasedParameterResolver = new BasicTypeBasedParameterResolver();
	private final ParameterResolver subClassedBasicTypeBasedParameterResolver = new SubClassedBasicTypeBasedParameterResolver();
	private final ParameterResolver parametrizedTypeBasedParameterResolver = new ParameterizedTypeBasedParameterResolver();

	@Test
	void missingTypeTypeBasedParameterResolver() {
		assertPreconditionViolationFor(MissingTypeTypeBasedParameterResolver::new)//
				.withMessage("Failed to discover parameter type supported by "
						+ MissingTypeTypeBasedParameterResolver.class.getName()
						+ "; potentially caused by lacking parameterized type in class declaration.");
	}

	@Test
	void supportsParameterForBasicTypes() {
		Parameter parameter1 = findParameterOfMethod("methodWithBasicTypeParameter", String.class);
		assertTrue(basicTypeBasedParameterResolver.supportsParameter(parameterContext(parameter1), mock()));
		assertTrue(subClassedBasicTypeBasedParameterResolver.supportsParameter(parameterContext(parameter1), mock()));

		Parameter parameter2 = findParameterOfMethod("methodWithObjectParameter", Object.class);
		assertFalse(basicTypeBasedParameterResolver.supportsParameter(parameterContext(parameter2), mock()));
	}

	@Test
	void supportsParameterForParameterizedTypes() {
		Parameter parameter1 = findParameterOfMethod("methodWithParameterizedTypeParameter", Map.class);
		assertTrue(parametrizedTypeBasedParameterResolver.supportsParameter(parameterContext(parameter1), mock()));

		Parameter parameter3 = findParameterOfMethod("methodWithAnotherParameterizedTypeParameter", Map.class);
		assertFalse(parametrizedTypeBasedParameterResolver.supportsParameter(parameterContext(parameter3), mock()));
	}

	@Test
	void resolve() {
		ExtensionContext extensionContext = extensionContext();
		ParameterContext parameterContext = parameterContext(
			findParameterOfMethod("methodWithBasicTypeParameter", String.class));
		assertEquals("Displaying TestAnnotation",
			basicTypeBasedParameterResolver.resolveParameter(parameterContext, extensionContext));

		Parameter parameter2 = findParameterOfMethod("methodWithParameterizedTypeParameter", Map.class);
		assertEquals(Map.of("ids", List.of(1, 42)),
			parametrizedTypeBasedParameterResolver.resolveParameter(parameterContext(parameter2), extensionContext));
	}

	private static ParameterContext parameterContext(Parameter parameter) {
		ParameterContext parameterContext = mock();
		when(parameterContext.getParameter()).thenReturn(parameter);
		return parameterContext;
	}

	private static ExtensionContext extensionContext() {
		ExtensionContext extensionContext = mock();
		when(extensionContext.getDisplayName()).thenReturn("Displaying");
		return extensionContext;
	}

	private Parameter findParameterOfMethod(String methodName, Class<?>... parameterTypes) {
		Method method = ReflectionSupport.findMethod(Sample.class, methodName, parameterTypes).orElseThrow();
		return method.getParameters()[0];
	}

	// -------------------------------------------------------------------------

	@SuppressWarnings("rawtypes")
	static class MissingTypeTypeBasedParameterResolver extends TypeBasedParameterResolver {

		@Override
		public Object resolveParameter(ParameterContext parameterContext, ExtensionContext extensionContext) {
			return "enigma";
		}
	}

	static class BasicTypeBasedParameterResolver extends TypeBasedParameterResolver<String> {

		@Override
		public String resolveParameter(ParameterContext parameterContext, ExtensionContext extensionContext)
				throws ParameterResolutionException {
			Class<?> parameterAnnotation = parameterContext.getParameter().getAnnotations()[0].annotationType();
			return "%s %s".formatted(extensionContext.getDisplayName(), parameterAnnotation.getSimpleName());
		}
	}

	static class SubClassedBasicTypeBasedParameterResolver extends BasicTypeBasedParameterResolver {
	}

	static class ParameterizedTypeBasedParameterResolver
			extends TypeBasedParameterResolver<Map<String, List<Integer>>> {

		@Override
		public Map<String, List<Integer>> resolveParameter(ParameterContext parameterContext,
				ExtensionContext extensionContext) throws ParameterResolutionException {
			return Map.of("ids", List.of(1, 42));
		}
	}

	@Retention(RetentionPolicy.RUNTIME)
	@Target(ElementType.PARAMETER)
	@interface TestAnnotation {
	}

	static class Sample {

		void methodWithBasicTypeParameter(@TestAnnotation String string) {
		}

		void methodWithObjectParameter(Object nothing) {
		}

		void methodWithParameterizedTypeParameter(Map<String, List<Integer>> map) {
		}

		void methodWithAnotherParameterizedTypeParameter(Map<String, List<Object>> nothing) {
		}
	}

}
