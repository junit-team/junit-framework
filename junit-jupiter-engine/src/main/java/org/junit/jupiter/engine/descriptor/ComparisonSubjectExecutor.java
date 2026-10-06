/*
 * Copyright 2015-2026 the original author or authors.
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v2.0 which
 * accompanies this distribution and is available at
 *
 * https://www.eclipse.org/legal/epl-v20.html
 */

package org.junit.jupiter.engine.descriptor;

import java.lang.reflect.Method;
import java.util.stream.Stream;

import org.junit.jupiter.api.TestTemplate;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.TestTemplateComparisonProvider;
import org.junit.jupiter.api.extension.TestTemplateInvocationContext;
import org.junit.jupiter.engine.config.JupiterConfiguration;
import org.junit.platform.engine.TestDescriptor;
import org.junit.platform.engine.UniqueId;

/**
 * Executes the test template method once per comparison subject provided by
 * the active {@link TestTemplateComparisonProvider TestTemplateComparisonProviders},
 * as children of the supplied parent: either a
 * {@linkplain TestTemplateComparisonTestDescriptor test template invocation}
 * or, if no invocation context provider is active, the test template itself.
 *
 * @since 6.2
 */
final class ComparisonSubjectExecutor
		extends TemplateExecutor<TestTemplateComparisonProvider, TestTemplateInvocationContext> {

	private final Class<?> testClass;
	private final Method templateMethod;
	private final JupiterConfiguration configuration;

	<T extends TestDescriptor & Filterable> ComparisonSubjectExecutor(T parent, Class<?> testClass,
			Method templateMethod, JupiterConfiguration configuration) {
		super(parent, TestTemplateComparisonProvider.class);
		this.testClass = testClass;
		this.templateMethod = templateMethod;
		this.configuration = configuration;
	}

	@Override
	boolean supports(TestTemplateComparisonProvider provider, ExtensionContext extensionContext) {
		return provider.supportsComparison(extensionContext);
	}

	@Override
	protected String getNoRegisteredProviderErrorMessage() {
		return "You must register at least one %s that supports @%s method [%s]".formatted(
			TestTemplateComparisonProvider.class.getSimpleName(), TestTemplate.class.getSimpleName(),
			this.templateMethod);
	}

	@Override
	Stream<? extends TestTemplateInvocationContext> provideContexts(TestTemplateComparisonProvider provider,
			ExtensionContext extensionContext) {
		return provider.provideComparisonSubjects(extensionContext);
	}

	@Override
	boolean mayReturnZeroContexts(TestTemplateComparisonProvider provider, ExtensionContext extensionContext) {
		return false;
	}

	@Override
	protected String getZeroContextsProvidedErrorMessage(TestTemplateComparisonProvider provider) {
		return "Provider [%s] did not provide any comparison subjects, but was expected to do so.".formatted(
			provider.getClass().getSimpleName());
	}

	@Override
	UniqueId createInvocationUniqueId(UniqueId parentUniqueId, int index) {
		return parentUniqueId.append(TestTemplateInvocationTestDescriptor.COMPARISON_SUBJECT_SEGMENT_TYPE, "#" + index);
	}

	@Override
	TestDescriptor createInvocationTestDescriptor(UniqueId uniqueId, TestTemplateInvocationContext subject, int index) {
		return new TestTemplateInvocationTestDescriptor(uniqueId, this.testClass, this.templateMethod, subject, index,
			this.configuration);
	}

}
