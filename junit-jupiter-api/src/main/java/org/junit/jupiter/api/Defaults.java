/*
 * Copyright 2026 the original author or authors.
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v2.0 which
 * accompanies this distribution and is available at
 *
 * https://www.eclipse.org/legal/epl-v20.html
 */

package org.junit.jupiter.api;

import static org.apiguardian.api.API.Status.INTERNAL;

import org.apiguardian.api.API;

/**
 * Defaults for configuration properties.
 */
@API(status = INTERNAL, since = "6.2")
public final class Defaults {

	/**
	 * Default value for {@value Constants#EXTENSIONS_AUTODETECTION_ENABLED_PROPERTY_NAME} is {@value}.
	 */
	public static final boolean EXTENSIONS_AUTODETECTION_ENABLED_DEFAULT = false;
	/**
	 * Default value for {@value Constants#CLOSING_STORED_AUTO_CLOSEABLE_ENABLED_PROPERTY_NAME} is {@value}.
	 */
	public static final boolean CLOSING_STORED_AUTO_CLOSEABLE_ENABLED_DEFAULT = true;
	/**
	 * Default value for {@value Constants#PARALLEL_EXECUTION_ENABLED_PROPERTY_NAME} is {@value}.
	 */
	public static final boolean PARALLEL_EXECUTION_ENABLED_DEFAULT = false;

	private Defaults() {
		/* no-op */
	}
}
