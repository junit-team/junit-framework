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
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;

import org.assertj.core.api.ThrowableAssert;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.platform.commons.PreconditionViolationException;

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
	void simpleConfigurationProperty() {
		compiler.compile(SimpleConfigurationProperty.class);
		assertThat(metaData()).isEqualTo("""
				{}
				""");
	}

	@Test
	void configurationPropertyMustBeFinal() {
		asserPreconditionViolation(() -> compiler.compile(NonFinalConfigurationProperty.class),
				"Field [NonFinalConfigurationProperty.EXAMPLE_PROPERTY_NAME] must have a constant value string");
	}

	@Test
	void configurationPropertyMustBeStatic() {
		compiler.compile(SimpleConfigurationProperty.class);
		assertThat(metaData()).isEqualTo("""
				{}
				""");
	}

	private static void asserPreconditionViolation(ThrowableAssert.ThrowingCallable throwingCallable, String message) {
		assertThatThrownBy(throwingCallable)
				.hasRootCauseExactlyInstanceOf(PreconditionViolationException.class)
				.hasRootCauseMessage(message);
	}

	private String metaData() throws UncheckedIOException {
		try {
			return Files.readString(outputDirectory.resolve(expectedMetadataPath));
		}
		catch (IOException e) {
			throw new UncheckedIOException(e);
		}
	}

}
