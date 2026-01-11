/*
 * Copyright 2015-2026 the original author or authors.
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v2.0 which
 * accompanies this distribution and is available at
 *
 * https://www.eclipse.org/legal/epl-v20.html
 */

package org.junit.platform.console.command;

import static org.apiguardian.api.API.Status.INTERNAL;

import java.util.Optional;

import org.apiguardian.api.API;
import org.jspecify.annotations.Nullable;

/**
 * @since 1.10
 */
@API(status = INTERNAL, since = "1.10")
public class CommandResult<T> {
	public static <T> CommandResult<T> success() {
		return create(ExitCode.SUCCESS, null);
	}

	public static <T> CommandResult<T> create(int exitCode, @Nullable T value) {
		return new CommandResult<>(exitCode, value);
	}

	private final int exitCode;

	private final @Nullable T value;

	private CommandResult(int exitCode, @Nullable T value) {
		this.exitCode = exitCode;
		this.value = value;
	}

	public int getExitCode() {
		return this.exitCode;
	}

	public Optional<T> getValue() {
		return Optional.ofNullable(this.value);
	}

}
