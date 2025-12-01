/*
 * Copyright 2015-2025 the original author or authors.
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v2.0 which
 * accompanies this distribution and is available at
 *
 * https://www.eclipse.org/legal/epl-v20.html
 */

import java.util.spi.ToolProvider;

import org.junit.platform.console.ConsoleLauncherToolProvider;

/**
 * Support for launching the JUnit Platform from the console.
 *
 * @since 1.0
 * @provides ToolProvider
 */
module org.junit.platform.console {

	requires static org.apiguardian.api;
	requires static transitive org.jspecify;

	requires org.junit.platform.commons;
	requires org.junit.platform.engine;
	requires org.junit.platform.launcher;
	requires org.junit.platform.reporting;

	exports org.junit.platform.console.output to org.junit.start;

	provides ToolProvider with ConsoleLauncherToolProvider;
}
