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
import static platform.tooling.support.tests.Projects.copyToWorkspace;

import java.io.IOException;
import java.nio.file.Path;

import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.platform.tests.process.OutputFiles;
import org.junit.platform.tests.process.ProcessResult;
import org.opentest4j.TestAbortedException;

import platform.tooling.support.Helper;
import platform.tooling.support.MavenRepo;
import platform.tooling.support.ProcessStarters;

/**
 * @since 6.0
 */
@Order(Integer.MIN_VALUE + 1)
class KotlinCoroutinesTests {

	@Test
	void failsExpectedlyWhenAllOptionalDependenciesArePresent(@TempDir Path workspace,
			@FilePrefix("gradle") OutputFiles outputFiles) throws Exception {
		var result = runBuild(workspace, outputFiles);

		assertEquals(1, result.exitCode(), "result=" + result);
		assertThat(result.stdOut()).contains("AssertionFailedError: expected");
		assertThat(result.stdErr()).contains("BUILD FAILED");
	}

	@Test
	void failsWithHelpfulErrorMessageWhenKotlinxCoroutinesIsMissing(@TempDir Path workspace,
			@FilePrefix("gradle") OutputFiles outputFiles) throws Exception {
		var result = runBuild(workspace, outputFiles, "-PwithoutKotlinxCoroutines");

		assertEquals(1, result.exitCode(), "result=" + result);
		assertThat(result.stdOut()).contains("PreconditionViolationException: Kotlin suspending function "
				+ "[public final java.lang.Object com.example.project.SuspendFunctionTests.test(kotlin.coroutines.Continuation<? super kotlin.Unit>)] "
				+ "requires org.jetbrains.kotlinx:kotlinx-coroutines-core to be on the classpath or module path. "
				+ "Please add a corresponding dependency.");
		assertThat(result.stdErr()).contains("BUILD FAILED");
	}

	@Test
	void failsWithHelpfulErrorMessageWhenKotlinReflectIsMissing(@TempDir Path workspace,
			@FilePrefix("gradle") OutputFiles outputFiles) throws Exception {
		var result = runBuild(workspace, outputFiles, "-PwithoutKotlinReflect");

		assertEquals(1, result.exitCode(), "result=" + result);
		assertThat(result.stdOut()).contains("PreconditionViolationException: Kotlin suspending function "
				+ "[public final java.lang.Object com.example.project.SuspendFunctionTests.test(kotlin.coroutines.Continuation<? super kotlin.Unit>)] "
				+ "requires org.jetbrains.kotlin:kotlin-reflect to be on the classpath or module path. "
				+ "Please add a corresponding dependency.");
		assertThat(result.stdErr()).contains("BUILD FAILED");
	}

	private static ProcessResult runBuild(Path workspace, OutputFiles outputFiles, String... extraArgs)
			throws InterruptedException, IOException {

		return ProcessStarters.gradlew() //
				.workingDir(copyToWorkspace(Projects.KOTLIN_COROUTINES, workspace)) //
				.addArguments("-Dmaven.repo=" + MavenRepo.dir()) //
				.addArguments("build", "--no-daemon", "--stacktrace", "--no-build-cache", "--warning-mode=fail") //
				.addArguments(extraArgs).putEnvironment("JDK17",
					Helper.getJavaHome(17).orElseThrow(TestAbortedException::new).toString()) //
				.redirectOutput(outputFiles) //
				.startAndWait();
	}
}
