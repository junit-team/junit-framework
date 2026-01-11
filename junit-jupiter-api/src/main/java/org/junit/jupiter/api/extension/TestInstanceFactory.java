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

import org.apiguardian.api.API;
import org.junit.jupiter.api.TestInstance;

/**
 * {@code TestInstanceFactory} defines the API for {@link Extension
 * Extensions} that wish to {@linkplain #createTestInstance create} test instances.
 *
 * <p>Common use cases include creating test instances with special construction
 * requirements or acquiring the test instance from a dependency injection
 * framework.
 *
 * <p>Extensions that implement {@code TestInstanceFactory} must be registered
 * at the class level.
 *
 * <h2>Warning</h2>
 *
 * <p>Only one {@code TestInstanceFactory} is allowed to be registered for any
 * given test class. Registering multiple factories for any single test class
 * will result in an exception being thrown for all tests in that class, in any
 * subclass, and in any nested class. Note that any {@code TestInstanceFactory}
 * registered in a {@linkplain Class#getSuperclass() superclass} or
 * {@linkplain Class#getEnclosingClass() enclosing} class (i.e., in the case of
 * a {@code @Nested} test class) is <em>inherited</em>. It is therefore the
 * user's responsibility to ensure that only a single {@code TestInstanceFactory}
 * is registered for any specific test class.
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
 * test-method scoped {@code ExtensionContext}. See
 * {@link #createTestInstance(TestInstanceFactoryContext, ExtensionContext)} for
 * further details.
 *
 * @since 5.3
 * @see #createTestInstance(TestInstanceFactoryContext, ExtensionContext)
 * @see TestInstanceFactoryContext
 * @see TestInstancePostProcessor
 * @see TestInstancePreDestroyCallback
 * @see ParameterResolver
 */
@FunctionalInterface
@API(status = STABLE, since = "5.7")
public interface TestInstanceFactory extends TestInstantiationAwareExtension {

	/**
	 * Callback for creating a test instance for the supplied context.
	 *
	 * <p>By default, the supplied {@link ExtensionContext} represents the test
	 * class that's about to be instantiated. Extensions may override
	 * {@link #getTestInstantiationExtensionContextScope} to return
	 * {@link ExtensionContextScope#TEST_METHOD TEST_METHOD} in order to change
	 * the scope of the {@code ExtensionContext} to the test method, unless the
	 * {@link TestInstance.Lifecycle#PER_CLASS PER_CLASS} lifecycle is used.
	 * Changing the scope makes test-specific data available to the
	 * implementation of this method and allows keeping state on the test level
	 * by using the provided {@link ExtensionContext.Store Store} instance.
	 *
	 * <p><strong>Note</strong>: the {@code ExtensionContext} supplied to a
	 * {@code TestInstanceFactory} will always return an empty
	 * {@link java.util.Optional} value from
	 * {@link ExtensionContext#getTestInstance() getTestInstance()} since the
	 * test instance cannot exist before it has been created by a
	 * {@code TestInstanceFactory} or the framework itself.
	 *
	 * @param factoryContext the context for the test class to be instantiated;
	 * never {@code null}
	 * @param extensionContext the current extension context; never {@code null}
	 * @return the test instance; never {@code null}
	 * @throws TestInstantiationException if an error occurs while creating the
	 * test instance
	 */
	Object createTestInstance(TestInstanceFactoryContext factoryContext, ExtensionContext extensionContext)
			throws TestInstantiationException;

}
