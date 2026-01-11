/*
 * Copyright 2015-2026 the original author or authors.
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v2.0 which
 * accompanies this distribution and is available at
 *
 * https://www.eclipse.org/legal/epl-v20.html
 */

package platform.tooling.support.tests;

import static java.util.Objects.requireNonNull;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.platform.tests.process.OutputFiles;

import platform.tooling.support.ProcessStarters;
import platform.tooling.support.ThirdPartyJars;

class ModularCompilationTests {

	@Test
	void compileAllJUnitModules(@TempDir Path workspace, @FilePrefix("javac") OutputFiles javacOutputFiles)
			throws Exception {
		var lib = Files.createDirectories(workspace.resolve("lib"));
		ThirdPartyJars.copyAll(lib);

		var moduleNames = Arrays.asList(System.getProperty("junit.modules").split(","));

		var outputDir = workspace.resolve("classes").toAbsolutePath();
		var processStarter = ProcessStarters.javaCommand("javac") //
				.workingDir(workspace) //
				.addArguments("-d", outputDir.toString()) //
				.addArguments("-Xlint:all", "-Werror") //
				.addArguments("-Xlint:-requires-automatic,-requires-transitive-automatic") // JUnit 4
				// external modules
				.addArguments("--module-path", lib.toAbsolutePath().toString());

		// source locations in module-specific form
		moduleNames.forEach(
			moduleName -> processStarter.addArguments("--module-source-path", moduleSourcePath(moduleName)));

		var result = processStarter
				// un-shadow
				.addArguments("--add-modules", "info.picocli") //
				.addArguments("--add-reads", "org.junit.platform.console=info.picocli") //
				.addArguments("--add-modules", "org.opentest4j.reporting.events") //
				.addArguments("--add-reads", "org.junit.platform.reporting=org.opentest4j.reporting.events") //
				.addArguments("--add-modules", "de.siegmar.fastcsv") //
				.addArguments("--add-reads", "org.junit.jupiter.params=de.siegmar.fastcsv")
				// modules to compile
				.addArguments("--module", String.join(",", moduleNames)) //
				.redirectOutput(javacOutputFiles) //
				.startAndWait();

		assertEquals(0, result.exitCode());
		assertThat(outputDir).isNotEmptyDirectory();
	}

	static String moduleSourcePath(String moduleName) {
		return "%s=%s".formatted(moduleName,
			requireNonNull(System.getProperty("junit.moduleSourcePath." + moduleName)));
	}
}
