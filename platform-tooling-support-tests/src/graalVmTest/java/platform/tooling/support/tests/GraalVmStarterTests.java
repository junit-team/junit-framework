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
import static java.util.stream.Collectors.toSet;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assumptions.assumeTrue;
import static org.junit.platform.commons.util.StringUtils.isNotBlank;
import static platform.tooling.support.Projects.copyToWorkspace;

import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Set;
import java.util.stream.StreamSupport;

import org.junit.jupiter.api.Test;
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
import tools.jackson.databind.json.JsonMapper;

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

		var gradlew = ProcessStarters.gradlew() //
				.workingDir(copyToWorkspace(Projects.GRAALVM_STARTER, workspace)) //
				.addArguments("-Dmaven.repo=" + MavenRepo.dir()) //
				.addArguments("javaToolchains", "nativeTest", "--no-daemon", "--stacktrace", "--no-build-cache", //
					"--warning-mode=fail", //
					"-PjdkVersion=" + jdkVersion) //
				.redirectOutput(outputFiles) //
				.putEnvironment("GRAALVM_HOME", getGraalVmHome(jdkVersion));

		var result = gradlew.startAndWait();

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

	@Test
	@Timeout(value = 1, unit = MINUTES)
	void reachabilityMetadataContainsRequiredSerializableTypes(@TempDir Path workspace,
			@FilePrefix("gradle") OutputFiles outputFiles) throws Exception {

		var gradlew = ProcessStarters.gradlew() //
				.workingDir(copyToWorkspace(Projects.GRAALVM_STARTER, workspace)) //
				.addArguments("-Dmaven.repo=" + MavenRepo.dir()) //
				.addArguments("test", "-Pagent", "--no-daemon", "--stacktrace", "--no-build-cache", //
					"--warning-mode=fail", //
					"-PjdkVersion=25") //
				.redirectOutput(outputFiles) //
				.putEnvironment("GRAALVM_HOME", getGraalVmHome(25));

		var result = gradlew.startAndWait();

		assertEquals(0, result.exitCode());

		var agentOutput = workspace.resolve(Path.of("build/native/agent-output/test/reachability-metadata.json"));
		assertThat(readSerializableTypes(agentOutput)) //
				.isEqualTo(readSerializableTypesFromJar());
	}

	private static Set<String> readSerializableTypesFromJar() throws IOException {
		try (var fileSystem = FileSystems.newFileSystem(MavenRepo.jar("junit-platform-engine"))) {
			return readSerializableTypes(fileSystem.getPath(
				"META-INF/native-image/org.junit.platform/junit-platform-engine/reachability-metadata.json"));
		}
	}

	private static Set<String> readSerializableTypes(Path reachabilityMetadata) throws IOException {
		var json = new JsonMapper().readTree(Files.readString(reachabilityMetadata));
		return StreamSupport.stream(json.path("reflection").spliterator(), false) //
				.filter(entry -> entry.path("serializable").asBoolean(false)) //
				.map(entry -> entry.path("type").asString()) //
				.collect(toSet());
	}

	private static String getGraalVmHome(int jdkVersion) {
		var envVarName = "GRAALVM_%d_HOME".formatted(jdkVersion);
		var graalVmHome = System.getenv(envVarName);
		assumeTrue(isNotBlank(graalVmHome), () -> "Expected env var is not set: " + envVarName);
		return graalVmHome;
	}
}
