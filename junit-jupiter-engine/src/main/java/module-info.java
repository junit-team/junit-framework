/*
 * Copyright 2015-2025 the original author or authors.
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v2.0 which
 * accompanies this distribution and is available at
 *
 * https://www.eclipse.org/legal/epl-v20.html
 */

import org.junit.jupiter.api.extension.Extension;
import org.junit.jupiter.engine.JupiterTestEngine;
import org.junit.platform.engine.TestEngine;

/**
 * Provides the JUnit Jupiter {@link TestEngine}
 * implementation.
 *
 * @since 5.0
 * @uses Extension
 * @provides TestEngine The {@code JupiterTestEngine}
 * runs Jupiter based tests on the platform.
 */
module org.junit.jupiter.engine {

	requires static org.apiguardian.api;
	requires static transitive org.jspecify;

	requires org.junit.jupiter.api;
	requires org.junit.platform.commons;
	requires org.junit.platform.engine;
	requires org.opentest4j;

	// exports org.junit.jupiter.engine; // Constants...

	uses Extension;

	provides TestEngine
			with JupiterTestEngine;

	opens org.junit.jupiter.engine.extension to org.junit.platform.commons;
}
