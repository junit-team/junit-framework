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

import extensions.ExpectToFail
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrowsExactly

class ExceptionAssertionExactDemo {
    @ExpectToFail
    // tag::user_guide[]
    @Test
    fun testExpectedExceptionIsThrown() {
        // The following assertion succeeds because the code under assertion throws
        // IllegalArgumentException which is exactly equal to the expected type.
        // The assertion also returns the thrown exception which can be used for
        // further assertions like asserting the exception message.
        val exception =
            assertThrowsExactly<IllegalArgumentException> {
                throw IllegalArgumentException("expected message")
            }
        assertEquals("expected message", exception.message)

        // The following assertion fails because the assertion expects exactly
        // RuntimeException to be thrown, not subclasses of RuntimeException.
        assertThrowsExactly<RuntimeException> {
            throw IllegalArgumentException("expected message")
        }
    }
    // end::user_guide[]
}
