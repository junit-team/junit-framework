/*
 * Copyright 2015-2026 the original author or authors.
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v2.0 which
 * accompanies this distribution and is available at
 *
 * https://www.eclipse.org/legal/epl-v20.html
 */

package org.junit.jupiter.api.parallel;

import static org.apiguardian.api.API.Status.STABLE;

import org.apiguardian.api.API;

/**
 * The access mode required by a test class or method for a given resource.
 *
 * @since 5.3
 * @see ResourceLock
 * @see ResourceLocksProvider.Lock
 */
@API(status = STABLE, since = "5.10")
public enum ResourceAccessMode {

	/**
	 * Require read and write access to the resource.
	 */
	READ_WRITE,

	/**
	 * Require only read access to the resource.
	 */
	READ

}
