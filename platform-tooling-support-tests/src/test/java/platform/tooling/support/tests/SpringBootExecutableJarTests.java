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
import static platform.tooling.support.ProcessStarters.java;
import static platform.tooling.support.ProcessStarters.maven;
import static platform.tooling.support.Projects.copyToWorkspace;

import java.nio.file.Path;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.platform.tests.process.OutputFiles;
import org.opentest4j.TestAbortedException;

import platform.tooling.support.FilePrefix;
import platform.tooling.support.Helper;
import platform.tooling.support.MavenRepo;
import platform.tooling.support.Projects;

/**
 * @since 6.2
 */
class SpringBootExecutableJarTests {

	@ManagedResource
	LocalMavenRepo localMavenRepo;

	@Test
	void scansClasspathInsideExecutableJar(@TempDir Path workspace, @FilePrefix("maven") OutputFiles mavenOutput,
			@FilePrefix("java") OutputFiles javaOutput) throws Exception {
		var javaHome = Helper.getJavaHome(17).orElseThrow(TestAbortedException::new);

		var mavenResult = maven(javaHome) //
				.workingDir(copyToWorkspace(Projects.SPRING_BOOT_EXECUTABLE_JAR, workspace)) //
				.addArguments(localMavenRepo.toCliArgument(), "-Dmaven.repo=" + MavenRepo.dir()) //
				.addArguments("--update-snapshots", "--batch-mode", "package") //
				.redirectOutput(mavenOutput) //
				.startAndWait();

		assertEquals(0, mavenResult.exitCode());
		assertThat(mavenResult.stdOutLines()).contains("[INFO] BUILD SUCCESS");

		var javaResult = java(javaHome) //
				.workingDir(workspace.resolve("app/target")) //
				.addArguments("-jar", "app-1.0-SNAPSHOT.jar") //
				// Selecting a package forces classpath scanning via `jar:nested:` URIs of both
				// BOOT-INF/classes (ExampleTests) and BOOT-INF/lib/tests-1.0-SNAPSHOT.jar (LibraryTests)
				.addArguments("execute", "--select-package=com.example", "--details=tree", "--details-theme=ascii",
					"--disable-banner", "--disable-ansi-colors", "--fail-if-no-tests") //
				.redirectOutput(javaOutput) //
				.startAndWait();

		assertEquals(0, javaResult.exitCode());
		assertThat(javaResult.stdOutLines()).containsSequence("""
				'-- JUnit Jupiter [OK]
				  +-- ExampleTests [OK]
				  | '-- test() [OK]
				  '-- LibraryTests [OK]
				    '-- test() [OK]
				""".lines().toList());
	}
}
