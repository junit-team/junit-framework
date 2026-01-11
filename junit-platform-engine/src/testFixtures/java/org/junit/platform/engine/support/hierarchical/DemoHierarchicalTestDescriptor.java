/*
 * Copyright 2015-2026 the original author or authors.
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v2.0 which
 * accompanies this distribution and is available at
 *
 * https://www.eclipse.org/legal/epl-v20.html
 */

package org.junit.platform.engine.support.hierarchical;

import static org.junit.platform.engine.support.hierarchical.Node.SkipResult.doNotSkip;
import static org.junit.platform.engine.support.hierarchical.Node.SkipResult.skip;

import java.util.function.BiConsumer;

import org.jspecify.annotations.Nullable;
import org.junit.platform.engine.TestDescriptor;
import org.junit.platform.engine.TestSource;
import org.junit.platform.engine.UniqueId;
import org.junit.platform.engine.support.descriptor.AbstractTestDescriptor;

/**
 * @since 1.0
 */
public class DemoHierarchicalTestDescriptor extends AbstractTestDescriptor implements Node<DemoEngineExecutionContext> {

	private final @Nullable BiConsumer<DemoEngineExecutionContext, TestDescriptor> executeBlock;

	private @Nullable String skippedReason;

	private boolean skipped;

	public DemoHierarchicalTestDescriptor(UniqueId uniqueId, String displayName,
			@Nullable BiConsumer<DemoEngineExecutionContext, TestDescriptor> executeBlock) {
		this(uniqueId, displayName, null, executeBlock);
	}

	public DemoHierarchicalTestDescriptor(UniqueId uniqueId, String displayName, @Nullable TestSource source,
			@Nullable BiConsumer<DemoEngineExecutionContext, TestDescriptor> executeBlock) {
		super(uniqueId, displayName, source);
		this.executeBlock = executeBlock;
	}

	@Override
	public Type getType() {
		return this.executeBlock != null ? Type.TEST : Type.CONTAINER;
	}

	public void markSkipped(String reason) {
		this.skipped = true;
		this.skippedReason = reason;
	}

	@Override
	public SkipResult shouldBeSkipped(DemoEngineExecutionContext context) {
		return skipped ? skip(skippedReason) : doNotSkip();
	}

	@Override
	public DemoEngineExecutionContext execute(DemoEngineExecutionContext context,
			DynamicTestExecutor dynamicTestExecutor) {
		if (this.executeBlock != null) {
			this.executeBlock.accept(context, this);
		}
		return context;
	}

}
