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

import java.io.PrintWriter;

import picocli.CommandLine;

class OutputStreamConfig {

	private final PrintWriter out;
	private final PrintWriter err;

	OutputStreamConfig(PrintWriter out, PrintWriter err) {
		this.out = out;
		this.err = err;
	}

	void applyTo(CommandLine commandLine) {
		commandLine.setOut(this.out).setErr(this.err);
	}

}
