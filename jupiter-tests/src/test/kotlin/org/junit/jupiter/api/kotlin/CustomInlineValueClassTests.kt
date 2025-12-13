package org.junit.jupiter.api.kotlin

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource

/**
 * Tests for custom inline value classes.
 *
 * Currently, disabled: The POC only supports kotlin.Result.
 * Support for arbitrary inline value classes needs to be added.
 *
 * @see <a href="https://github.com/junit-team/junit-framework/issues/5081">Issue #5081</a>
 */
@Disabled("POC only supports kotlin.Result, not custom inline value classes")
class CustomInlineValueClassTests {

    @MethodSource("userIdProvider")
    @ParameterizedTest
    fun testUserId(userId: UserId) {
        assertEquals(123L, userId.value)
    }

    @MethodSource("emailProvider")
    @ParameterizedTest
    fun testEmail(email: Email) {
        assertEquals("test@example.com", email.value)
    }

    companion object {
        @JvmStatic
        fun userIdProvider() = listOf(Arguments.of(UserId(123L)))

        @JvmStatic
        fun emailProvider() = listOf(Arguments.of(Email("test@example.com")))
    }
}