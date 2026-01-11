/*
 * Copyright 2015-2026 the original author or authors.
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v2.0 which
 * accompanies this distribution and is available at
 *
 * https://www.eclipse.org/legal/epl-v20.html
 */

package org.junit.jupiter.api.io;

import static org.apiguardian.api.API.Status.STABLE;

import org.apiguardian.api.API;

/**
 * Enumeration of cleanup modes for {@link TempDir @TempDir}.
 *
 * <p>When a test with a temporary directory completes, it might be useful in
 * some cases to be able to view the contents of the temporary directory used by
 * the test. {@code CleanupMode} allows you to control how a {@code TempDir}
 * is cleaned up.
 *
 * @since 5.9
 * @see TempDir
 */
@API(status = STABLE, since = "5.11")
public enum CleanupMode {

	/**
	 * Use the default cleanup mode.
	 *
	 * @see TempDir#DEFAULT_CLEANUP_MODE_PROPERTY_NAME
	 */
	DEFAULT,

	/**
	 * Always clean up a temporary directory after the test has completed.
	 */
	ALWAYS,

	/**
	 * Only clean up a temporary directory if the test completed successfully.
	 */
	ON_SUCCESS,

	/**
	 * Never clean up a temporary directory after the test has completed.
	 */
	NEVER

}
