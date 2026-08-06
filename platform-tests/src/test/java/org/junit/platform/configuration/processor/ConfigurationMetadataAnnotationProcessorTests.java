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
			compiler.compile(WithoutConfigurationProperty.class);
			var metaDataPath = outputDirectory.resolve(expectedMetadataPath);
			assertThat(metaDataPath).doesNotExist();
		}

		@Test
		void minimal() {
			compiler.compile(MinimalConfigurationProperty.class);
			assertMetaDataIsEqualTo("""
					{
					  "properties": [
						{
						  "name": "org.example.property",
						  "sourceType": "org.junit.platform.configuration.processor.MinimalConfigurationProperty"
						}
					  ]
					}""");
		}

		@Test
		void documented() {
			compiler.compile(DocumentedConfigurationProperty.class);
			assertMetaDataIsEqualTo("""
					{
					  "properties": [
						{
						  "name": "org.example.property",
						  "description": "A brief description of this property.",
						  "sourceType": "org.junit.platform.configuration.processor.DocumentedConfigurationProperty"
						}
					  ]
					}""");
		}

		@Test
		void documentedWithAtValue() {
			compiler.compile(DocumentedWithAtValueConfigurationProperty.class);
			assertMetaDataIsEqualTo(
				"""
						{
						  "properties": [
							{
							  "name": "org.example.property",
							  "description": "A brief description of this property.",
							  "sourceType": "org.junit.platform.configuration.processor.DocumentedWithAtValueConfigurationProperty"
							}
						  ]
						}""");
		}

		@Test
		void documentedWithMultipleLines() {
			compiler.compile(DocumentedWithMultiLinesConfigurationProperty.class);
			assertMetaDataIsEqualTo(
				"""
						{
						  "properties": [
							{
							  "name": "org.example.property",
							  "description": "A brief multi-line description of this property.",
							  "sourceType": "org.junit.platform.configuration.processor.DocumentedWithMultiLinesConfigurationProperty"
							}
						  ]
						}""");
		}

		@Test
		void documentedWithMultipleParagraphs() {
			compiler.compile(DocumentedWithMultipleParagraphsConfigurationProperty.class);
			assertMetaDataIsEqualTo(
				"""
						{
						  "properties": [
							{
							  "name": "org.example.property",
							  "description": "A brief description of this property.",
							  "sourceType": "org.junit.platform.configuration.processor.DocumentedWithMultipleParagraphsConfigurationProperty"
							}
						  ]
						}""");
		}

		@Test
		void documentedWithHeader() {
			compiler.compile(DocumentedWithHeaderConfigurationProperty.class);
			assertMetaDataIsEqualTo(
				"""
						{
						  "properties": [
							{
							  "name": "org.example.property",
							  "description": "A brief description of this property.",
							  "sourceType": "org.junit.platform.configuration.processor.DocumentedWithHeaderConfigurationProperty"
							}
						  ]
						}""");
		}

		@Test
		void deprecated() {
			// TODO: Inheritance? Meta?
			// TODO: Warning level?
			compiler.compile(DeprecatedConfigurationProperty.class);
			assertMetaDataIsEqualTo("""
					{
					  "properties": [
					    {
					      "name": "org.example.property",
					      "sourceType": "org.junit.platform.configuration.processor.DeprecatedConfigurationProperty",
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
			compiler.compile(ClassDeprecatedConfigurationProperty.class);
			assertMetaDataIsEqualTo(
				"""
						{
						  "properties": [
							{
							  "name": "org.example.property",
							  "sourceType": "org.junit.platform.configuration.processor.ClassDeprecatedConfigurationProperty",
							  "deprecation": { }
							}
						  ]
						}""");
		}

		@Test
		void mustBeFinal() {
			asserPreconditionViolation(() -> compiler.compile(NonFinalConfigurationProperty.class),
				"Field [%s.EXAMPLE_PROPERTY_NAME] must be declared final".formatted(
					NonFinalConfigurationProperty.class.getName()));
		}

		@Test
		void mustBeStatic() {
			asserPreconditionViolation(() -> compiler.compile(NonStaticConfigurationProperty.class),
				"Field [%s.EXAMPLE_PROPERTY_NAME] must be declared static".formatted(
					NonStaticConfigurationProperty.class.getName()));
		}

		@Test
		void mustBeString() {
			asserPreconditionViolation(() -> compiler.compile(NonStringConfigurationProperty.class),
				"Field [%s.EXAMPLE_PROPERTY_NAME] must have a constant string value".formatted(
					NonStringConfigurationProperty.class.getName()));
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
