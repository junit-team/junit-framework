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

import static java.util.stream.Collectors.toCollection;
import static org.apiguardian.api.API.Status.INTERNAL;
import static org.junit.jupiter.api.TestInstance.Lifecycle.PER_METHOD;
import static org.junit.jupiter.engine.descriptor.LifecycleMethodUtils.validateClassTemplateInvocationLifecycleMethodsAreDeclaredCorrectly;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.function.UnaryOperator;
import java.util.stream.Stream;

import org.apiguardian.api.API;
import org.junit.jupiter.api.ClassTemplate;
import org.junit.jupiter.api.extension.ClassTemplateInvocationContext;
import org.junit.jupiter.api.extension.ClassTemplateInvocationContextProvider;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.TestInstances;
import org.junit.jupiter.api.parallel.ResourceLocksProvider;
import org.junit.jupiter.engine.execution.ExtensionContextSupplier;
import org.junit.jupiter.engine.execution.JupiterEngineExecutionContext;
import org.junit.jupiter.engine.extension.ExtensionRegistry;
import org.junit.platform.engine.TestDescriptor;
import org.junit.platform.engine.TestTag;
import org.junit.platform.engine.UniqueId;
import org.junit.platform.engine.support.discovery.DiscoveryIssueReporter;
import org.junit.platform.engine.support.hierarchical.ExclusiveResource;
import org.junit.platform.engine.support.hierarchical.Node;

/**
 * @since 5.13
 */
@API(status = INTERNAL, since = "5.13")
public class ClassTemplateTestDescriptor extends ClassBasedTestDescriptor implements Filterable {

	public static final String STANDALONE_CLASS_SEGMENT_TYPE = "class-template";
	public static final String NESTED_CLASS_SEGMENT_TYPE = "nested-class-template";

	private final Map<Integer, Collection<? extends TestDescriptor>> childrenPrototypesByIndex = new HashMap<>();
	private final List<TestDescriptor> childrenPrototypes = new ArrayList<>();
	private final ClassBasedTestDescriptor delegate;
	private final DynamicDescendantFilter dynamicDescendantFilter;

	public ClassTemplateTestDescriptor(UniqueId uniqueId, ClassBasedTestDescriptor delegate) {
		this(uniqueId, delegate, new DynamicDescendantFilter());
	}

	private ClassTemplateTestDescriptor(UniqueId uniqueId, ClassBasedTestDescriptor delegate,
			DynamicDescendantFilter dynamicDescendantFilter) {
		super(uniqueId, delegate.getTestClass(), delegate.getDisplayName(), delegate.configuration);
		this.delegate = delegate;
		this.dynamicDescendantFilter = dynamicDescendantFilter;
	}

	// --- TestDescriptor ------------------------------------------------------

	@Override
	public Set<TestTag> getTags() {
		return this.delegate.getTags();
	}

	// --- Validatable ---------------------------------------------------------

	@Override
	protected void validateCoreLifecycleMethods(DiscoveryIssueReporter reporter) {
		this.delegate.validateCoreLifecycleMethods(reporter);
	}

	@Override
	protected void validateClassTemplateInvocationLifecycleMethods(DiscoveryIssueReporter reporter) {
		boolean requireStatic = this.classInfo.lifecycle == PER_METHOD;
		validateClassTemplateInvocationLifecycleMethodsAreDeclaredCorrectly(getTestClass(), requireStatic, reporter);
	}

	// --- Filterable ----------------------------------------------------------

	@Override
	public DynamicDescendantFilter getDynamicDescendantFilter() {
		return this.dynamicDescendantFilter;
	}

	// --- JupiterTestDescriptor -----------------------------------------------

	@Override
	protected JupiterTestDescriptor copyIncludingDescendants(UnaryOperator<UniqueId> uniqueIdTransformer) {
		ClassTemplateTestDescriptor copy = (ClassTemplateTestDescriptor) super.copyIncludingDescendants(
			uniqueIdTransformer);
		this.childrenPrototypes.forEach(oldChild -> {
			TestDescriptor newChild = ((JupiterTestDescriptor) oldChild).copyIncludingDescendants(uniqueIdTransformer);
			copy.childrenPrototypes.add(newChild);
		});
		this.childrenPrototypesByIndex.forEach((index, oldChildren) -> {
			List<? extends TestDescriptor> newChildren = oldChildren.stream() //
					.map(oldChild -> ((JupiterTestDescriptor) oldChild).copyIncludingDescendants(uniqueIdTransformer)) //
					.toList();
			copy.childrenPrototypesByIndex.put(index, newChildren);
		});
		return copy;
	}

	@Override
	protected ClassTemplateTestDescriptor withUniqueId(UnaryOperator<UniqueId> uniqueIdTransformer) {
		return new ClassTemplateTestDescriptor(uniqueIdTransformer.apply(getUniqueId()), this.delegate,
			this.dynamicDescendantFilter.copy(uniqueIdTransformer));
	}

	// --- TestDescriptor ------------------------------------------------------

	@Override
	public void prune() {
		super.prune();
		if (this.children.isEmpty()) {
			return;
		}
		// Create copy to avoid ConcurrentModificationException
		new LinkedHashSet<>(this.children).forEach(child -> child.accept(TestDescriptor::prune));
		// Second iteration to avoid processing children that were pruned in the first iteration
		this.children.forEach(child -> {
			if (child instanceof ClassTemplateInvocationTestDescriptor descriptor) {
				int index = descriptor.getIndex();
				this.dynamicDescendantFilter.allowIndex(index - 1);
				this.childrenPrototypesByIndex.put(index, child.getChildren());
			}
			else {
				this.childrenPrototypes.add(child);
			}
		});
		this.children.clear();
	}

