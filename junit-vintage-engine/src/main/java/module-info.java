/*
 * Copyright 2015-2025 the original author or authors.
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v2.0 which
 * accompanies this distribution and is available at
 *
 * https://www.eclipse.org/legal/epl-v20.html
 */

import org.junit.platform.engine.TestEngine;
import org.junit.vintage.engine.VintageTestEngine;

/**
 * Provides a {@link TestEngine} for running JUnit 3
 * and 4 based tests on the platform.
 *
 * @since 4.12
 * @provides TestEngine The {@code VintageTestEngine}
 * runs JUnit 3 and 4 based tests on the platform.
 */
@SuppressWarnings("deprecation")
module org.junit.vintage.engine {

	requires static org.apiguardian.api;
	requires static transitive org.jspecify;

	requires junit; // 4
	requires org.junit.platform.engine;

	provides TestEngine
			with VintageTestEngine;
}
