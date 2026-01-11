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
 * {@code TestInstancePostProcessor} defines the API for {@link Extension
 * Extensions} that wish to <em>post-process</em> test instances.
 *
 * <p>Common use cases include injecting dependencies into the test
 * instance, invoking custom initialization methods on the test instance,
 * etc.
 *
 * <p>Extensions that implement {@code TestInstancePostProcessor} must be
 * registered at the class level, {@linkplain ExtendWith declaratively} via a
 * field of the test class, or {@linkplain RegisterExtension programmatically}
 * via a <em>static</em> field of the test class.
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
 * {@link #postProcessTestInstance(Object, ExtensionContext)} for further details.
 *
 * @since 5.0
 * @see #postProcessTestInstance(Object, ExtensionContext)
 * @see TestInstancePreDestroyCallback
 * @see TestInstanceFactory
 * @see ParameterResolver
 */
@FunctionalInterface
@API(status = STABLE, since = "5.0")
public interface TestInstancePostProcessor extends TestInstantiationAwareExtension {

	/**
	 * Callback for post-processing the supplied test instance.
	 *
	 * <p>By default, the supplied {@link ExtensionContext} represents the test
	 * class that's being post-processed. Extensions may override
	 * {@link #getTestInstantiationExtensionContextScope} to return
	 * {@link ExtensionContextScope#TEST_METHOD TEST_METHOD} in order to change
	 * the scope of the {@code ExtensionContext} to the test method, unless the
	 * {@link TestInstance.Lifecycle#PER_CLASS PER_CLASS} lifecycle is used.
	 * Changing the scope makes test-specific data available to the
	 * implementation of this method and allows keeping state on the test level
	 * by using the provided {@link ExtensionContext.Store Store} instance.
	 *
	 * <p><strong>Note</strong>: the {@code ExtensionContext} supplied to a
	 * {@code TestInstancePostProcessor} will always return an empty
	 * {@link java.util.Optional} value from {@link ExtensionContext#getTestInstance()
	 * getTestInstance()}. A {@code TestInstancePostProcessor} should therefore
	 * only attempt to process the supplied {@code testInstance}.
	 *
	 * @param testInstance the instance to post-process; never {@code null}
	 * @param context the current extension context; never {@code null}
	 */
	void postProcessTestInstance(Object testInstance, ExtensionContext context) throws Exception;

}
