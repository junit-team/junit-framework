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
import static org.junit.jupiter.api.Assertions.assertTimeoutPreemptively;
import static org.junit.platform.launcher.LauncherConstants.MEMORY_CLEANUP_ENABLED_PROPERTY_NAME;
import static platform.tooling.support.tests.Projects.copyToWorkspace;

import java.nio.file.Path;
import java.time.Duration;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.platform.tests.process.OutputFiles;
import org.junit.platform.tests.process.ProcessResult;

import platform.tooling.support.MavenRepo;
import platform.tooling.support.ProcessStarters;

/**
 * @since 6.1
 */
class MemoryCleanupTests {

	@TempDir
	Path workspace;

	@Test
	void runsWithSmallHeapSize(@FilePrefix("javac") OutputFiles javacOutputFiles,
			@FilePrefix("java") OutputFiles javaOutputFiles) throws Exception {

		copyToWorkspace(Projects.MEMORY_CLEANUP, workspace);
		compile(javacOutputFiles);

		var result = assertTimeoutPreemptively(Duration.ofSeconds(10), () -> executeWithSmallHeapSize(javaOutputFiles));

		assertThat(result).isNotNull();
		assertThat(result.exitCode()).isOne();
		assertThat(result.stdOut()) //
				.contains("1000000 tests found") //
				.contains(" 999999 tests successful") //
				.contains("      1 tests failed");
	}

	void compile(OutputFiles javacOutputFiles) throws Exception {
		var result = ProcessStarters.javaCommand("javac") //
				.workingDir(workspace) //
				.addArguments("-Xlint:all") //
				.addArguments("--release", "17") //
				.addArguments("-proc:none") //
				.addArguments("-d", workspace.resolve("bin").toString()) //
				.addArguments("--class-path", MavenRepo.jar("junit-platform-console-standalone").toString()) //
				.addArguments(workspace.resolve("src/OneMillionTests.java").toString()) //
				.redirectOutput(javacOutputFiles) //
				.startAndWait();

		assertThat(result.exitCode()).isZero();
		assertThat(result.stdOut()).isEmpty();
		assertThat(result.stdErr()).isEmpty();
	}

	private ProcessResult executeWithSmallHeapSize(OutputFiles outputFiles) throws Exception {
		return ProcessStarters.java() //
				.workingDir(workspace) //
				.addArguments("-Xmx16m") //
				.addArguments("-jar", MavenRepo.jar("junit-platform-console-standalone").toString()) //
				.addArguments("execute") //
				.addArguments("--scan-class-path") //
				.addArguments("--disable-banner") //
				.addArguments("--classpath", "bin") //
				.addArguments("--details=summary") //
				.addArguments("--config=%s=%s".formatted(MEMORY_CLEANUP_ENABLED_PROPERTY_NAME, true)) //
				.redirectOutput(outputFiles) //
				.startAndWait();
	}

}
