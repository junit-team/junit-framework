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

import static org.apiguardian.api.API.Status.DEPRECATED;
import static org.apiguardian.api.API.Status.MAINTAINED;

import org.apiguardian.api.API;

/**
 * {@code TestInstantiationAwareExtension} defines the API for {@link Extension
 * Extensions} that are aware of or influence the instantiation of test classes.
 *
 * <p>This interface is not intended to be implemented directly. Instead, extensions
 * should implement one of the sub-interfaces listed below.
 *
 * <ul>
 * <li>{@link InvocationInterceptor}</li>
 * <li>{@link ParameterResolver}</li>
 * <li>{@link TestInstancePreConstructCallback}</li>
 * <li>{@link TestInstancePostProcessor}</li>
 * <li>{@link TestInstanceFactory}</li>
 * </ul>
 *
 * <p>See {@link #getTestInstantiationExtensionContextScope(ExtensionContext)} for
 * further details.
 *
 * @since 5.12
 */
@API(status = MAINTAINED, since = "5.13.3")
public interface TestInstantiationAwareExtension extends Extension {

	/**
	 * Determine whether this extension should receive a test-method scoped
	 * {@link ExtensionContext} during the instantiation of test classes or
	 * processing of test instances.
	 *
	 * <p>If an extension returns {@link ExtensionContextScope#TEST_METHOD TEST_METHOD}
	 * from this method, methods defined in the following extension APIs will be
	 * called with a test-method scoped {@code ExtensionContext} instead of a
	 * test-class scoped context. Note, however, that a test-class scoped context
	 * will always be supplied if the
	 * {@link org.junit.jupiter.api.TestInstance.Lifecycle#PER_CLASS PER_CLASS}
	 * test instance lifecycle is used.
	 *
	 * <ul>
	 * <li>{@link InvocationInterceptor}: only the
	 * {@link InvocationInterceptor#interceptTestClassConstructor
	 * interceptTestClassConstructor(...)} method</li>
	 * <li>{@link ParameterResolver}: only when resolving constructor parameters</li>
	 * <li>{@link TestInstancePreConstructCallback}</li>
	 * <li>{@link TestInstancePostProcessor}</li>
	 * <li>{@link TestInstanceFactory}</li>
	 * </ul>
	 *
	 * <p>When a test-method scoped {@code ExtensionContext} is supplied, implementations
	 * of the above extension APIs will observe the following differences.
	 *
	 * <ul>
	 *   <li>
	 *     {@link ExtensionContext#getElement() getElement()} may refer to the
	 *     test method.
	 *   </li>
	 *   <li>
	 *     {@link ExtensionContext#getTestClass() getTestClass()} may refer to a
	 *     nested test class.
	 *     <ul>
	 *       <li>
	 *         For {@link TestInstancePostProcessor}, use {@code testInstance.getClass()}
	 *         to get the test class associated with the supplied instance.
	 *       </li>
	 *       <li>
	 *         For {@link TestInstanceFactory} and {@link TestInstancePreConstructCallback},
	 *         use {@link TestInstanceFactoryContext#getTestClass()} to get the
	 *         class under construction.
	 *       </li>
	 *       <li>
	 *         For {@link ParameterResolver}, when resolving a parameter for a
	 *         constructor, ensure that the
	 *         {@link ParameterContext#getDeclaringExecutable() Executable} is a
	 *         {@link java.lang.reflect.Constructor Constructor}, and then use
	 *         {@code constructor.getDeclaringClass()} to get the test class
	 *         associated with the constructor.
	 *       </li>
	 *     </ul>
	 *   </li>
	 *   <li>
	 *     {@link ExtensionContext#getTestMethod() getTestMethod()} is no longer
	 *     empty, unless the
	 *     {@link org.junit.jupiter.api.TestInstance.Lifecycle#PER_CLASS PER_CLASS}
	 *     test instance lifecycle is used.
	 *   </li>
	 *   <li>
	 *     If the extension adds a {@link ExtensionContext.Store.CloseableResource
	 *     CloseableResource} or {@link AutoCloseable} to the
	 *     {@link ExtensionContext.Store Store} (unless the
	 *     {@code junit.jupiter.extensions.store.close.autocloseable.enabled}
	 *     configuration parameter is set to {@code false}), then the resource will
	 *     be closed just after the instance is destroyed.
	 *   </li>
	 *   <li>
	 *     Extensions can now access data previously stored by a
	 *     {@link TestTemplateInvocationContext}, unless the
	 *     {@link org.junit.jupiter.api.TestInstance.Lifecycle#PER_CLASS PER_CLASS}
	 *     test instance lifecycle is used.
	 *   </li>
	 * </ul>
	 *
	 * <p><strong>Note</strong>: The behavior which is enabled by returning
	 * {@link ExtensionContextScope#TEST_METHOD TEST_METHOD} from this method
	 * will become the default in future versions of JUnit. To ensure forward
	 * compatibility, extension authors are therefore advised to opt into this
	 * feature, even if they do not require the new functionality.
	 *
	 * @implNote There are no guarantees about how often this method will be called.
	 * Therefore, implementations should be idempotent and avoid side effects.
	 * If computation of the return value is costly, implementations may wish to
	 * cache the result in the {@link ExtensionContext.Store Store} of the supplied
	 * {@code ExtensionContext}.
	 * @param rootContext the root extension context to allow inspection of
	 * configuration parameters; never {@code null}
	 * @since 5.12
	 */
	@API(status = MAINTAINED, since = "5.13.3")
	default ExtensionContextScope getTestInstantiationExtensionContextScope(ExtensionContext rootContext) {
		return ExtensionContextScope.DEFAULT;
	}

	/**
	 * {@code ExtensionContextScope} is used to define the scope of the
	 * {@link ExtensionContext} supplied to an extension during the instantiation
	 * of test classes or processing of test instances.
	 *
	 * @since 5.12
	 * @see TestInstantiationAwareExtension#getTestInstantiationExtensionContextScope(ExtensionContext)
	 */
	@API(status = MAINTAINED, since = "5.13.3")
	enum ExtensionContextScope {

		/**
		 * The extension should receive an {@link ExtensionContext} for the
		 * the <em>default</em> scope.
		 *
		 * <p>The default scope is determined by the configuration parameter
		 * {@link #DEFAULT_SCOPE_PROPERTY_NAME}. If not specified, extensions
		 * will receive an {@link ExtensionContext} scoped to the test class.
		 *
		 * @deprecated This behavior will be removed from future versions of
		 * JUnit, and {@link #TEST_METHOD} will become the default.
		 *
		 * @see #DEFAULT_SCOPE_PROPERTY_NAME
		 */
		@Deprecated(since = "5.12") //
		@API(status = DEPRECATED, since = "5.12")
		DEFAULT,

		/**
		 * The extension should receive an {@link ExtensionContext} scoped to
		 * the test method.
		 *
		 * <p>Note, however, that a test-class scoped context will always be
		 * supplied if the
		 * {@link org.junit.jupiter.api.TestInstance.Lifecycle#PER_CLASS PER_CLASS}
		 * test instance lifecycle is used.
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
