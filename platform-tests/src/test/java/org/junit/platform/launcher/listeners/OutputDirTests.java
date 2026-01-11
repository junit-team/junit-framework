/*
 * Copyright 2015-2026 the original author or authors.
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v2.0 which
 * accompanies this distribution and is available at
 *
 * https://www.eclipse.org/legal/epl-v20.html
 */

package org.junit.platform.launcher.listeners;

import static org.assertj.core.api.Assertions.as;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.InstanceOfAssertFactories.STRING;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.platform.launcher.LauncherConstants;

class OutputDirTests {

	@TempDir
	Path cwd;

	@Test
	void getOutputDirUsesCustomOutputDir() throws Exception {
		var customDir = cwd.resolve("custom-dir");
		var outputDir = OutputDir.create(Optional.of(customDir.toAbsolutePath().toString())).toPath();
		assertThat(Files.isSameFile(customDir, outputDir)).isTrue();
		assertThat(outputDir).exists();
	}

	@Test
	void getOutputDirUsesCustomOutputDirWithPlaceholder() {
		var customDir = cwd.resolve("build").resolve("junit-" + LauncherConstants.OUTPUT_DIR_UNIQUE_NUMBER_PLACEHOLDER);
		var outputDir = OutputDir.create(Optional.of(customDir.toAbsolutePath().toString())).toPath();
		assertThat(outputDir).exists() //
				.hasParent(cwd.resolve("build")) //
				.extracting(it -> it.getFileName().toString(), as(STRING)) //
				.matches("junit-\\d+");
	}

	@Test
	void getOutputDirFallsBackToCurrentWorkingDir() throws Exception {
		var expected = cwd;

		assertOutputDirIsDetected(expected);
	}

	@Test
	void getOutputDirDetectsMavenPom() throws Exception {
		Files.createFile(cwd.resolve("pom.xml"));
		var expected = cwd.resolve("target");

		assertOutputDirIsDetected(expected);
	}

	@Test
	void getOutputDirDetectsGradleGroovyDefaultBuildScript() throws Exception {
		Files.createFile(cwd.resolve("build.gradle"));
		var expected = cwd.resolve("build");

		assertOutputDirIsDetected(expected);
	}

	@Test
	void getOutputDirDetectsGradleGroovyCustomBuildScript() throws Exception {
		Files.createFile(cwd.resolve("sub-project.gradle"));
		var expected = cwd.resolve("build");

		assertOutputDirIsDetected(expected);
	}

	@Test
	void getOutputDirDetectsGradleKotlinDefaultBuildScript() throws Exception {
		Files.createFile(cwd.resolve("build.gradle.kts"));
		var expected = cwd.resolve("build");

		assertOutputDirIsDetected(expected);
	}

	@Test
	void getOutputDirDetectsGradleKotlinCustomBuildScript() throws Exception {
		Files.createFile(cwd.resolve("sub-project.gradle.kts"));
		var expected = cwd.resolve("build");

		assertOutputDirIsDetected(expected);
	}

	private void assertOutputDirIsDetected(Path expected) throws IOException {
		var outputDir = OutputDir.createSafely(Optional.empty(), () -> cwd).toPath();
		assertThat(Files.isSameFile(expected, outputDir)).isTrue();
		assertThat(outputDir).exists();
	}

}
