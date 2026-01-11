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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;

import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.ParameterResolver;
import org.junit.jupiter.engine.config.JupiterConfiguration;
import org.junit.jupiter.engine.execution.ParameterResolutionUtilsTests.ConfigurableParameterResolver;
import org.junit.jupiter.engine.execution.ParameterResolutionUtilsTests.ConstructorInjectionTestCase;
import org.junit.jupiter.engine.execution.ParameterResolutionUtilsTests.MethodSource;
import org.junit.jupiter.engine.execution.ParameterResolutionUtilsTests.NumberParameterResolver;
import org.junit.jupiter.engine.execution.ParameterResolutionUtilsTests.StringParameterResolver;
import org.junit.jupiter.engine.extension.MutableExtensionRegistry;
import org.junit.platform.commons.support.ReflectionSupport;
import org.junit.platform.commons.util.ReflectionUtils;

/**
 * @since 5.9
 */
abstract class AbstractExecutableInvokerTests {

	private static final String ENIGMA = "enigma";

	protected final MethodSource instance = mock();

	protected @Nullable Method method;

	protected final ExtensionContext extensionContext = mock();

	private final JupiterConfiguration configuration = mock();

	protected final MutableExtensionRegistry extensionRegistry = MutableExtensionRegistry.createRegistryWithDefaultExtensions(
		configuration);

	@Test
	void constructorInjection() {
		register(new StringParameterResolver(), new NumberParameterResolver());

		Class<ConstructorInjectionTestCase> outerClass = ConstructorInjectionTestCase.class;
		Constructor<ConstructorInjectionTestCase> constructor = ReflectionUtils.getDeclaredConstructor(outerClass);
		ConstructorInjectionTestCase outer = invokeConstructor(constructor, null);

		assertNotNull(outer);
		assertEquals(ENIGMA, outer.str);

		Class<ConstructorInjectionTestCase.NestedTestCase> innerClass = ConstructorInjectionTestCase.NestedTestCase.class;
		Constructor<ConstructorInjectionTestCase.NestedTestCase> innerConstructor = ReflectionUtils.getDeclaredConstructor(
			innerClass);
		ConstructorInjectionTestCase.NestedTestCase inner = invokeConstructor(innerConstructor, outer);

		assertNotNull(inner);
		assertEquals(42, inner.num);
	}

	@Test
	void resolveArgumentsViaParameterResolver() {
		testMethodWithASingleStringParameter();
		thereIsAParameterResolverThatResolvesTheParameterTo("argument");

		invokeMethod();

		verify(instance).singleStringParameter("argument");
	}

	private void thereIsAParameterResolverThatResolvesTheParameterTo(Object argument) {
		register(ConfigurableParameterResolver.supportsAndResolvesTo(parameterContext -> argument));
	}

	private void testMethodWithASingleStringParameter() {
		this.method = ReflectionSupport.findMethod(this.instance.getClass(), "singleStringParameter",
			String.class).orElseThrow();
	}

	private void register(ParameterResolver... resolvers) {
		for (ParameterResolver resolver : resolvers) {
			extensionRegistry.registerExtension(resolver, this);
		}
	}

	abstract void invokeMethod();

	abstract <T> T invokeConstructor(Constructor<T> constructor, @Nullable Object outerInstance);

}
