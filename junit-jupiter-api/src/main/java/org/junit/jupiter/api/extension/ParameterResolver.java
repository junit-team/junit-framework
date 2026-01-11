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

import static org.apiguardian.api.API.Status.STABLE;

import java.lang.reflect.Parameter;

import org.apiguardian.api.API;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.TestInstance;

/**
 * {@code ParameterResolver} defines the API for {@link Extension Extensions}
 * that wish to dynamically resolve arguments for {@linkplain Parameter parameters}
 * at runtime.
 *
 * <p>If a constructor for a test class or a
 * {@link org.junit.jupiter.api.Test @Test},
 * {@link org.junit.jupiter.api.BeforeEach @BeforeEach},
 * {@link org.junit.jupiter.api.AfterEach @AfterEach},
 * {@link org.junit.jupiter.api.BeforeAll @BeforeAll}, or
 * {@link org.junit.jupiter.api.AfterAll @AfterAll} method declares a parameter,
 * an argument for the parameter must be resolved at runtime by a
 * {@code ParameterResolver}.
 *
 * <p>By default, when the methods in this interface are called for a test class
 * constructor, the supplied {@link ExtensionContext} represents the test
 * class that's about to be instantiated. Extensions may override
 * {@link #getTestInstantiationExtensionContextScope} to return
 * {@link ExtensionContextScope#TEST_METHOD TEST_METHOD} in order to change
 * the scope of the {@code ExtensionContext} to the test method, unless the
 * {@link TestInstance.Lifecycle#PER_CLASS PER_CLASS} lifecycle is used.
 * Changing the scope makes test-specific data available to the
 * implementation of this method and allows keeping state on the test level
 * by using the provided {@link ExtensionContext.Store Store} instance.
 *
 * <h2>Constructor Requirements</h2>
 *
 * <p>Consult the documentation in {@link Extension} for details on
 * constructor requirements.
 *
 * <h2>{@code ExtensionContext} Scope</h2>
 *
 * <p>As of JUnit Jupiter 5.12, this API participates in the
 * {@link TestInstantiationAwareExtension} contract. Implementations of this API
 * may therefore choose to override
 * {@link TestInstantiationAwareExtension#getTestInstantiationExtensionContextScope(ExtensionContext)
 * getTestInstantiationExtensionContextScope(ExtensionContext)} to require a
 * test-method scoped {@code ExtensionContext}.
 *
 * @since 5.0
 * @see #supportsParameter(ParameterContext, ExtensionContext)
 * @see #resolveParameter(ParameterContext, ExtensionContext)
 * @see ParameterContext
 * @see TestInstanceFactory
 * @see TestInstancePostProcessor
 * @see TestInstancePreDestroyCallback
 */
@API(status = STABLE, since = "5.0")
public interface ParameterResolver extends TestInstantiationAwareExtension {

	/**
	 * Determine if this resolver supports resolution of an argument for the
	 * {@link Parameter} in the supplied {@link ParameterContext} for the supplied
	 * {@link ExtensionContext}.
	 *
	 * <p>The {@link java.lang.reflect.Method} or {@link java.lang.reflect.Constructor}
	 * in which the parameter is declared can be retrieved via
	 * {@link ParameterContext#getDeclaringExecutable()}.
	 *
	 * @param parameterContext the context for the parameter for which an argument should
	 * be resolved; never {@code null}
	 * @param extensionContext the extension context for the {@code Executable}
	 * about to be invoked; never {@code null}
	 * @return {@code true} if this resolver can resolve an argument for the parameter
	 * @see #resolveParameter
	 * @see ParameterContext
	 */
	boolean supportsParameter(ParameterContext parameterContext, ExtensionContext extensionContext)
			throws ParameterResolutionException;

	/**
	 * Resolve an argument for the {@link Parameter} in the supplied {@link ParameterContext}
	 * for the supplied {@link ExtensionContext}.
	 *
	 * <p>This method is only called by the framework if {@link #supportsParameter}
	 * previously returned {@code true} for the same {@link ParameterContext}
	 * and {@link ExtensionContext}.
	 *
	 * <p>The {@link java.lang.reflect.Method} or {@link java.lang.reflect.Constructor}
	 * in which the parameter is declared can be retrieved via
	 * {@link ParameterContext#getDeclaringExecutable()}.
	 *
	 * @param parameterContext the context for the parameter for which an argument should
	 * be resolved; never {@code null}
	 * @param extensionContext the extension context for the {@code Executable}
	 * about to be invoked; never {@code null}
	 * @return the resolved argument for the parameter; may only be {@code null} if the
	 * parameter type is not a primitive
	 * @see #supportsParameter
	 * @see ParameterContext
	 */
	@Nullable
	Object resolveParameter(ParameterContext parameterContext, ExtensionContext extensionContext)
			throws ParameterResolutionException;

}
