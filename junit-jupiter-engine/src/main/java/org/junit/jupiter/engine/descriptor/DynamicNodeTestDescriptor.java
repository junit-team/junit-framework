/*
 * Copyright 2015-2025 the original author or authors.
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v2.0 which
 * accompanies this distribution and is available at
 *
 * https://www.eclipse.org/legal/epl-v20.html
 */

package org.junit.jupiter.engine.descriptor;

import java.util.Optional;
import java.util.function.Function;

import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.DynamicNode;
import org.junit.jupiter.api.extension.ConditionEvaluationResult;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.engine.config.JupiterConfiguration;
import org.junit.jupiter.engine.execution.JupiterEngineExecutionContext;
import org.junit.platform.engine.TestDescriptor;
import org.junit.platform.engine.TestSource;
import org.junit.platform.engine.UniqueId;

/**
 * Base {@link TestDescriptor} for a {@link DynamicNode}.
 *
 * @since 5.0.3
 */
abstract class DynamicNodeTestDescriptor extends JupiterTestDescriptor {

	protected final int index;
	private final Optional<ExecutionMode> executionMode;
	private final Optional<Function<? super ExtensionContext, ? extends ConditionEvaluationResult>> executionCondition;

	DynamicNodeTestDescriptor(UniqueId uniqueId, int index, DynamicNode dynamicNode, @Nullable TestSource testSource,
			JupiterConfiguration configuration) {
		super(uniqueId, dynamicNode.getDisplayName(), testSource, configuration);
		this.index = index;
		this.executionMode = dynamicNode.getExecutionMode().map(JupiterTestDescriptor::toExecutionMode);
		this.executionCondition = dynamicNode.getExecutionCondition();
	}

	@Override
	Optional<ExecutionMode> getExplicitExecutionMode() {
		return executionMode;
	}

	@Override
	public String getLegacyReportingName() {
		// @formatter:off
		return getParent()
				.map(TestDescriptor::getLegacyReportingName)
				.orElseGet(this::getDisplayName)
						+ "[" + index + "]";
		// @formatter:on
	}

	@Override
	public JupiterEngineExecutionContext prepare(JupiterEngineExecutionContext context) {
		ExtensionContext extensionContext = new DynamicExtensionContext(context.getExtensionContext(),
			context.getExecutionListener(), this, context.getConfiguration(), context.getExtensionRegistry(),
			context.getLauncherStoreFacade());
		// @formatter:off
		return context.extend()
				.withExtensionContext(extensionContext)
				.build();
		// @formatter:on
	}

	@Override
	public SkipResult shouldBeSkipped(JupiterEngineExecutionContext context) {
		return this.executionCondition //
				.map(condition -> condition.apply(context.getExtensionContext())) //
				.map(this::toSkipResult) //
				.orElse(SkipResult.doNotSkip());
	}

}
