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

import java.util.concurrent.Future;

import org.jspecify.annotations.Nullable;
import org.junit.platform.engine.CancellationToken;
import org.junit.platform.engine.EngineExecutionListener;
import org.junit.platform.engine.ExecutionRequest;
import org.junit.platform.engine.TestDescriptor;
import org.junit.platform.engine.TestEngine;

/**
 * Implementation core of all {@link TestEngine TestEngines} that wish to
 * use the {@link Node} abstraction as the driving principle for structuring
 * and executing test suites.
 *
 * <p>A {@code HierarchicalTestExecutor} is instantiated by a concrete
 * implementation of {@link HierarchicalTestEngine} and takes care of
 * executing nodes in the hierarchy in the appropriate order as well as
 * firing the necessary events in the {@link EngineExecutionListener}.
 *
 * @param <C> the type of {@code EngineExecutionContext} used by the
 * {@code HierarchicalTestEngine}
 * @since 1.0
 */
class HierarchicalTestExecutor<C extends EngineExecutionContext> {

	private final ExecutionRequest request;
	private final C rootContext;
	private final HierarchicalTestExecutorService executorService;
	private final ThrowableCollector.Factory throwableCollectorFactory;

	HierarchicalTestExecutor(ExecutionRequest request, C rootContext, HierarchicalTestExecutorService executorService,
			ThrowableCollector.Factory throwableCollectorFactory) {
		this.request = request;
		this.rootContext = rootContext;
		this.executorService = executorService;
		this.throwableCollectorFactory = throwableCollectorFactory;
	}

	Future<@Nullable Void> execute() {
		return this.executorService.submit(createRootTestTask());
	}

	private NodeTestTask<C> createRootTestTask() {
		NodeTestTaskContext taskContext = createTaskContext();
		TestDescriptor rootTestDescriptor = this.request.getRootTestDescriptor();
		NodeTestTask<C> rootTestTask = new NodeTestTask<>(taskContext, rootTestDescriptor);
		rootTestTask.setParentContext(this.rootContext);
		return rootTestTask;
	}

	private NodeTestTaskContext createTaskContext() {
		EngineExecutionListener executionListener = this.request.getEngineExecutionListener();
		NodeExecutionAdvisor executionAdvisor = new NodeTreeWalker().walk(this.request.getRootTestDescriptor());
		CancellationToken cancellationToken = this.request.getCancellationToken();
		return new NodeTestTaskContext(executionListener, this.executorService, this.throwableCollectorFactory,
			executionAdvisor, cancellationToken);
	}

}
