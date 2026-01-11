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

import java.io.IOException;
import java.io.PrintStream;
import java.nio.file.Files;
import java.nio.file.Path;

import org.jspecify.annotations.Nullable;
import org.junit.platform.commons.JUnitException;

class StandardStreamsHandler implements AutoCloseable {

	private @Nullable PrintStream stdout;

	private @Nullable PrintStream stderr;

	StandardStreamsHandler() {
	}

	/**
	 * Redirect standard output (stdout) and standard error (stderr) to the specified
	 * file paths.
	 *
	 * <p>If the paths are the same, both streams are redirected to the same file.
	 *
	 * <p>The default charset is used for writing to the files.
	 *
	 * @param stdoutPath the file path for standard output, or {@code null} to
	 * indicate no redirection
	 * @param stderrPath the file path for standard error, or {@code null} to
	 * indicate no redirection
	 */
	public void redirectStandardStreams(@Nullable Path stdoutPath, @Nullable Path stderrPath) {
		if (isSameFile(stdoutPath, stderrPath)) {
			try {
				PrintStream commonStream = new PrintStream(Files.newOutputStream(stdoutPath), true);
				this.stdout = commonStream;
				this.stderr = commonStream;
			}
			catch (IOException ex) {
				throw new JUnitException("Error redirecting stdout and stderr to file: " + stdoutPath, ex);
			}
		}
		else {
			if (stdoutPath != null) {
				try {
					this.stdout = new PrintStream(Files.newOutputStream(stdoutPath), true);
				}
				catch (IOException ex) {
					throw new JUnitException("Error redirecting stdout to file: " + stdoutPath, ex);
				}
			}

			if (stderrPath != null) {
				try {
					this.stderr = new PrintStream(Files.newOutputStream(stderrPath), true);
				}
				catch (IOException ex) {
					throw new JUnitException("Error redirecting stderr to file: " + stderrPath, ex);
				}
			}
		}

		if (this.stdout != null) {
			System.setOut(this.stdout);
		}
		if (this.stderr != null) {
			System.setErr(this.stderr);
		}
	}

	@Override
	public void close() {
		try {
			if (this.stdout != null) {
				this.stdout.close();
			}
		}
		finally {
			if (this.stderr != null) {
				this.stderr.close();
			}
		}
	}

	private static boolean isSameFile(@Nullable Path path1, @Nullable Path path2) {
		if (path1 == null || path2 == null) {
			return false;
		}
		return path1.normalize().toAbsolutePath().equals(path2.normalize().toAbsolutePath());
	}

}
