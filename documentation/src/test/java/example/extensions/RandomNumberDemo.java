/*
 * Copyright 2015-2026 the original author or authors.
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v2.0 which
 * accompanies this distribution and is available at
 *
 * https://www.eclipse.org/legal/epl-v20.html
 */

package example.extensions;

import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

// tag::user_guide[]
class RandomNumberDemo {

	// Use static randomNumber0 field anywhere in the test class,
	// including @BeforeAll or @AfterEach lifecycle methods.
	@Random
	// end::user_guide[]
	@Nullable
	// tag::user_guide[]
	private static Integer randomNumber0;

	// Use randomNumber1 field in test methods and @BeforeEach
	// or @AfterEach lifecycle methods.
	@Random
	private int randomNumber1;

	RandomNumberDemo(@Random int randomNumber2) {
		// Use randomNumber2 in constructor.
	}

	@BeforeEach
	void beforeEach(@Random int randomNumber3) {
		// Use randomNumber3 in @BeforeEach method.
	}

	@Test
	void test(@Random int randomNumber4) {
		// Use randomNumber4 in test method.
	}

}
// end::user_guide[]
