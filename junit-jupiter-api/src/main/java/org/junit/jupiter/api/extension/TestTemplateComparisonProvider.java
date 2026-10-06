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

import static org.apiguardian.api.API.Status.EXPERIMENTAL;

import java.util.stream.Stream;

import org.apiguardian.api.API;

/**
 * {@code TestTemplateComparisonProvider} defines the API for
 * {@link Extension Extensions} that wish to enable <em>comparison testing</em>
 * for a {@link org.junit.jupiter.api.TestTemplate @TestTemplate} method.
 *
 * <p>Comparison testing executes the same test against several
 * <em>comparison subjects</em> &mdash; for example, alternative
 * implementations of an API, a real server and its test double, or several
 * database backends &mdash; and reports the outcome for each subject
 * separately, so that it is immediately visible whether a behavior fails for
 * all subjects or only for some of them.
 *
 * <h2>Test Tree</h2>
 *
 * <p>Comparison subjects always form the innermost level of the test tree.
 * The {@linkplain TestTemplateInvocationContext invocation contexts} of all
 * active {@link TestTemplateInvocationContextProvider
 * TestTemplateInvocationContextProviders} are chained as usual, but every
 * resulting invocation becomes a container in which the test template method
 * is executed once per comparison subject. For example, a
 * {@code @ParameterizedTest} with two sets of arguments that is compared
 * across two subjects results in the following tree.
 *
 * <pre>
 * test(String, Api)
 * ├─ [1] value = "foo"
 * │  ├─ subject A
 * │  └─ subject B
 * └─ [2] value = "bar"
 *    ├─ subject A
 *    └─ subject B
 * </pre>
 *
 * <p>If no {@code TestTemplateInvocationContextProvider} is active for a test
 * template, the test template method is executed once per comparison subject
 * directly below the test template.
 *
 * <h2>Comparison Subjects</h2>
 *
 * <p>A comparison subject is described by a {@link TestTemplateInvocationContext}
 * that supplies its display name and the additional extensions that are
 * registered for the execution of the test template method against that
 * subject, in addition to the extensions of the enclosing invocation context.
 * Typically, these include a {@link ParameterResolver} that injects the
 * subject into the test method and its lifecycle methods. An
 * {@link ExecutionCondition} among them may disable the test for an individual
 * subject; the corresponding test is then reported as skipped.
 *
 * <h2>Methods</h2>
 *
 * <p>This interface defines two methods: {@link #supportsComparison} and
 * {@link #provideComparisonSubjects}. The former is called by the framework to
 * determine whether this extension wants to compare a test template that is
 * about to be executed across comparison subjects. If so, the latter is called
 * once for every invocation of the test template and must return a non-empty
 * {@link Stream} of {@link TestTemplateInvocationContext} instances, one per
 * comparison subject. Otherwise, this provider is ignored for the execution of
 * the current test template.
 *
 * <p>If multiple {@code TestTemplateComparisonProviders} are active for a test
 * template, the comparison subjects of all of them are chained.
 *
 * <h2>Constructor Requirements</h2>
 *
 * <p>Consult the documentation in {@link Extension} for details on constructor
 * requirements.
 *
 * @since 6.2
 * @see org.junit.jupiter.api.TestTemplate
 * @see TestTemplateInvocationContext
 * @see TestTemplateInvocationContextProvider
 */
@API(status = EXPERIMENTAL, since = "6.2")
public interface TestTemplateComparisonProvider extends Extension {

	/**
	 * Determine if this provider supports comparing the test template method
	 * represented by the supplied {@code context} across comparison subjects.
	 *
	 * <p>This method is called once for the test template, to determine the
	 * shape of its test tree, and once for every invocation of the test
	 * template, before {@link #provideComparisonSubjects} is called; it must
	 * return the same result for all of these calls.
	 *
	 * @param context the extension context for the test template method or
	 * one of its invocations; never {@code null}
	 * @return {@code true} if this provider can provide comparison subjects
	 * @see #provideComparisonSubjects
	 * @see ExtensionContext
	 */
	boolean supportsComparison(ExtensionContext context);

	/**
	 * Provide the comparison subjects for an invocation of the test template
	 * method represented by the supplied {@code context}.
	 *
	 * <p>This method is only called by the framework if {@link #supportsComparison}
	 * previously returned {@code true} for the test template method. It is
	 * called once for every invocation of the test template, with the extension
	 * context of that invocation, and must return a non-empty {@code Stream}.
	 * Returning an empty {@code Stream} is considered an execution error.
	 *
	 * <p>The returned {@code Stream} will be properly closed by calling
	 * {@link Stream#close()}, making it safe to use a resource such as
	 * {@link java.nio.file.Files#lines(java.nio.file.Path) Files.lines()}.
	 *
	 * @param context the extension context for the invocation of the test
	 * template method about to be executed; never {@code null}
	 * @return a {@code Stream} of {@code TestTemplateInvocationContext}
	 * instances, one per comparison subject; never {@code null} or empty
	 * @throws TemplateInvocationValidationException if validation fails while
	 * providing or closing the {@link Stream}
	 * @see #supportsComparison
	 * @see ExtensionContext
	 */
	Stream<? extends TestTemplateInvocationContext> provideComparisonSubjects(ExtensionContext context);

}
