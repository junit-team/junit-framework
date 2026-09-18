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
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;

import org.intellij.lang.annotations.Language;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.platform.configuration.testcases.DefaultClassWithoutType;
import org.junit.platform.configuration.testcases.DefaultDifferentSets;
import org.junit.platform.configuration.testcases.DefaultMultipleValues;
import org.junit.platform.configuration.testcases.Defaults;
import org.junit.platform.configuration.testcases.Deprecation;
import org.junit.platform.configuration.testcases.DeprecationWithDetails;
import org.junit.platform.configuration.testcases.Documented;
import org.junit.platform.configuration.testcases.DocumentedWithAtCode;
import org.junit.platform.configuration.testcases.DocumentedWithAtLink;
import org.junit.platform.configuration.testcases.DocumentedWithAtLinkPlain;
import org.junit.platform.configuration.testcases.DocumentedWithAtSee;
import org.junit.platform.configuration.testcases.DocumentedWithAtValue;
import org.junit.platform.configuration.testcases.DocumentedWithHeader;
import org.junit.platform.configuration.testcases.DocumentedWithMultiLines;
import org.junit.platform.configuration.testcases.DocumentedWithMultipleParagraphs;
import org.junit.platform.configuration.testcases.Minimal;
import org.junit.platform.configuration.testcases.NonFinal;
import org.junit.platform.configuration.testcases.NonStatic;
import org.junit.platform.configuration.testcases.NonString;
import org.junit.platform.configuration.testcases.TypeAbstractClassWithDefaultClass;
import org.junit.platform.configuration.testcases.TypeClass;
import org.junit.platform.configuration.testcases.TypeEnum;
import org.junit.platform.configuration.testcases.TypeEnumWithDocumentedEnumValues;
import org.junit.platform.configuration.testcases.TypeEnumWithStringDefault;
import org.junit.platform.configuration.testcases.TypeInterface;
import org.junit.platform.configuration.testcases.TypeInterfaceWithDefaultClass;
import org.junit.platform.configuration.testcases.TypeString;
import org.junit.platform.configuration.testcases.Without;

/**
 * The dependencies of platform-tests depend on the
 * {@link ConfigurationMetadataAnnotationProcessor}. As a consequence, it is
 * likely that building the project will find problems with the processor
 * before this test does.
 */
class ConfigurationMetadataAnnotationProcessorTests {

	final String expectedMetadataPath = "META-INF/junit-platform-configuration-metadata.json";

	final Path sourceDirectory = Path.of("src/test/java");

	@Nested
	class ConfigurationParameter {

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
			compiler.compileWithoutError(Without.class);
			var metaDataPath = outputDirectory.resolve(expectedMetadataPath);
			assertThat(metaDataPath).doesNotExist();
		}

