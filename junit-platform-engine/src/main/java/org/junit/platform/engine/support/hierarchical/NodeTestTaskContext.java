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

import org.junit.platform.engine.CancellationToken;
import org.junit.platform.engine.EngineExecutionListener;

/**
 * @since 1.3.1
 */
record NodeTestTaskContext(EngineExecutionListener listener, HierarchicalTestExecutorService executorService,
		ThrowableCollector.Factory throwableCollectorFactory, NodeExecutionAdvisor executionAdvisor,
		CancellationToken cancellationToken) {

	NodeTestTaskContext withListener(EngineExecutionListener listener) {
		if (this.listener == listener) {
			return this;
		}
		return new NodeTestTaskContext(listener, executorService, throwableCollectorFactory, executionAdvisor,
			cancellationToken);
	}

}
