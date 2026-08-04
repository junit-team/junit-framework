/*
 * Copyright 2026 the original author or authors.
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v2.0 which
 * accompanies this distribution and is available at
 *
 * https://www.eclipse.org/legal/epl-v20.html
 */

package org.junit.platform.configuration.processor;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class ConfigurationMetadataAnnotationProcessorTests {

	final String expectedMetadataPath = "META-INF/junit-platform-configuration-metadata.json";

	final Path sourceDirectory = Path.of("src/test/java");

	@TempDir
	Path outputDirectory;

	TestCompiler compiler;

	@BeforeEach
	void setup() {
		var processor = new ConfigurationMetadataAnnotationProcessor();
		compiler = new TestCompiler(sourceDirectory, outputDirectory, processor);
	}

	@Test
	void test() throws IOException {
		compiler.compile(OneConfigurationProperty.class);
		assertThat(metaData()).isEqualTo("""
				{}
				""");
	}

	private String metaData() throws IOException {
		return Files.readString(outputDirectory.resolve(expectedMetadataPath));
	}

}