		@Test
		void minimal() {
			compiler.compileWithoutError(Minimal.class);
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
			compiler.compileWithoutError(Documented.class);
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
			compiler.compileWithoutError(DocumentedWithAtValue.class);
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
			compiler.compileWithoutError(DocumentedWithMultiLines.class);
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
			compiler.compileWithoutError(DocumentedWithMultipleParagraphs.class);
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
			compiler.compileWithoutError(DocumentedWithHeader.class);
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
		void documentedWithAtSee() {
			compiler.compileWithoutError(DocumentedWithAtSee.class);
			assertMetaDataIsEqualTo("""
					{
					  "properties": [
						{
						  "name": "org.example.property",
						  "description": "A brief description of this property.",
						  "sourceType": "org.junit.platform.configuration.testcases.DocumentedWithAtSee"
						}
					  ]
					}""");
		}

		@Test
		void documentedWithAtCode() {
			compiler.compileWithoutError(DocumentedWithAtCode.class);
			assertMetaDataIsEqualTo("""
					{
					  "properties": [
						{
						  "name": "org.example.property",
						  "description": "Some example code in the first paragraph.",
						  "sourceType": "org.junit.platform.configuration.testcases.DocumentedWithAtCode"
						}
					  ]
					}""");
		}

		@Test
		void documentedWithAtLink() {
			compiler.compileWithoutError(DocumentedWithAtLink.class);
			assertMetaDataIsEqualTo("""
					{
					  "properties": [
						{
						  "name": "org.example.property",
						  "description": "Some DocumentedWithAtLink and document with at link in the first paragraph.",
						  "sourceType": "org.junit.platform.configuration.testcases.DocumentedWithAtLink"
						}
					  ]
					}""");
		}

		@Test
		void documentedWithAtLinkPlain() {
			compiler.compileWithoutError(DocumentedWithAtLinkPlain.class);
			assertMetaDataIsEqualTo(
				"""
						{
						  "properties": [
							{
							  "name": "org.example.property",
							  "description": "Some document with at linkplain and DocumentedWithAtLinkPlain in the first paragraph.",
							  "sourceType": "org.junit.platform.configuration.testcases.DocumentedWithAtLinkPlain"
							}
						  ]
						}""");
		}

		@Test
		void deprecation() {
			compiler.compileWithoutError(Deprecation.class);
			assertMetaDataIsEqualTo("""
					{
					  "properties": [
					    {
					      "name": "org.example.property",
					      "sourceType": "org.junit.platform.configuration.testcases.Deprecation",
					      "deprecation": { }
					    }
					  ]
					}""");
		}

		@Test
		void deprecationWithDetails() {
			compiler.compileWithoutError(DeprecationWithDetails.class);
			assertMetaDataIsEqualTo("""
					{
					  "properties": [
					    {
					      "name": "org.example.property",
					      "sourceType": "org.junit.platform.configuration.testcases.DeprecationWithDetails",
					      "deprecation": {
					          "reason": "This property was migrated to com.example",
					          "replacement": "com.example.property",
					          "since": "2.0.0"
					        }
					    }
					  ]
					}""");
		}

		@Test
		void classType() {
			compiler.compileWithoutError(TypeClass.class);
			assertMetaDataIsEqualTo("""
					{
					  "properties": [
						{
						  "name": "org.example.property",
						  "type": "org.junit.platform.configuration.testcases.TypeClass.Example",
						  "sourceType": "org.junit.platform.configuration.testcases.TypeClass"
						}
					  ],
					  "hints": [
					  	{
					  	  "name": "org.example.property",
					  	  "providers": [
					  		  {
					  			  "name": "class-reference",
					  			  "parameters": {
					  				  "target": "org.junit.platform.configuration.testcases.TypeClass.Example"
					  			  }
					  		  }
					  	  ]
					  	}
					  ]
					}""");
		}

		@Test
		void classTypeAbstractWithDefaultClass() {
			compiler.compileWithoutError(TypeAbstractClassWithDefaultClass.class);
			assertMetaDataIsEqualTo(
				"""
						{
							"properties": [
							  {
								"name": "org.example.classes",
								"type": "org.junit.platform.configuration.testcases.TypeAbstractClassWithDefaultClass.Example",
								"sourceType": "org.junit.platform.configuration.testcases.TypeAbstractClassWithDefaultClass",
								"defaultValue": "org.junit.platform.configuration.testcases.TypeAbstractClassWithDefaultClass.Default"
							  }
							],
							"hints": [
							 {
								 "name": "org.example.classes",
								 "providers": [
									 {
										 "name": "class-reference",
										 "parameters": {
											 "target": "org.junit.platform.configuration.testcases.TypeAbstractClassWithDefaultClass.Example"
										 }
									 }
								 ]
							 }
						  ]
						}""");
		}

		@Test
		void interfaceType() {
			compiler.compileWithoutError(TypeInterface.class);
			assertMetaDataIsEqualTo("""
					{
					  "properties": [
						{
						  "name": "org.example.property",
						  "type": "org.junit.platform.configuration.testcases.TypeInterface.Example",
						  "sourceType": "org.junit.platform.configuration.TypeInterface.TypeClass"
						}
					  ],
					  "hints": [
					  	{
					  	  "name": "org.example.property",
					  	  "providers": [
					  		  {
					  			  "name": "class-reference",
					  			  "parameters": {
					  				  "target": "org.junit.platform.configuration.testcases.TypeClass.Example"
					  			  }
					  		  }
					  	  ]
					  	}
					  ]
					}""");
		}

		@Test
		void interfaceTypeWithDefaultClass() {
			compiler.compileWithoutError(TypeInterfaceWithDefaultClass.class);
			assertMetaDataIsEqualTo(
				"""
						{
							"properties": [
							  {
								"name": "org.example.classes",
								"type": "org.junit.platform.configuration.testcases.TypeInterfaceWithDefaultClass.Example",
								"sourceType": "org.junit.platform.configuration.testcases.TypeInterfaceWithDefaultClass",
								"defaultValue": "org.junit.platform.configuration.testcases.TypeInterfaceWithDefaultClass.Default"
							  }
							],
							"hints": [
							 {
								 "name": "org.example.classes",
								 "providers": [
									 {
										 "name": "class-reference",
										 "parameters": {
											 "target": "org.junit.platform.configuration.testcases.TypeInterfaceWithDefaultClass.Example"
										 }
									 }
								 ]
							 }
						  ]
						}""");
		}

		@Test
		void stringType() {
			compiler.compileWithoutError(TypeString.class);
			assertMetaDataIsEqualTo("""
					{
					  "properties": [
						{
						  "name": "org.example.property",
						  "type": "java.lang.String",
						  "sourceType": "org.junit.platform.configuration.testcases.TypeString"
						}
					  ]
					}""");
		}

		@Test
		void enumType() {
			compiler.compileWithoutError(TypeEnum.class);
			assertMetaDataIsEqualTo("""
					{
					  "properties": [
						{
						  "name": "org.example.property",
						  "type": "org.junit.platform.configuration.testcases.TypeEnum.ExampleEnum",
						  "sourceType": "org.junit.platform.configuration.testcases.TypeEnum"
						}
					  ],
					  "hints": [
						{
						  "name": "org.example.property",
						  "values": [
							  {
								  "value": "A"
							  },
							  {
								  "value": "B"
							  }
						  ]
						}
					  ]
					}""");
		}

		@Test
		void enumTypeWithStringDefault() {
			compiler.compileWithoutError(TypeEnumWithStringDefault.class);
			assertMetaDataIsEqualTo("""
					{
					  "properties": [
						{
						  "name": "org.example.property",
						  "type": "org.junit.platform.configuration.testcases.TypeEnumWithStringDefault.ExampleEnum",
						  "sourceType": "org.junit.platform.configuration.testcases.TypeEnumWithStringDefault",
						  "defaultValue": "A"
						}
					  ],
					  "hints": [
						{
						  "name": "org.example.property",
						  "values": [
							  {
								  "value": "A"
							  },
							  {
								  "value": "B"
							  }
						  ]
						}
					  ]
					}""");
		}

		@Test
		void enumTypeWithDocumentedEnumValues() {
			compiler.compileWithoutError(TypeEnumWithDocumentedEnumValues.class);
			assertMetaDataIsEqualTo(
				"""
						{
						  "properties": [
							{
							  "name": "org.example.property",
							  "type": "org.junit.platform.configuration.testcases.TypeEnumWithDocumentedEnumValues.ExampleEnum",
							  "sourceType": "org.junit.platform.configuration.testcases.TypeEnumWithDocumentedEnumValues"
							}
						  ],
						  "hints": [
							{
							  "name": "org.example.property",
							  "values": [
								  {
									  "value": "A",
									  "description": "A is the first option."
								  },
								  {
									  "value": "B",
									  "description": "B is the second option."
								  }
							  ]
							}
						  ]
						}""");
		}

		@Test
		void defaults() {
			compiler.compileWithoutError(Defaults.class);
			assertMetaDataIsEqualTo("""
					{
						"properties": [
						  {
							"name": "org.example.shorts",
							"type": "java.lang.Short",
							"sourceType": "org.junit.platform.configuration.testcases.Defaults",
							"defaultValue": 1
						  },
						  {
							"name": "org.example.bytes",
							"type": "java.lang.Byte",
							"sourceType": "org.junit.platform.configuration.testcases.Defaults",
							"defaultValue": 42
						  },
						  {
							"name": "org.example.ints",
							"type": "java.lang.Integer",
							"sourceType": "org.junit.platform.configuration.testcases.Defaults",
							"defaultValue": 42
						  },
						  {
							"name": "org.example.longs",
							"type": "java.lang.Long",
							"sourceType": "org.junit.platform.configuration.testcases.Defaults",
							"defaultValue": 42
						  },
						  {
							"name": "org.example.floats",
							"type": "java.lang.Float",
							"sourceType": "org.junit.platform.configuration.testcases.Defaults",
							"defaultValue": 42.0
						  },
						  {
							"name": "org.example.doubles",
							"type": "java.lang.Double",
							"sourceType": "org.junit.platform.configuration.testcases.Defaults",
							"defaultValue": 42.0
						  },
						  {
							"name": "org.example.chars",
							"type": "java.lang.Character",
							"sourceType": "org.junit.platform.configuration.testcases.Defaults",
							"defaultValue": "4"
						  },
						  {
							"name": "org.example.booleans",
							"type": "java.lang.Boolean",
							"sourceType": "org.junit.platform.configuration.testcases.Defaults",
							"defaultValue": true
						  },
						  {
							"name": "org.example.strings",
							"type": "java.lang.String",
							"sourceType": "org.junit.platform.configuration.testcases.Defaults",
							"defaultValue": "default"
						  },
						  {
							"name": "org.example.classes",
							"type": "org.junit.platform.configuration.testcases.Defaults.Example",
							"sourceType": "org.junit.platform.configuration.testcases.Defaults",
							"defaultValue": "org.junit.platform.configuration.testcases.Defaults.Default"
						  }
						],
						"hints": [
						  {
							  "name": "org.example.booleans",
							  "values": [
								  {
									  "value": true
								  },
								  {
									  "value": false
								  }
							  ]
						  },
						 {
							 "name": "org.example.classes",
							 "providers": [
								 {
									 "name": "class-reference",
									 "parameters": {
										 "target": "org.junit.platform.configuration.testcases.Defaults.Example"
									 }
								 }
							 ]
						 }
					  ]
					}""");
		}

		@Test
		void mustBeFinal() {
			var result = compiler.compile(NonFinal.class);
			assertThat(result.diagnostics()) //
					.extracting(diagnostic -> diagnostic.getMessage(Locale.ROOT)) //
					.contains(
						"@ConfigurationParameter annotated field must be static, final, and have constant string value");
		}

		@Test
		void mustBeStatic() {
			var result = compiler.compile(NonStatic.class);
			assertThat(result.diagnostics()) //
					.extracting(diagnostic -> diagnostic.getMessage(Locale.ROOT)) //
					.contains(
						"@ConfigurationParameter annotated field must be static, final, and have constant string value");
		}

		@Test
		void mustBeString() {
			var result = compiler.compile(NonString.class);
			assertThat(result.diagnostics()) //
					.extracting(diagnostic -> diagnostic.getMessage(Locale.ROOT)) //
					.contains(
						"@ConfigurationParameter annotated field must be static, final, and have constant string value");
		}

		@Test
		void mustHaveExactlyOneSetOfDefaults() {
			var result = compiler.compile(DefaultDifferentSets.class);
			assertThat(result.diagnostics()) //
					.extracting(diagnostic -> diagnostic.getMessage(Locale.ROOT)) //
					.contains("@ConfigurationParameter must have exactly one default value");
		}

		@Test
		void mustHaveExactlyOneDefaultValue() {
			var result = compiler.compile(DefaultMultipleValues.class);
			assertThat(result.diagnostics()) //
					.extracting(diagnostic -> diagnostic.getMessage(Locale.ROOT)) //
					.contains("@ConfigurationParameter must have exactly one default value");
		}

		@Test
		void mustHaveTypeWhenDefaultValueIsClass() {
			var result = compiler.compile(DefaultClassWithoutType.class);
			assertThat(result.diagnostics()) //
					.extracting(diagnostic -> diagnostic.getMessage(Locale.ROOT)) //
					.contains("@ConfigurationParameter must declare a type when the default value is a classValue");
		}

		private void assertMetaDataIsEqualTo(@Language("JSON") String json) {
			assertThat(metaData()).isEqualToNormalizingWhitespace(json);
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

}
