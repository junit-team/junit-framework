/*
 * Copyright 2015-2026 the original author or authors.
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v2.0 which
 * accompanies this distribution and is available at
 *
 * https://www.eclipse.org/legal/epl-v20.html
 */

package org.junit.platform.launcher.core;

import static org.apiguardian.api.API.Status.INTERNAL;

import java.util.ServiceLoader;

import org.apiguardian.api.API;
import org.junit.platform.commons.logging.Logger;
import org.junit.platform.commons.logging.LoggerFactory;
import org.junit.platform.commons.util.ClassLoaderUtils;
import org.junit.platform.engine.TestEngine;

/**
 * @since 1.0
 */
@API(status = INTERNAL, since = "1.0", consumers = "org.junit.platform.suite.engine")
public final class ServiceLoaderTestEngineRegistry {

	public ServiceLoaderTestEngineRegistry() {
	}

	public Iterable<TestEngine> loadTestEngines() {
		Iterable<TestEngine> testEngines = ServiceLoader.load(TestEngine.class,
			ClassLoaderUtils.getDefaultClassLoader());
		getLogger().config(() -> TestEngineFormatter.format("Discovered TestEngines", testEngines));
		return testEngines;
	}

	private static Logger getLogger() {
		// Not a constant to avoid problems with building GraalVM native images
		return LoggerFactory.getLogger(ServiceLoaderTestEngineRegistry.class);
	}

}
