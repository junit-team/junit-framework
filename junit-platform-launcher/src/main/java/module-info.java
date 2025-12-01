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
import org.junit.platform.launcher.LauncherDiscoveryListener;
import org.junit.platform.launcher.LauncherInterceptor;
import org.junit.platform.launcher.LauncherSessionListener;
import org.junit.platform.launcher.PostDiscoveryFilter;
import org.junit.platform.launcher.TestExecutionListener;
import org.junit.platform.launcher.listeners.UniqueIdTrackingListener;

/**
 * Public API for configuring and launching test plans.
 *
 * <p>This API is typically used by IDEs and build tools.
 *
 * @since 1.0
 * @uses TestEngine
 * @uses LauncherDiscoveryListener
 * @uses LauncherInterceptor
 * @uses LauncherSessionListener
 * @uses PostDiscoveryFilter
 * @uses TestExecutionListener
 */
module org.junit.platform.launcher {

	requires static transitive org.apiguardian.api;
	requires static transitive org.jspecify;
	requires static jdk.jfr;

	requires transitive java.logging;
	requires transitive org.junit.platform.commons;
	requires transitive org.junit.platform.engine;

	exports org.junit.platform.launcher;
	exports org.junit.platform.launcher.core;
	exports org.junit.platform.launcher.listeners;
	exports org.junit.platform.launcher.listeners.discovery;

	uses TestEngine;
	uses LauncherDiscoveryListener;
	uses LauncherInterceptor;
	uses LauncherSessionListener;
	uses PostDiscoveryFilter;
	uses TestExecutionListener;

	provides TestExecutionListener
			with UniqueIdTrackingListener;
}
