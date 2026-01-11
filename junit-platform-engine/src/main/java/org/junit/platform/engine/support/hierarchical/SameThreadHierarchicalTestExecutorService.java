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

import static java.util.concurrent.CompletableFuture.completedFuture;
import static org.apiguardian.api.API.Status.STABLE;

import java.util.List;
import java.util.concurrent.Future;

import org.apiguardian.api.API;
import org.jspecify.annotations.Nullable;

/**
 * A simple {@linkplain HierarchicalTestExecutorService executor service} that
 * executes all {@linkplain TestTask test tasks} in the caller's thread.
 *
 * @since 1.3
 */
@API(status = STABLE, since = "1.10")
public class SameThreadHierarchicalTestExecutorService implements HierarchicalTestExecutorService {

	public SameThreadHierarchicalTestExecutorService() {
	}

	@Override
	public Future<@Nullable Void> submit(TestTask testTask) {
		testTask.execute();
		return completedFuture(null);
	}

	@Override
	public void invokeAll(List<? extends TestTask> tasks) {
		tasks.forEach(TestTask::execute);
	}

	@Override
	public void close() {
		// nothing to do
	}

}