	@Override
	public boolean mayRegisterTests() {
		return !childrenPrototypes.isEmpty() || !childrenPrototypesByIndex.isEmpty();
	}

	// --- TestClassAware ------------------------------------------------------

	@Override
	public List<Class<?>> getEnclosingTestClasses() {
		return delegate.getEnclosingTestClasses();
	}

	// --- ClassBasedTestDescriptor --------------------------------------------

	@Override
	public TestInstances instantiateTestClass(JupiterEngineExecutionContext parentExecutionContext,
			ExtensionContextSupplier extensionContext, ExtensionRegistry registry,
			JupiterEngineExecutionContext context) {
		return delegate.instantiateTestClass(parentExecutionContext, extensionContext, registry, context);
	}

	// --- ResourceLockAware ---------------------------------------------------

	@Override
	public Function<ResourceLocksProvider, Set<ResourceLocksProvider.Lock>> getResourceLocksProviderEvaluator() {
		return delegate.getResourceLocksProviderEvaluator();
	}

	// --- Node ----------------------------------------------------------------

	@Override
	public Set<ExclusiveResource> getExclusiveResources() {
		Set<ExclusiveResource> result = determineExclusiveResources().collect(toCollection(HashSet::new));
		Visitor visitor = testDescriptor -> {
			if (testDescriptor instanceof Node<?> node) {
				result.addAll(node.getExclusiveResources());
			}
		};
		this.childrenPrototypes.forEach(child -> child.accept(visitor));
		this.childrenPrototypesByIndex.values() //
				.forEach(prototypes -> prototypes //
						.forEach(child -> child.accept(visitor)));
		return result;
	}

	@Override
	public void cleanUp(JupiterEngineExecutionContext context) throws Exception {
		this.childrenPrototypes.clear();
		this.childrenPrototypesByIndex.clear();
		this.dynamicDescendantFilter.allowAll();
		super.cleanUp(context);
	}

	@Override
	public JupiterEngineExecutionContext execute(JupiterEngineExecutionContext context,
			DynamicTestExecutor dynamicTestExecutor) throws Exception {

		new ClassTemplateExecutor().execute(context, dynamicTestExecutor);
		return context;
	}

	class ClassTemplateExecutor
			extends TemplateExecutor<ClassTemplateInvocationContextProvider, ClassTemplateInvocationContext> {

		ClassTemplateExecutor() {
			super(ClassTemplateTestDescriptor.this, ClassTemplateInvocationContextProvider.class);
		}

		@Override
		boolean supports(ClassTemplateInvocationContextProvider provider, ExtensionContext extensionContext) {
			return provider.supportsClassTemplate(extensionContext);
		}

		@Override
		protected String getNoRegisteredProviderErrorMessage() {
			return "You must register at least one %s that supports @%s class [%s]".formatted(
				ClassTemplateInvocationContextProvider.class.getSimpleName(), ClassTemplate.class.getSimpleName(),
				getTestClass().getName());
		}

		@Override
		Stream<? extends ClassTemplateInvocationContext> provideContexts(
				ClassTemplateInvocationContextProvider provider, ExtensionContext extensionContext) {
			return provider.provideClassTemplateInvocationContexts(extensionContext);
		}

		@Override
		boolean mayReturnZeroContexts(ClassTemplateInvocationContextProvider provider,
				ExtensionContext extensionContext) {
			return provider.mayReturnZeroClassTemplateInvocationContexts(extensionContext);
		}

		@Override
		protected String getZeroContextsProvidedErrorMessage(ClassTemplateInvocationContextProvider provider) {
			return """
					Provider [%s] did not provide any invocation contexts, but was expected to do so. \
					You may override mayReturnZeroClassTemplateInvocationContexts() to allow this.""".formatted(
				provider.getClass().getSimpleName());
		}

		@Override
		UniqueId createInvocationUniqueId(UniqueId parentUniqueId, int index) {
			return parentUniqueId.append(ClassTemplateInvocationTestDescriptor.SEGMENT_TYPE, "#" + index);
		}

		@Override
		TestDescriptor createInvocationTestDescriptor(UniqueId uniqueId,
				ClassTemplateInvocationContext invocationContext, int index) {
			ClassTemplateInvocationTestDescriptor containerInvocationDescriptor = new ClassTemplateInvocationTestDescriptor(
				uniqueId, ClassTemplateTestDescriptor.this, invocationContext, index, getSource().orElse(null),
				ClassTemplateTestDescriptor.this.configuration);

			collectChildren(index, uniqueId) //
					.forEach(containerInvocationDescriptor::addChild);

			return containerInvocationDescriptor;
		}

		private Stream<? extends TestDescriptor> collectChildren(int index, UniqueId invocationUniqueId) {
			if (ClassTemplateTestDescriptor.this.childrenPrototypesByIndex.containsKey(index)) {
				return ClassTemplateTestDescriptor.this.childrenPrototypesByIndex.remove(index).stream();
			}
			UnaryOperator<UniqueId> transformer = new UniqueIdPrefixTransformer(getUniqueId(), invocationUniqueId);
			return ClassTemplateTestDescriptor.this.childrenPrototypes.stream() //
					.map(JupiterTestDescriptor.class::cast) //
					.map(it -> it.copyIncludingDescendants(transformer));
		}
	}

}
