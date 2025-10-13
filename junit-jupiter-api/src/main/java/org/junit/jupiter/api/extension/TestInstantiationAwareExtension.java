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

import static org.apiguardian.api.API.Status.DEPRECATED;
import static org.apiguardian.api.API.Status.MAINTAINED;

import org.apiguardian.api.API;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.extension.ExtensionContext.Store;

/**
 * Interface for {@link Extension Extensions} that are aware and can influence
 * the instantiation of test instances.
 *
 * <p>This interface is not indented to be implemented directly. Instead, extend
 * one of its sub-interfaces.
 *
 * @since 5.12
 * @see InvocationInterceptor#interceptTestClassConstructor
 * @see ParameterResolver
 * @see TestInstancePreConstructCallback
 * @see TestInstancePostProcessor
 * @see TestInstanceFactory
 */
@API(status = MAINTAINED, since = "5.13.3")
public interface TestInstantiationAwareExtension extends Extension {

	/**
	 * Whether this extension should receive a test-scoped
	 * {@link ExtensionContext} during the instantiation of test instances.
	 *
	 * <p>If an extension returns
	 * {@link ExtensionContextScope#TEST_METHOD TEST_METHOD} from this method,
	 * the following extension methods will be called with a test-scoped
	 * {@link ExtensionContext} instead of a class-scoped one, unless the
	 * {@link TestInstance.Lifecycle#PER_CLASS PER_CLASS} lifecycle is used:
	 *
	 * <ul>
	 * <li>{@link InvocationInterceptor#interceptTestClassConstructor}</li>
	 * <li>{@link ParameterResolver} when resolving constructor parameters</li>
	 * <li>{@link TestInstancePreConstructCallback}</li>
	 * <li>{@link TestInstancePostProcessor}</li>
	 * <li>{@link TestInstanceFactory}</li>
	 * </ul>
	 *
	 * <p>In such cases, implementations of these extension callbacks can
	 * observe the following differences:
	 *
	 * <ul>
	 * <li>{@link ExtensionContext#getElement() getElement()} may refer to the
	 * test method and {@link ExtensionContext#getTestClass() getTestClass()}
	 * may refer to a nested test class.
	 * Use {@link TestInstanceFactoryContext#getTestClass()} to get the class
	 * under construction.</li>
	 * <li>{@link ExtensionContext#getTestMethod() getTestMethod()} is no longer
	 * empty, unless the {@link TestInstance.Lifecycle#PER_CLASS PER_CLASS}
	 * lifecycle is used.</li>
	 * <li>If the callback adds a new {@link Store.CloseableResource} or
	 * {@link AutoCloseable} to the {@link Store Store} (unless the
	 * {@code junit.jupiter.extensions.store.close.autocloseable.enabled}
	 * configuration parameter is set to {@code false}), then
	 * the resource is closed just after the instance is destroyed.</li>
	 * <li>The callbacks can now access data previously stored by
	 * {@link TestTemplateInvocationContext}, unless the
	 * {@link TestInstance.Lifecycle#PER_CLASS PER_CLASS} lifecycle is used.</li>
	 * </ul>
	 *
	 * <p><strong>Note</strong>: The behavior which is enabled by returning
	 * {@link ExtensionContextScope#TEST_METHOD TEST_METHOD} from this method
	 * will become the default in future versions of JUnit. To ensure forward
	 * compatibility, extension implementors are therefore advised to opt in,
	 * even if they don't require the new functionality.
	 *
	 * @implNote There are no guarantees about how often this method is called.
	 *           Therefore, implementations should be idempotent and avoid side
	 *           effects. They may, however, cache the result for performance in
	 *           the {@link Store Store} of the supplied
	 *           {@link ExtensionContext}, if necessary.
	 * @param rootContext the root extension context to allow inspection of
	 *                    configuration parameters; never {@code null}
	 * @since 5.12
	 */
	@API(status = MAINTAINED, since = "5.13.3")
	default ExtensionContextScope getTestInstantiationExtensionContextScope(ExtensionContext rootContext) {
		return ExtensionContextScope.DEFAULT;
	}

	/**
	 * {@code ExtensionContextScope} is used to define the scope of the
	 * {@link ExtensionContext} passed to an extension during the instantiation
	 * of test instances.
	 *
	 * @since 5.12
	 * @see TestInstantiationAwareExtension#getTestInstantiationExtensionContextScope
	 */
	@API(status = MAINTAINED, since = "5.13.3")
	enum ExtensionContextScope {

		/**
		 * The extension should receive an {@link ExtensionContext} scoped to
		 * in the <em>default</em> scope.
		 *
		 * <p>The default scope is determined by the configuration parameter
		 * {@link #DEFAULT_SCOPE_PROPERTY_NAME}. If not specified, extensions
		 * will receive an {@link ExtensionContext} scoped to the test class.
		 *
		 * @deprecated This behavior will be removed from future versions of
		 * JUnit and {@link #TEST_METHOD} will become the default.
		 *
		 * @see #DEFAULT_SCOPE_PROPERTY_NAME
		 */
		@API(status = DEPRECATED, since = "5.12") //
		@Deprecated(since = "5.12")
		DEFAULT,

		/**
		 * The extension should receive an {@link ExtensionContext} scoped to
		 * the test method, unless the
		 * {@link TestInstance.Lifecycle#PER_CLASS PER_CLASS} lifecycle is used.
		 */
		TEST_METHOD;

		/**
		 * Property name used to set the default extension context scope: {@value}
		 *
		 * <h4>Supported Values</h4>
		 *
		 * <p>Supported values include names of enum constants defined in this
		 * class, ignoring case.
		 *
		 * @see #DEFAULT
		 */
		public static final String DEFAULT_SCOPE_PROPERTY_NAME = "junit.jupiter.extensions.testinstantiation.extensioncontextscope.default";
	}

}
