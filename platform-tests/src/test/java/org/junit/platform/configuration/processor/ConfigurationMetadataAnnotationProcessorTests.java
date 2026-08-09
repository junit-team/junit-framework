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

import org.assertj.core.api.AbstractStringAssert;
import org.assertj.core.api.ThrowableAssert;
import org.intellij.lang.annotations.Language;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.platform.commons.PreconditionViolationException;
import org.junit.platform.configuration.testcases.ClassDeprecated;
import org.junit.platform.configuration.testcases.Documented;
import org.junit.platform.configuration.testcases.DocumentedWithAtValue;
import org.junit.platform.configuration.testcases.DocumentedWithHeader;
import org.junit.platform.configuration.testcases.DocumentedWithMultiLines;
import org.junit.platform.configuration.testcases.DocumentedWithMultipleParagraphs;
import org.junit.platform.configuration.testcases.Minimal;
import org.junit.platform.configuration.testcases.NonFinal;
import org.junit.platform.configuration.testcases.NonStatic;
import org.junit.platform.configuration.testcases.NonString;
import org.junit.platform.configuration.testcases.PropertyDeprecated;
import org.junit.platform.configuration.testcases.Without;

class ConfigurationMetadataAnnotationProcessorTests {

	final String expectedMetadataPath = "META-INF/junit-platform-configuration-metadata.json";

	final Path sourceDirectory = Path.of("src/test/java");

	@Nested
	class ConfigurationProperty {

		@TempDir
		Path outputDirectory;

		TestCompiler compiler;

		@BeforeEach
		void setup() {
			var processor = new ConfigurationMetadataAnnotationProcessor();
			compiler = new TestCompiler(sourceDirectory, outputDirectory, processor);
		}

		@Test
		void none() {
			compiler.compile(Without.class);
			var metaDataPath = outputDirectory.resolve(expectedMetadataPath);
			assertThat(metaDataPath).doesNotExist();
		}

		@Test
		void minimal() {
			compiler.compile(Minimal.class);
			assertMetaDataIsEqualTo("""
					{
					  "properties": [
						{
						  "name": "org.example.property",
						  "sourceType": "org.junit.platform.configuration.testcases.Minimal"
						}
					  ]
					}""");
		}

		@Test
		void documented() {
			compiler.compile(Documented.class);
			assertMetaDataIsEqualTo("""
					{
					  "properties": [
						{
						  "name": "org.example.property",
						  "description": "A brief description of this property.",
						  "sourceType": "org.junit.platform.configuration.testcases.Documented"
						}
					  ]
					}""");
		}

		@Test
		void documentedWithAtValue() {
			compiler.compile(DocumentedWithAtValue.class);
			assertMetaDataIsEqualTo("""
					{
					  "properties": [
						{
						  "name": "org.example.property",
						  "description": "A brief description of this property.",
						  "sourceType": "org.junit.platform.configuration.testcases.DocumentedWithAtValue"
						}
					  ]
					}""");
		}

		@Test
		void documentedWithMultipleLines() {
			compiler.compile(DocumentedWithMultiLines.class);
			assertMetaDataIsEqualTo("""
					{
					  "properties": [
						{
						  "name": "org.example.property",
						  "description": "A brief multi-line description of this property.",
						  "sourceType": "org.junit.platform.configuration.testcases.DocumentedWithMultiLines"
						}
					  ]
					}""");
		}

		@Test
		void documentedWithMultipleParagraphs() {
			compiler.compile(DocumentedWithMultipleParagraphs.class);
			assertMetaDataIsEqualTo("""
					{
					  "properties": [
						{
						  "name": "org.example.property",
						  "description": "A brief description of this property.",
						  "sourceType": "org.junit.platform.configuration.testcases.DocumentedWithMultipleParagraphs"
						}
					  ]
					}""");
		}

		@Test
		void documentedWithHeader() {
			compiler.compile(DocumentedWithHeader.class);
			assertMetaDataIsEqualTo("""
					{
					  "properties": [
						{
						  "name": "org.example.property",
						  "description": "A brief description of this property.",
						  "sourceType": "org.junit.platform.configuration.testcases.DocumentedWithHeader"
						}
					  ]
					}""");
		}

		@Test
		void deprecated() {
			// TODO: Inheritance? Meta?
			// TODO: Warning level?
			compiler.compile(PropertyDeprecated.class);
			assertMetaDataIsEqualTo("""
					{
					  "properties": [
					    {
					      "name": "org.example.property",
					      "sourceType": "org.junit.platform.configuration.testcases.PropertyDeprecated",
					      "deprecation": { }
					    }
					  ]
					}""");
		}

		@SuppressWarnings("deprecation")
		@Test
		@Disabled("Not yet implemented")
		void classDeprecated() {
			// TODO: Inheritance? Meta?
			compiler.compile(ClassDeprecated.class);
			assertMetaDataIsEqualTo("""
					{
					  "properties": [
						{
						  "name": "org.example.property",
						  "sourceType": "org.junit.platform.configuration.testcases.ClassDeprecated",
						  "deprecation": { }
						}
					  ]
					}""");
		}

		@Test
		void mustBeFinal() {
			asserPreconditionViolation(() -> compiler.compile(NonFinal.class),
				"Field [%s.EXAMPLE_PROPERTY_NAME] must be declared final".formatted(NonFinal.class.getName()));
		}

		@Test
		void mustBeStatic() {
			asserPreconditionViolation(() -> compiler.compile(NonStatic.class),
				"Field [%s.EXAMPLE_PROPERTY_NAME] must be declared static".formatted(NonStatic.class.getName()));
		}

		@Test
		void mustBeString() {
			asserPreconditionViolation(() -> compiler.compile(NonString.class),
				"Field [%s.EXAMPLE_PROPERTY_NAME] must have a constant string value".formatted(
					NonString.class.getName()));
		}

		private AbstractStringAssert<?> assertMetaDataIsEqualTo(@Language("JSON") String json) {
			return assertThat(metaData()).isEqualToIgnoringWhitespace(json);
		}

		private String metaData() throws UncheckedIOException {
			try {
				var metaDataPath = outputDirectory.resolve(expectedMetadataPath);
				return Files.readString(metaDataPath);
			}
			catch (IOException e) {
				throw new UncheckedIOException(e);
			}
		}
	}

	private static void asserPreconditionViolation(ThrowableAssert.ThrowingCallable throwingCallable, String message) {
		assertThatThrownBy(throwingCallable) //
				.hasRootCauseExactlyInstanceOf(PreconditionViolationException.class) //
				.hasRootCauseMessage(message);
	}

}
