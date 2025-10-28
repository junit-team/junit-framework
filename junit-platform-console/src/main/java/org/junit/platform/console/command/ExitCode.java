/*
 * Copyright 2015-2025 the original author or authors.
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v2.0 which
 * accompanies this distribution and is available at
 *
 * https://www.eclipse.org/legal/epl-v20.html
 */

package org.junit.platform.console.command;

/**
 * Well-known exit codes of the {@code junit} tool.
 *
 * @since 6.0.1
 */
final class ExitCode {
	/**
	 * Exit code indicating a successful tool run.
	 */
	public static final int SUCCESS = 0;

	/**
	 * Exit code indicating an unsuccessful run.
	 */
	public static final int ANY_ERROR = -1;

	/**
	 * Exit code indicating test failure(s).
	 */
	public static final int TEST_FAILED = 1;

	/**
	 * Exit code indicating no tests found.
	 */
	public static final int NO_TESTS_FOUND = 2;

	/**
	 * Exit code indicating invalid user input.
	 */
	public static final int INVALID_INPUT = 3;

	private ExitCode() {
	}
}
