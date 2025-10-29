/*
 * Copyright 2015-2025 the original author or authors.
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
import org.junit.jupiter.api.TestInstance.Lifecycle;

/**
 * {@code TestInstancePreConstructCallback} defines the API for {@link Extension
 * Extensions} that wish to be invoked <em>prior</em> to creation of test instances.
 *
 * <p>This extension is a symmetric counterpart to {@link TestInstancePreDestroyCallback}.
 * The use cases for this extension may include preparing context-sensitive arguments
 * that are injected into the instance's constructor.
 *
 * <p>Extensions that implement {@code TestInstancePreConstructCallback} must be
 * registered at the class level if the test class is configured with
 * {@link Lifecycle @TestInstance(Lifecycle.PER_CLASS)} semantics. If the test
 * class is configured with
 * {@link Lifecycle @TestInstance(Lifecycle.PER_METHOD)} semantics,
 * {@code TestInstancePreConstructCallback} extensions may be registered at the
 * class level or at the method level. In the latter case, the extension will
 * only be applied to the test method for which it is registered.
 *
 * <h2>Constructor Requirements</h2>
 *
 * <p>Consult the documentation in {@link Extension} for details on constructor
 * requirements.
 *
 * <h2>{@code ExtensionContext} Scope</h2>
 *
 * <p>As of JUnit Jupiter 5.12, this API participates in the
 * {@link TestInstantiationAwareExtension} contract. Implementations of this API
 * may therefore choose to override
 * {@link TestInstantiationAwareExtension#getTestInstantiationExtensionContextScope(ExtensionContext)
 * getTestInstantiationExtensionContextScope(ExtensionContext)} to require a
 * test-method scoped {@code ExtensionContext}. See
 * {@link #preConstructTestInstance(TestInstanceFactoryContext, ExtensionContext)}
 * for further details.
 *
 * @since 5.9
 * @see TestInstancePreDestroyCallback
 * @see TestInstanceFactory
 * @see ParameterResolver
 */
@FunctionalInterface
@API(status = STABLE, since = "5.11")
public interface TestInstancePreConstructCallback extends TestInstantiationAwareExtension {

	/**
	 * Callback invoked prior to test instances being constructed.
	 *
	 * <p>By default, the supplied {@link ExtensionContext} represents the test
	 * class that's about to be constructed. Extensions may override
	 * {@link #getTestInstantiationExtensionContextScope} to return
	 * {@link ExtensionContextScope#TEST_METHOD TEST_METHOD} in order to change
	 * the scope of the {@code ExtensionContext} to the test method, unless the
	 * {@link TestInstance.Lifecycle#PER_CLASS PER_CLASS} lifecycle is used.
	 * Changing the scope makes test-specific data available to the
	 * implementation of this method and allows keeping state on the test level
	 * by using the provided {@link ExtensionContext.Store Store} instance.
	 *
	 * @param factoryContext the context for the test instance about to be instantiated;
	 * never {@code null}
	 * @param context the current extension context; never {@code null}
	 */
	void preConstructTestInstance(TestInstanceFactoryContext factoryContext, ExtensionContext context) throws Exception;

}
