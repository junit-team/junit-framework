/*
 * Copyright 2015-2026 the original author or authors.
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v2.0 which
 * accompanies this distribution and is available at
 *
 * https://www.eclipse.org/legal/epl-v20.html
 */

/// Annotation processor to generate machine-readable configuration parameter
/// documentation.
///
/// The annotation processor all configuration parameters marked with
/// {@link org.junit.platform.configuration.api.ConfigurationParameter} to
/// {@value org.junit.platform.configuration.processor.ConfigurationMetadataAnnotationProcessor#METADATA_PATH}
/// in [Spring Boot's Configuration Metadata](https://docs.spring.io/spring-boot/specification/configuration-metadata/format.html)
/// format. This enables IDEs and other tools to process and validate Test
/// Engine configuration.
///
/// <h4>Usage</h4>
///
/// Given this minimal example:
///
/// <pre>{@code
/// /**
///   * A brief multi-line description of
///   * this property: {@value}.
///   *
///   * <p>Followed by an additional paragraph.
///   */
///  @ConfigurationParameter
///  public static final String EXAMPLE_PROPERTY_NAME = "org.example.property";
///
/// }</pre>
///
/// Processing the annotations with {@code org.junit.platform:junit-platform-configuration-processor}
/// will produce:
///
/// <pre>{@code
/// {
///   "properties": [
/// 	    {
/// 	  "name": "org.example.property",
///       "description": "A brief multi-line description of this property.",
/// 	  "sourceType": "com.example.app.Constants"
///    }
///   ]
/// }
/// }
/// </pre>
///
/// Of note is that the first paragraph from the doc string will be used
/// to describe the property. If the first paragraph ends with
/// {@code : {@value}.} or {@code : {@value}} it will be replaced with
/// {@code .}. Likewise {@code {@code}} and {@code {@link}} tags are replaced
/// with plain text versions.
///
module org.junit.platform.configuration.processor {
	requires static transitive org.jspecify;
	requires static transitive org.apiguardian.api;

	requires java.compiler;
	requires org.junit.platform.configuration.api;

	provides javax.annotation.processing.Processor with org.junit.platform.configuration.processor.ConfigurationMetadataAnnotationProcessor;
}
