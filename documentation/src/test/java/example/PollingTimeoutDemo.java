/*
 * Copyright 2015-2026 the original author or authors.
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v2.0 which
 * accompanies this distribution and is available at
 *
 * https://www.eclipse.org/legal/epl-v20.html
 */

package example;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

class PollingTimeoutDemo {

	// tag::user_guide[]
	@Test
	@Timeout(5) // Poll at most 5 seconds
	void pollUntil() throws InterruptedException {
		while (asynchronousResultNotAvailable()) {
			Thread.sleep(250); // custom poll interval
		}
		// Obtain the asynchronous result and perform assertions
	}
	// end::user_guide[]

	private boolean asynchronousResultNotAvailable() {
		return false;
	}

}
