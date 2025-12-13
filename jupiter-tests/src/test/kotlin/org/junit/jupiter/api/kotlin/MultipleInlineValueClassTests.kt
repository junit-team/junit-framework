package org.junit.jupiter.api.kotlin

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource

class MultipleInlineValueClassTests {

    @MethodSource("mixedProvider")
    @ParameterizedTest
    fun testMultipleValueClasses(
        userId: UserId,
        email: Email,
        result: Result<String>
    ) {
        assertEquals(100L, userId.value)
        assertEquals("user@test.com", email.value)
        assertEquals("data", result.getOrThrow())
    }

    @MethodSource("normalAndValueClassProvider")
    @ParameterizedTest
    fun testMixedParameters(
        normalString: String,
        userId: UserId
    ) {
        assertEquals("normal", normalString)
        assertEquals(200L, userId.value)
    }

    companion object {
        @JvmStatic
        fun mixedProvider() = listOf(
            Arguments.of(
                UserId(100L),
                Email("user@test.com"),
                Result.success("data")
            )
        )

        @JvmStatic
        fun normalAndValueClassProvider() = listOf(
            Arguments.of("normal", UserId(200L))
        )
    }
}