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

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.apiguardian.api.API;

/**
 * {@code @RegisterExtension} is used to register an {@link Extension} via a
 * field in a test class.
 *
 * <p>In contrast to {@link ExtendWith @ExtendWith} which is used to register
 * extensions <em>declaratively</em>, {@code @RegisterExtension} can be used to
 * register an extension <em>programmatically</em> &mdash; for example, in order
 * to pass arguments to the extension's constructor, {@code static} factory
 * method, or builder API.
 *
 * <p>{@code @RegisterExtension} fields must not be {@code null} (when evaluated)
 * but may be either {@code static} or non-static.
 *
 * <h2>Static Fields</h2>
 *
 * <p>If a {@code @RegisterExtension} field is {@code static}, the extension
 * will be registered after extensions that are registered at the class level
 * via {@code @ExtendWith}. Such <em>static</em> extensions are not limited in
 * which extension APIs they can implement. Extensions registered via static
 * fields may therefore implement class-level and instance-level extension APIs
 * such as {@link BeforeAllCallback}, {@link AfterAllCallback},
 * {@link TestInstanceFactory}, {@link TestInstancePostProcessor} and
 * {@link TestInstancePreDestroyCallback} as well as method-level extension APIs
 * such as {@link BeforeEachCallback}, etc.
 *
 * <h2>Instance Fields</h2>
 *
 * <p>If a {@code @RegisterExtension} field is non-static (i.e., an instance
 * field), the extension will be registered after the test class has been
 * instantiated and after all {@link TestInstancePostProcessor
 * TestInstancePostProcessors} have been given a chance to post-process the
 * test instance (potentially injecting the instance of the extension to be
 * used into the annotated field). Thus, if such an <em>instance</em> extension
 * implements class-level or instance-level extension APIs such as
 * {@link BeforeAllCallback}, {@link AfterAllCallback},
 * {@link TestInstanceFactory}, or {@link TestInstancePostProcessor} those APIs
 * will not be honored. By default, an instance extension will be registered
 * <em>after</em> extensions that are registered at the method level via
 * {@code @ExtendWith}; however, if the test class is configured with
 * {@link org.junit.jupiter.api.TestInstance.Lifecycle @TestInstance(Lifecycle.PER_CLASS)}
 * semantics, an instance extension will be registered <em>before</em> extensions
 * that are registered at the method level via {@code @ExtendWith}.
 *
 * <h2>Inheritance</h2>
 *
 * <p>{@code @RegisterExtension} fields are inherited from superclasses.
 * Furthermore, {@code @RegisterExtension} fields from superclasses will be
 * registered before {@code @RegisterExtension} fields in subclasses unless
 * {@code @Order} is used to alter that behavior (see below).
 *
 * <h2>Registration Order</h2>
 *
 * <p>By default, if multiple extensions are registered via
 * {@code @RegisterExtension}, they will be ordered using an algorithm that is
 * deterministic but intentionally nonobvious. This ensures that subsequent runs
 * of a test suite execute extensions in the same order, thereby allowing for
 * repeatable builds. However, there are times when extensions need to be
 * registered in an explicit order. To achieve that, you can annotate
 * {@code @RegisterExtension} fields with {@link org.junit.jupiter.api.Order @Order}.
 * Any {@code @RegisterExtension} field not annotated with {@code @Order} will be
 * ordered using the {@link org.junit.jupiter.api.Order#DEFAULT default} order
 * value. Note that {@code @ExtendWith} fields can also be ordered with
 * {@code @Order}, relative to {@code @RegisterExtension} fields and other
 * {@code @ExtendWith} fields.
 *
 * <h2>Example Usage</h2>
 *
 * <p>In the following example, the {@code docs} field in the test class is
 * initialized programmatically by supplying a custom {@code lookUpDocsDir()}
 * method to a {@code static} factory method in the {@code DocumentationExtension}.
 * The configured {@code DocumentationExtension} will be automatically registered
 * as an extension. In addition, test methods can access the instance of the
 * extension via the {@code docs} field if necessary.
 *
 * <pre style="code">
 * class DocumentationTests {
 *
 *     static Path lookUpDocsDir() {
 *         // return path to docs dir
 *     }
 *
 *     {@literal @}RegisterExtension
 *     DocumentationExtension docs =
 *         DocumentationExtension.forPath(lookUpDocsDir());
 *
 *     {@literal @}Test
 *     void generateDocumentation() {
 *         // use docs ...
 *     }
 * }</pre>
 *
 * <h2>Supported Extension APIs</h2>
 *
 * <ul>
 * <li>{@link ExecutionCondition}</li>
 * <li>{@link InvocationInterceptor}</li>
 * <li>{@link BeforeAllCallback}</li>
 * <li>{@link AfterAllCallback}</li>
 * <li>{@link BeforeEachCallback}</li>
 * <li>{@link AfterEachCallback}</li>
 * <li>{@link BeforeTestExecutionCallback}</li>
 * <li>{@link AfterTestExecutionCallback}</li>
 * <li>{@link TestInstanceFactory}</li>
 * <li>{@link TestInstancePostProcessor}</li>
 * <li>{@link TestInstancePreConstructCallback}</li>
 * <li>{@link TestInstancePreDestroyCallback}</li>
 * <li>{@link ParameterResolver}</li>
 * <li>{@link LifecycleMethodExecutionExceptionHandler}</li>
 * <li>{@link TestExecutionExceptionHandler}</li>
 * <li>{@link TestTemplateInvocationContextProvider}</li>
 * <li>{@link TestWatcher}</li>
 * </ul>
 *
 * @since 5.1
 * @see ExtendWith @ExtendWith
 * @see Extension
 * @see org.junit.jupiter.api.Order @Order
 */
@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@API(status = STABLE, since = "5.1")
public @interface RegisterExtension {
}
