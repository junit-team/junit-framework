/*
 * Copyright 2015-2026 the original author or authors.
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v2.0 which
 * accompanies this distribution and is available at
 *
 * https://www.eclipse.org/legal/epl-v20.html
 */

package example.kotlin

// tag::user_guide[]
import example.util.Calculator
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class AssertJAssertionsDemo {
    private val calculator = Calculator()

    @Test
    fun assertWithAssertJ() {
        assertThat(calculator.subtract(4, 1)).isEqualTo(3)
    }
}
// end::user_guide[]
