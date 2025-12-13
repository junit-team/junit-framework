package org.junit.jupiter.api.kotlin

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.assertNotNull
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource

class NullableInlineValueClassTests {

    @MethodSource("nullableResultProvider")
    @ParameterizedTest
    fun testNullableResult(result: Result<String>?) {
        assertNotNull(result)
        assertEquals("test", result.getOrNull())
    }

    @MethodSource("nullableUserIdProvider")
    @ParameterizedTest
    fun testNullableUserId(userId: UserId?) {
        assertNotNull(userId)
        assertEquals(999L, userId.value)
    }

    companion object {
        @JvmStatic
        fun nullableResultProvider() =
            listOf(
                Arguments.of(Result.success("test"))
            )

        @JvmStatic
        fun nullableUserIdProvider() =
            listOf(
                Arguments.of(UserId(999L))
            )
    }
}