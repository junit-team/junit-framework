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

import java.io.PrintWriter;
import java.util.Optional;

import org.apiguardian.api.API;

/**
 * Internal facade to run a CLI command that exists to hide implementation
 * details such as the used library.
 *
 * @since 1.10
 */
@API(status = INTERNAL, since = "1.10")
public class CommandFacade {

	private final ConsoleTestExecutor.Factory consoleTestExecutorFactory;

	public CommandFacade(ConsoleTestExecutor.Factory consoleTestExecutorFactory) {
		this.consoleTestExecutorFactory = consoleTestExecutorFactory;
	}

	public CommandResult<?> run(String[] args) {
		return run(args, Optional.empty());
	}

	public CommandResult<?> run(String[] args, PrintWriter out, PrintWriter err) {
		try {
			return run(args, Optional.of(new OutputStreamConfig(out, err)));
		}
		finally {
			out.flush();
			err.flush();
		}
	}

	private CommandResult<?> run(String[] args, Optional<OutputStreamConfig> outputStreamConfig) {
		Optional<String> version = ManifestVersionProvider.getImplementationVersion();
		System.setProperty("junit.docs.version",
			version.map(it -> it.endsWith("-SNAPSHOT") ? "snapshot" : it).orElse("current"));
		return new MainCommand(consoleTestExecutorFactory).run(args, outputStreamConfig);
	}

}
