/*
 * Copyright 2015-2025 the original author or authors.
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v2.0 which
 * accompanies this distribution and is available at
 *
 * https://www.eclipse.org/legal/epl-v20.html
 */

package platform.tooling.support.tests;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static platform.tooling.support.tests.Projects.copyToWorkspace;

import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledOnJre;
import org.junit.jupiter.api.condition.JRE;
import org.junit.jupiter.api.io.TempDir;
import org.junit.platform.tests.process.OutputFiles;

import platform.tooling.support.Helper;
import platform.tooling.support.MavenRepo;
import platform.tooling.support.ProcessStarters;
import platform.tooling.support.ThirdPartyJars;

/**
 * @since 6.1
 */
class JUnitStartTests {

	@TempDir
	static Path workspace;

	@BeforeAll
	static void prepareLocalLibraryDirectoryWithJUnitModules() throws Exception {
		copyToWorkspace(Projects.JUNIT_START, workspace);
		var lib = workspace.resolve("lib");
		try {
			Files.createDirectories(lib);
			try (var directoryStream = Files.newDirectoryStream(lib, "*.jar")) {
				for (Path jarFile : directoryStream) {
					Files.delete(jarFile);
				}
			}
			for (var module : Helper.loadModuleDirectoryNames()) {
				if (module.startsWith("junit-platform") || module.startsWith("junit-jupiter")
						|| module.equals("junit-start")) {
					if (module.equals("junit-jupiter-migrationsupport"))
						continue;
					if (module.startsWith("junit-platform-suite"))
						continue;
					if (module.equals("junit-platform-testkit"))
						continue;
					var jar = MavenRepo.jar(module);
					Files.copy(jar, lib.resolve(module + ".jar"));
				}
			}
			ThirdPartyJars.copy(lib, "org.apiguardian", "apiguardian-api");
			ThirdPartyJars.copy(lib, "org.jspecify", "jspecify");
			ThirdPartyJars.copy(lib, "org.opentest4j", "opentest4j");
			ThirdPartyJars.copy(lib, "org.opentest4j.reporting", "open-test-reporting-tooling-spi");
		}
		catch (Exception e) {
			throw new AssertionError("Preparing local library folder failed", e);
		}
	}

	@Test
	@EnabledOnJre(JRE.JAVA_25)
	void junitRun(@FilePrefix("junit-run") OutputFiles outputFiles) throws Exception {
		var result = ProcessStarters.java() //
				.workingDir(workspace) //
				.addArguments("--module-path", "lib") // relative to workspace
				.addArguments("--add-modules", "org.junit.start") // configure root module
				.addArguments("compact/JUnitRun.java") // leverage Java's source mode
				.redirectOutput(outputFiles) //
				.startAndWait();

		assertEquals(0, result.exitCode());
		assertTrue(result.stdOut().contains("addition()"), result.stdOut());
	}

	@Test
	@EnabledOnJre(JRE.JAVA_25)
	void junitRunClass(@FilePrefix("junit-run-class") OutputFiles outputFiles) throws Exception {
		var result = ProcessStarters.java() //
				.workingDir(workspace) //
				.addArguments("--module-path", "lib") // relative to workspace
				.addArguments("--add-modules", "org.junit.start") // configure root module
				.addArguments("compact/JUnitRunClass.java") // leverage Java's source mode
				.redirectOutput(outputFiles) //
				.startAndWait();

		assertEquals(0, result.exitCode());
		assertTrue(result.stdOut().contains("substraction()"), result.stdOut());
	}

	@Test
	@EnabledOnJre(JRE.JAVA_25)
	void junitRunModule(@FilePrefix("junit-run-module") OutputFiles outputFiles) throws Exception {
		var result = ProcessStarters.java() //
				.workingDir(workspace) //
				.putEnvironment("NO_COLOR", "1") // --disable-ansi-colors
				.addArguments("--module-path", "lib") // relative to workspace
				.addArguments("modular/p/JUnitRunModule.java") // leverage Java's source mode
				.redirectOutput(outputFiles) //
				.startAndWait();

		assertEquals(0, result.exitCode());
		assertTrue(result.stdOut().contains("multiplication()"), result.stdOut());
	}

}
