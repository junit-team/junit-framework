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

// tag::user_guide[]
import static org.assertj.core.api.Assertions.assertThat;

import example.util.Calculator;

import org.junit.jupiter.api.Test;

class AssertJAssertionsDemo {

	private final Calculator calculator = new Calculator();

	@Test
	void assertWithAssertJ() {
		assertThat(calculator.subtract(4, 1)).isEqualTo(3);
	}

}
// end::user_guide[]
