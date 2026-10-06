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

import static java.util.Collections.emptySet;
import static java.util.Objects.requireNonNull;
import static org.apiguardian.api.API.Status.INTERNAL;
import static org.junit.jupiter.engine.extension.MutableExtensionRegistry.createRegistryFrom;

import java.lang.reflect.Method;
import java.util.List;
import java.util.OptionalInt;
import java.util.Set;
import java.util.function.UnaryOperator;
import java.util.stream.Stream;

import org.apiguardian.api.API;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.TestTemplate;
import org.junit.jupiter.api.extension.Extension;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.TestInstances;
import org.junit.jupiter.api.extension.TestTemplateComparisonProvider;
import org.junit.jupiter.api.extension.TestTemplateInvocationContext;
import org.junit.jupiter.engine.config.JupiterConfiguration;
import org.junit.jupiter.engine.execution.JupiterEngineExecutionContext;
import org.junit.jupiter.engine.extension.MutableExtensionRegistry;
import org.junit.platform.engine.TestDescriptor;
import org.junit.platform.engine.UniqueId;
import org.junit.platform.engine.support.hierarchical.ExclusiveResource;

/**
 * {@link TestDescriptor} for an invocation of a
 * {@link org.junit.jupiter.api.TestTemplate @TestTemplate} method that is
 * compared across comparison subjects: a container that encloses one
 * {@linkplain TestTemplateInvocationTestDescriptor execution} of the test
 * template method per comparison subject provided by the active
 * {@linkplain TestTemplateComparisonProvider
 * TestTemplateComparisonProviders}.
 *
 * @since 6.2
 * @see TestTemplateComparisonProvider
 */
@API(status = INTERNAL, since = "6.2")
public class TestTemplateComparisonTestDescriptor extends MethodBasedTestDescriptor implements Filterable {

	private @Nullable TestTemplateInvocationContext invocationContext;
	private final int index;
	private final DynamicDescendantFilter dynamicDescendantFilter;

	TestTemplateComparisonTestDescriptor(UniqueId uniqueId, Class<?> testClass, Method templateMethod,
			TestTemplateInvocationContext invocationContext, int index, DynamicDescendantFilter dynamicDescendantFilter,
			JupiterConfiguration configuration) {
		super(uniqueId, invocationContext.getDisplayName(index), testClass, templateMethod, configuration);
		this.invocationContext = invocationContext;
		this.index = index;
		this.dynamicDescendantFilter = dynamicDescendantFilter;
	}

	// --- JupiterTestDescriptor -----------------------------------------------

	@Override
	protected TestTemplateComparisonTestDescriptor withUniqueId(UnaryOperator<UniqueId> uniqueIdTransformer) {
		return new TestTemplateComparisonTestDescriptor(uniqueIdTransformer.apply(getUniqueId()), getTestClass(),
			getTestMethod(), requiredInvocationContext(), this.index,
			this.dynamicDescendantFilter.copy(uniqueIdTransformer), this.configuration);
	}

	// --- Filterable ----------------------------------------------------------

	@Override
	public DynamicDescendantFilter getDynamicDescendantFilter() {
		return this.dynamicDescendantFilter;
	}

	// --- TestDescriptor ------------------------------------------------------

	@Override
	public Type getType() {
		return Type.CONTAINER;
	}

	@Override
	public boolean mayRegisterTests() {
		return true;
	}

	@Override
	protected OptionalInt getLegacyReportingIndex() {
		return OptionalInt.of(this.index);
	}

	// --- Node ----------------------------------------------------------------

	@Override
	public Set<ExclusiveResource> getExclusiveResources() {
		// Resources are already collected and returned by the enclosing TestTemplateTestDescriptor
		return emptySet();
	}

	@Override
	public JupiterEngineExecutionContext prepare(JupiterEngineExecutionContext context) {
		MutableExtensionRegistry registry = context.getExtensionRegistry();
		List<Extension> additionalExtensions = requiredInvocationContext().getAdditionalExtensions();
		if (!additionalExtensions.isEmpty()) {
			MutableExtensionRegistry childRegistry = createRegistryFrom(registry, Stream.empty());
			additionalExtensions.forEach(
				extension -> childRegistry.registerExtension(extension, requiredInvocationContext()));
			registry = childRegistry;
		}

		// The test instance should be properly maintained by the enclosing class's ExtensionContext.
		TestInstances testInstances = context.getExtensionContext().getTestInstances().orElse(null);

		ExtensionContext extensionContext = new TestTemplateExtensionContext(context.getExtensionContext(),
			context.getExecutionListener(), this, context.getConfiguration(), registry,
			context.getLauncherStoreFacade(), testInstances);

		requiredInvocationContext().prepareInvocation(extensionContext);

		// @formatter:off
		return context.extend()
				.withExtensionRegistry(registry)
				.withExtensionContext(extensionContext)
				.build();
		// @formatter:on
	}

	@Override
	public JupiterEngineExecutionContext execute(JupiterEngineExecutionContext context,
			DynamicTestExecutor dynamicTestExecutor) {

		new ComparisonSubjectExecutor(this, getTestClass(), getTestMethod(), this.configuration) //
				.execute(context, dynamicTestExecutor);
		return context;
	}

	@Override
	public void cleanUp(JupiterEngineExecutionContext context) throws Exception {
		// forget invocationContext so it can be garbage collected
		this.invocationContext = null;
		super.cleanUp(context);
	}

	private TestTemplateInvocationContext requiredInvocationContext() {
		return requireNonNull(this.invocationContext);
	}

	/**
	 * Executes the test template method once per comparison subject provided
	 * by the active {@link TestTemplateComparisonProvider TestTemplateComparisonProviders},
	 * as children of the supplied parent: either a
	 * {@code TestTemplateComparisonTestDescriptor} or, if no invocation context
	 * provider is active, the test template itself.
	 */
	static final class ComparisonSubjectExecutor
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
			return parentUniqueId.append(TestTemplateInvocationTestDescriptor.COMPARISON_SUBJECT_SEGMENT_TYPE,
				"#" + index);
		}

		@Override
		TestDescriptor createInvocationTestDescriptor(UniqueId uniqueId, TestTemplateInvocationContext subject,
				int index) {
			return new TestTemplateInvocationTestDescriptor(uniqueId, this.testClass, this.templateMethod, subject,
				index, this.configuration);
		}
	}

}
