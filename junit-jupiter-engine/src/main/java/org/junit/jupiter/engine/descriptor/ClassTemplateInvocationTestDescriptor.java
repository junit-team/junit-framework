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
import static org.junit.jupiter.engine.descriptor.CallbackSupport.invokeAfterCallbacks;
import static org.junit.jupiter.engine.descriptor.CallbackSupport.invokeBeforeCallbacks;
import static org.junit.jupiter.engine.extension.MutableExtensionRegistry.createRegistryFrom;
import static org.junit.jupiter.engine.support.JupiterThrowableCollectorFactory.createThrowableCollector;

import java.util.List;
import java.util.Set;
import java.util.function.Function;
import java.util.function.UnaryOperator;
import java.util.stream.Stream;

import org.apiguardian.api.API;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.extension.AfterClassTemplateInvocationCallback;
import org.junit.jupiter.api.extension.BeforeClassTemplateInvocationCallback;
import org.junit.jupiter.api.extension.ClassTemplateInvocationContext;
import org.junit.jupiter.api.extension.Extension;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.parallel.ResourceLocksProvider;
import org.junit.jupiter.engine.config.JupiterConfiguration;
import org.junit.jupiter.engine.execution.JupiterEngineExecutionContext;
import org.junit.jupiter.engine.extension.MutableExtensionRegistry;
import org.junit.platform.engine.TestSource;
import org.junit.platform.engine.UniqueId;
import org.junit.platform.engine.support.hierarchical.ExclusiveResource;
import org.junit.platform.engine.support.hierarchical.ThrowableCollector;

/**
 * @since 5.13
 */
@API(status = INTERNAL, since = "5.13")
public class ClassTemplateInvocationTestDescriptor extends JupiterTestDescriptor
		implements TestClassAware, ResourceLockAware {

	public static final String SEGMENT_TYPE = "class-template-invocation";

	private final ClassTemplateTestDescriptor parent;

	private @Nullable ClassTemplateInvocationContext invocationContext;

	private final int index;

	public ClassTemplateInvocationTestDescriptor(UniqueId uniqueId, ClassTemplateTestDescriptor parent,
			ClassTemplateInvocationContext invocationContext, int index, @Nullable TestSource source,
			JupiterConfiguration configuration) {
		super(uniqueId, invocationContext.getDisplayName(index), source, configuration);
		this.parent = parent;
		this.invocationContext = invocationContext;
		this.index = index;
	}

	public int getIndex() {
		return index;
	}

	// --- JupiterTestDescriptor -----------------------------------------------

	@Override
	protected ClassTemplateInvocationTestDescriptor withUniqueId(UnaryOperator<UniqueId> uniqueIdTransformer) {
		return new ClassTemplateInvocationTestDescriptor(uniqueIdTransformer.apply(getUniqueId()), parent,
			requiredInvocationContext(), this.index, getSource().orElse(null), this.configuration);
	}

	// --- TestDescriptor ------------------------------------------------------

	@Override
	public Type getType() {
		return Type.CONTAINER;
	}

	@Override
	public String getLegacyReportingName() {
		return getTestClass().getName() + "[" + index + "]";
	}

	// --- TestClassAware ------------------------------------------------------

	@Override
	public Class<?> getTestClass() {
		return parent.getTestClass();
	}

	@Override
	public List<Class<?>> getEnclosingTestClasses() {
		return parent.getEnclosingTestClasses();
	}

	// --- ResourceLockAware ---------------------------------------------------

	@Override
	public ExclusiveResourceCollector getExclusiveResourceCollector() {
		return parent.getExclusiveResourceCollector();
	}

	@Override
	public Function<ResourceLocksProvider, Set<ResourceLocksProvider.Lock>> getResourceLocksProviderEvaluator() {
		return parent.getResourceLocksProviderEvaluator();
	}

	// --- Node ----------------------------------------------------------------

	@Override
	public Set<ExclusiveResource> getExclusiveResources() {
		// Resources are already collected and returned by the enclosing ClassTemplateTestDescriptor
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
		ExtensionContext extensionContext = new ClassTemplateInvocationExtensionContext(context.getExtensionContext(),
			context.getExecutionListener(), this, context.getConfiguration(), registry,
			context.getLauncherStoreFacade());
		ThrowableCollector throwableCollector = createThrowableCollector();
		throwableCollector.execute(() -> requiredInvocationContext().prepareInvocation(extensionContext));
		return context.extend() //
				.withExtensionRegistry(registry) //
				.withExtensionContext(extensionContext) //
				.withThrowableCollector(throwableCollector) //
				.build();
	}

	@Override
	public SkipResult shouldBeSkipped(JupiterEngineExecutionContext context) {
		context.getThrowableCollector().assertEmpty();
		return SkipResult.doNotSkip();
	}

	@Override
	public JupiterEngineExecutionContext before(JupiterEngineExecutionContext context) throws Exception {
		invokeBeforeCallbacks(BeforeClassTemplateInvocationCallback.class, context,
			BeforeClassTemplateInvocationCallback::beforeClassTemplateInvocation);
		context.getThrowableCollector().assertEmpty();
		return context;
	}

	@Override
	public JupiterEngineExecutionContext execute(JupiterEngineExecutionContext context,
			DynamicTestExecutor dynamicTestExecutor) throws Exception {
		Visitor visitor = context.getExecutionListener()::dynamicTestRegistered;
		getChildren().forEach(child -> child.accept(visitor));
		return context;
	}

	@Override
	public void after(JupiterEngineExecutionContext context) throws Exception {

		ThrowableCollector throwableCollector = context.getThrowableCollector();
		Throwable previousThrowable = throwableCollector.getThrowable();

		invokeAfterCallbacks(AfterClassTemplateInvocationCallback.class, context,
			AfterClassTemplateInvocationCallback::afterClassTemplateInvocation);

		// If the previous Throwable was not null when this method was called,
		// that means an exception was already thrown either before or during
		// the execution of this Node. If an exception was already thrown, any
		// later exceptions were added as suppressed exceptions to that original
		// exception unless a more severe exception occurred in the meantime.
		if (previousThrowable != throwableCollector.getThrowable()) {
			throwableCollector.assertEmpty();
		}
	}

	@Override
	public void cleanUp(JupiterEngineExecutionContext context) throws Exception {
		// forget invocationContext so it can be garbage collected
		this.invocationContext = null;
		super.cleanUp(context);
	}

	private ClassTemplateInvocationContext requiredInvocationContext() {
		return requireNonNull(this.invocationContext);
	}
}
