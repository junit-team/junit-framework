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
import java.util.Comparator;
import java.util.StringJoiner;
import java.util.stream.StreamSupport;

import org.junit.platform.engine.TestEngine;
import org.junit.platform.launcher.core.ServiceLoaderTestEngineRegistry;

import picocli.CommandLine.Command;

@Command(//
		name = "engines", //
		description = "List available test engines" //
)
class ListTestEnginesCommand extends BaseCommand<Void> {

	@Override
	protected Void execute(PrintWriter out) {
		displayEngines(out);
		return null;
	}

	void displayEngines(PrintWriter out) {
		ServiceLoaderTestEngineRegistry registry = new ServiceLoaderTestEngineRegistry();
		Iterable<TestEngine> engines = registry.loadTestEngines();
		StreamSupport.stream(engines.spliterator(), false) //
				.sorted(Comparator.comparing(TestEngine::getId)) //
				.forEach(engine -> displayEngine(out, engine));
		out.flush();
	}

	private void displayEngine(PrintWriter out, TestEngine engine) {
		StringJoiner details = new StringJoiner(":", " (", ")");
		engine.getGroupId().ifPresent(details::add);
		engine.getArtifactId().ifPresent(details::add);
		engine.getVersion().ifPresent(details::add);
		out.println(getColorScheme().string("@|bold %s|@%s".formatted(engine.getId(), details)));
	}

}
