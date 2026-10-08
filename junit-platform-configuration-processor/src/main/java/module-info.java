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
/// For further details and examples see <a href="https://docs.junit.org/current/advanced-topics/configuration-parameter-documentation.html">User Guide</a>.
module org.junit.platform.configuration.processor {
	requires static transitive org.jspecify;
	requires static transitive org.apiguardian.api;

	requires java.compiler;
	requires org.junit.platform.configuration.api;

	provides javax.annotation.processing.Processor with org.junit.platform.configuration.processor.ConfigurationMetadataAnnotationProcessor;
}
