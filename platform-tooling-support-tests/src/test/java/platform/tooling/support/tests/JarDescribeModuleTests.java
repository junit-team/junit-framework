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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertLinesMatch;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static platform.tooling.support.tests.Projects.getSourceDirectory;

import java.lang.module.ModuleFinder;
import java.nio.file.Files;

import org.junit.jupiter.api.Order;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.platform.tests.process.OutputFiles;

import platform.tooling.support.Helper;
import platform.tooling.support.MavenRepo;
import platform.tooling.support.ProcessStarters;

/**
 * @since 1.3
 */
@Order(Integer.MAX_VALUE)
class JarDescribeModuleTests {

	@ParameterizedTest(quoteTextArguments = false)
	@MethodSource("platform.tooling.support.Helper#loadModuleDirectoryNames")
	void describeModule(String module, @FilePrefix("jar") OutputFiles outputFiles) throws Exception {
		var sourceDirectory = getSourceDirectory(Projects.JAR_DESCRIBE_MODULE);
		var modulePath = MavenRepo.jar(module);

		var result = ProcessStarters.javaCommand("jar") //
				.workingDir(sourceDirectory) //
				.addArguments("--describe-module", "--file", modulePath.toAbsolutePath().toString()) //
				.redirectOutput(outputFiles) //
				.startAndWait();

		assertEquals(0, result.exitCode());
		assertEquals("", result.stdErr(), "error log isn't empty");

		var expectedLines = replaceVersionPlaceholders(
			Files.readString(sourceDirectory.resolve(module + ".expected.txt")).strip());
		assertLinesMatch(expectedLines.lines().toList(), result.stdOut().strip().lines().toList());
	}

	@ParameterizedTest(quoteTextArguments = false)
	@MethodSource("platform.tooling.support.Helper#loadModuleDirectoryNames")
	void packageNamesStartWithNameOfTheModule(String module) {
		var modulePath = MavenRepo.jar(module);
		var moduleDescriptor = ModuleFinder.of(modulePath).findAll().iterator().next().descriptor();
		var moduleName = moduleDescriptor.name();
		for (var packageName : moduleDescriptor.packages()) {
			assertTrue(packageName.startsWith(moduleName));
		}
	}

	private static String replaceVersionPlaceholders(String line) {
		line = line.replace("${version}", Helper.version());
		return line;
	}

}
