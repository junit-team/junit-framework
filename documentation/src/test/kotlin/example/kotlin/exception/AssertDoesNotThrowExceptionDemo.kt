/*
 * Copyright 2015-2026 the original author or authors.
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v2.0 which
 * accompanies this distribution and is available at
 *
 * https://www.eclipse.org/legal/epl-v20.html
 */

package example.kotlin.exception

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow

class AssertDoesNotThrowExceptionDemo {
    // tag::user_guide[]
    @Test
    fun testExceptionIsNotThrown() {
        assertDoesNotThrow {
            shouldNotThrowException()
        }
    }

    fun shouldNotThrowException() {
    }
    // end::user_guide[]
}
