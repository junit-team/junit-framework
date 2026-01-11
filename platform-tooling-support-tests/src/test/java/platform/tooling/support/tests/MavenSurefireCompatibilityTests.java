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

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static platform.tooling.support.tests.Projects.copyToWorkspace;

import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.platform.tests.process.OutputFiles;
import org.opentest4j.TestAbortedException;

import platform.tooling.support.Helper;
import platform.tooling.support.MavenRepo;
import platform.tooling.support.ProcessStarters;

/**
 * @since 1.9.2
 */
class MavenSurefireCompatibilityTests {

	static final String MINIMUM_SUPPORTED_SUREFIRE_VERSION = "3.0.0";

	@ManagedResource
	LocalMavenRepo localMavenRepo;

	@Test
	void testMavenSurefireCompatibilityProject(@TempDir Path workspace, @FilePrefix("maven") OutputFiles outputFiles)
			throws Exception {
		var result = ProcessStarters.maven(Helper.getJavaHome(17).orElseThrow(TestAbortedException::new)) //
				.workingDir(copyToWorkspace(Projects.MAVEN_SUREFIRE_COMPATIBILITY, workspace)) //
				.addArguments(localMavenRepo.toCliArgument(), "-Dmaven.repo=" + MavenRepo.dir()) //
				.addArguments("-Dsurefire.version=" + MINIMUM_SUPPORTED_SUREFIRE_VERSION) //
				.addArguments("--update-snapshots", "--batch-mode", "test") //
				.redirectOutput(outputFiles) //
				.startAndWait();

		assertEquals(0, result.exitCode());
		assertEquals("", result.stdErr());

		var output = result.stdOutLines();
		assertTrue(output.contains("[INFO] BUILD SUCCESS"));
		assertTrue(output.contains("[INFO] Tests run: 1, Failures: 0, Errors: 0, Skipped: 0"));

		var targetDir = workspace.resolve("target");
		try (var stream = Files.list(targetDir)) {
			assertThat(stream.filter(file -> file.getFileName().toString().startsWith("junit-platform-unique-ids"))) //
					.hasSize(1);
		}
	}
}
