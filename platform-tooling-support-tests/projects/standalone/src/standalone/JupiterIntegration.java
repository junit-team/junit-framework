/*
 * Copyright 2015-2026 the original author or authors.
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v2.0 which
 * accompanies this distribution and is available at
 *
 * https://www.eclipse.org/legal/epl-v20.html
 */

package standalone;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

class JupiterIntegration {

	@Test
	void successful() {
		Runtime.getRuntime().addShutdownHook(new Thread(() -> {
			new other.OtherwiseNotReferencedClass();
		}));
	}

	@Test
	@Disabled("integration-test-disabled")
	void disabled() {
	}

	@Test
	void abort() {
		Assumptions.assumeTrue(false, "integration-test-abort");
	}

	@Test
	void fail() {
		Assertions.fail("integration-test-fail");
	}
}
