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

import org.jspecify.annotations.Nullable;
import org.junit.platform.engine.TestSource;
import org.junit.platform.engine.UniqueId;
import org.junit.platform.engine.support.descriptor.AbstractTestDescriptor;

/**
 * @since 1.0
 */
public class DemoHierarchicalContainerDescriptor extends AbstractTestDescriptor
		implements Node<DemoEngineExecutionContext> {

	private final @Nullable Runnable beforeBlock;

	public DemoHierarchicalContainerDescriptor(UniqueId uniqueId, String displayName, @Nullable TestSource source,
			@Nullable Runnable beforeBlock) {
		super(uniqueId, displayName, source);
		this.beforeBlock = beforeBlock;
	}

	@Override
	public Type getType() {
		return Type.CONTAINER;
	}

	@Override
	public boolean mayRegisterTests() {
		return true;
	}

	@Override
	public SkipResult shouldBeSkipped(DemoEngineExecutionContext context) {
		return doNotSkip();
	}

	@Override
	public DemoEngineExecutionContext before(DemoEngineExecutionContext context) {
		if (this.beforeBlock != null) {
			this.beforeBlock.run();
		}
		return context;
	}

}
