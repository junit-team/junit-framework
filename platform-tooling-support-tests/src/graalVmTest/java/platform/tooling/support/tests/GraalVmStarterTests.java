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

import static java.util.concurrent.TimeUnit.MINUTES;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assumptions.assumeTrue;
import static org.junit.platform.commons.util.StringUtils.isNotBlank;
import static platform.tooling.support.Projects.copyToWorkspace;

import java.nio.file.Path;

import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.api.extension.DisabledOnOpenJ9;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.junit.platform.tests.process.OutputFiles;

import platform.tooling.support.FilePrefix;
import platform.tooling.support.MavenRepo;
import platform.tooling.support.ProcessStarters;
import platform.tooling.support.Projects;

/**
 * @since 1.9.1
 */
@DisabledOnOpenJ9
class GraalVmStarterTests {

	@ParameterizedTest
	@ValueSource(ints = { 21, 25 })
	@Timeout(value = 10, unit = MINUTES)
	void runsTestsInNativeImage(int jdkVersion, @TempDir Path workspace, @FilePrefix("gradle") OutputFiles outputFiles)
			throws Exception {
		var envVarName = "GRAALVM_%d_HOME".formatted(jdkVersion);
		var graalVmHome = System.getenv(envVarName);
		assumeTrue(isNotBlank(graalVmHome), () -> "Expected env var is not set: " + envVarName);

		var result = ProcessStarters.gradlew() //
				.workingDir(copyToWorkspace(Projects.GRAALVM_STARTER, workspace)) //
				.addArguments("-Dmaven.repo=" + MavenRepo.dir()) //
				.addArguments("javaToolchains", "nativeTest", "--no-daemon", "--stacktrace", "--no-build-cache", //
					// Use `--warning-mode=fail` again after the following issue is resolved and released:
					// https://github.com/graalvm/native-build-tools/issues/900
					"--warning-mode=all", //
					"--refresh-dependencies", //
					"-PjdkVersion=" + jdkVersion) //
				.redirectOutput(outputFiles) //
				.putEnvironment("GRAALVM_HOME", graalVmHome) //
				.startAndWait();

		assertEquals(0, result.exitCode());
		assertThat(result.stdOutLines()) //
				.anyMatch(line -> line.contains("CalculatorTests > 1 + 1 = 2 SUCCESSFUL")) //
				.anyMatch(line -> line.contains("CalculatorTests > 1 + 100 = 101 SUCCESSFUL")) //
				.anyMatch(line -> line.contains(
					"ClassLevelAnnotationTests$Inner > ClassLevelAnnotationTests, Inner, test SUCCESSFUL")) //
				.anyMatch(
					line -> line.contains("com.example.project.CalculatorParameterizedClassTests > [1] 1 SUCCESSFUL")) //
				.anyMatch(
					line -> line.contains("com.example.project.CalculatorParameterizedClassTests > [2] 2 SUCCESSFUL")) //
				.anyMatch(line -> line.contains("com.example.project.VintageTests > test SUCCESSFUL")) //
				.anyMatch(line -> line.contains("BUILD SUCCESSFUL"));
	}
}
