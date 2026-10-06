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
 * {@link Extension Extensions} that enable <em>comparison testing</em> for a
 * {@link org.junit.jupiter.api.TestTemplate @TestTemplate} method: executing
 * the same test against several <em>comparison subjects</em> &mdash; for
 * example, alternative implementations of an API, a real server and its test
 * double, or several database backends &mdash; and reporting the outcome for
 * each subject separately.
 *
 * <p>Comparison subjects always form the innermost level of the test tree.
 * The invocation contexts of all active
 * {@link TestTemplateInvocationContextProvider TestTemplateInvocationContextProviders}
 * are chained as usual, but every resulting invocation becomes a container in
 * which the test template method is executed once per comparison subject. If
 * no {@code TestTemplateInvocationContextProvider} is active, the test template
 * method is executed once per comparison subject directly below the test
 * template. The comparison subjects of multiple active
 * {@code TestTemplateComparisonProviders} are chained.
 *
 * <p>A comparison subject is described by a {@link TestTemplateInvocationContext}
 * whose additional extensions &mdash; typically a {@link ParameterResolver}
 * that injects the subject &mdash; are registered in addition to those of the
 * enclosing invocation.
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
	 * <p>This method is called for the test template and again for each of
	 * its invocations; it must return the same result for all of these calls.
	 *
	 * @param context the extension context for the test template method or
	 * one of its invocations; never {@code null}
	 * @return {@code true} if this provider can provide comparison subjects
	 */
	boolean supportsComparison(ExtensionContext context);

	/**
	 * Provide the comparison subjects for an invocation of the test template
	 * method represented by the supplied {@code context}.
	 *
	 * <p>This method is called once for every invocation of the test template
	 * if {@link #supportsComparison} returned {@code true}. It must return a
	 * non-empty {@code Stream}, which will be properly closed by calling
	 * {@link Stream#close()}.
	 *
	 * @param context the extension context for the invocation of the test
	 * template method about to be executed; never {@code null}
	 * @return a {@code Stream} of {@code TestTemplateInvocationContext}
	 * instances, one per comparison subject; never {@code null} or empty
	 * @throws TemplateInvocationValidationException if validation fails while
	 * providing or closing the {@link Stream}
	 */
	Stream<? extends TestTemplateInvocationContext> provideComparisonSubjects(ExtensionContext context);

}
