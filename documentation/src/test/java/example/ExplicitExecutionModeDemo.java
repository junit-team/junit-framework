/*
 * Copyright 2015-2025 the original author or authors.
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v2.0 which
 * accompanies this distribution and is available at
 *
 * https://www.eclipse.org/legal/epl-v20.html
 */

package example;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;

//tag::user_guide[]
@Execution(ExecutionMode.CONCURRENT)
class ExplicitExecutionModeDemo {

	@Test
	void testA() {
		// concurrent
	}

	@Test
	@Execution(ExecutionMode.SAME_THREAD)
	void testB() {
		// overrides to same_thread
	}

}
//end::user_guide[]
