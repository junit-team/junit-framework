/*
 * Copyright 2015-2026 the original author or authors.
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v2.0 which
 * accompanies this distribution and is available at
 *
 * https://www.eclipse.org/legal/epl-v20.html
 */

package org.junit.platform.commons.util;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

/**
 * Unit tests for {@link RuntimeUtils}.
 *
 * @since 1.6
 */
class RuntimeUtilsTests {

	@Test
	void jmxIsAvailableAndInputArgumentsAreReturned() {
		var optionalArguments = RuntimeUtils.getInputArguments();
		assertTrue(optionalArguments.isPresent(), "JMX not available or something else happened...");
		var arguments = optionalArguments.get();
		assertNotNull(arguments);
	}

}
