/*
 * Copyright 2015-2026 the original author or authors.
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v2.0 which
 * accompanies this distribution and is available at
 *
 * https://www.eclipse.org/legal/epl-v20.html
 */

package org.junit.jupiter.api.parallel;

import static org.apiguardian.api.API.Status.STABLE;

import org.apiguardian.api.API;

/**
 * Supported execution modes for parallel test execution.
 *
 * @since 5.3
 * @see #SAME_THREAD
 * @see #CONCURRENT
 */
@API(status = STABLE, since = "5.10")
public enum ExecutionMode {

	/**
	 * Force execution in same thread as the parent node.
	 *
	 * @see #CONCURRENT
	 */
	SAME_THREAD,

	/**
	 * Allow concurrent execution with any other node.
	 *
	 * @see #SAME_THREAD
	 */
	CONCURRENT

}
