/*
 * Copyright 2015-2026 the original author or authors.
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v2.0 which
 * accompanies this distribution and is available at
 *
 * https://www.eclipse.org/legal/epl-v20.html
 */

module org.junit.platform.configuration.processor {
	requires static transitive org.apiguardian.api;
	requires static transitive org.jspecify;

	requires java.compiler;
	requires org.junit.platform.configuration.api;

	provides javax.annotation.processing.Processor with org.junit.platform.configuration.processor.ConfigurationMetadataAnnotationProcessor;
}
