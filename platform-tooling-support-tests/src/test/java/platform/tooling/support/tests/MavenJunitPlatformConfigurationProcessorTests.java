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
import static platform.tooling.support.Projects.copyToWorkspace;

import java.nio.file.Path;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.platform.tests.process.OutputFiles;
import org.opentest4j.TestAbortedException;

import platform.tooling.support.FilePrefix;
import platform.tooling.support.Helper;
import platform.tooling.support.MavenRepo;
import platform.tooling.support.ProcessStarters;
import platform.tooling.support.Projects;

/**
 * @since 6.2.0
 */
class MavenJunitPlatformConfigurationProcessorTests {

	@ManagedResource
	LocalMavenRepo localMavenRepo;

	@Test
	void processesAnnotationsIntoMetaData(@TempDir Path workspace, @FilePrefix("maven") OutputFiles outputFiles)
			throws Exception {
		var result = ProcessStarters.maven(Helper.getJavaHome(17).orElseThrow(TestAbortedException::new)) //
				.workingDir(copyToWorkspace(Projects.MAVEN_JUNIT_PLATFORM_CONFIGURATION_PROCESSOR, workspace)) //
				.addArguments(localMavenRepo.toCliArgument(), "-Dmaven.repo=" + MavenRepo.dir()) //
				.addArguments("--update-snapshots", "--batch-mode", "compile") //
				.redirectOutput(outputFiles) //
				.startAndWait();

		assertEquals(0, result.exitCode());
		assertEquals("", result.stdErr());

		var output = result.stdOutLines();
		assertTrue(output.contains("[INFO] BUILD SUCCESS"));

		var metaData = workspace.resolve("target/classes/META-INF/junit-platform-configuration-metadata.json");
		assertThat(metaData).exists().content().isEqualToNormalizingWhitespace("""
				{
				   "properties": [
				       {
						   "name": "org.example.property",
						   "sourceType": "org.example.Constants"
					   }
				   ]
				}
				""");
	}
}
