/*
 * Copyright 2015-2025 the original author or authors.
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v2.0 which
 * accompanies this distribution and is available at
 *
 * https://www.eclipse.org/legal/epl-v20.html
 */
@file:API(status = STABLE, since = "5.7")

package org.junit.jupiter.api

import org.apiguardian.api.API
import org.apiguardian.api.API.Status.MAINTAINED
import org.apiguardian.api.API.Status.STABLE
import org.junit.jupiter.api.AssertionFailureBuilder.assertionFailure
import org.junit.jupiter.api.function.Executable
import org.junit.platform.commons.util.UnrecoverableExceptions.rethrowIfUnrecoverable
import java.time.Duration
import java.util.stream.Stream
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.InvocationKind.AT_MOST_ONCE
import kotlin.contracts.InvocationKind.EXACTLY_ONCE
import kotlin.contracts.contract

/**
 * @see Assertions.fail
 */
fun fail(
    message: String?,
    throwable: Throwable? = null
): Nothing = Assertions.fail<Nothing>(message, throwable)

/**
 * @see Assertions.fail
 */
@OptIn(ExperimentalContracts::class)
@API(status = MAINTAINED, since = "5.13.3")
@JvmName("fail_nonNullableLambda")
fun fail(message: () -> String): Nothing {
    contract {
        callsInPlace(message, EXACTLY_ONCE)
    }

    return Assertions.fail(message())
}

/**
 * @see Assertions.fail
 */
fun fail(message: (() -> String)?): Nothing = Assertions.fail<Nothing>(message)

/**
 * @see Assertions.fail
 */
fun fail(throwable: Throwable?): Nothing = Assertions.fail<Nothing>(throwable)

/**
 * [Stream] of functions to be executed.
 */
private typealias ExecutableStream = Stream<() -> Unit>

private fun ExecutableStream.convert() = map { Executable(it) }

/**
 * @see Assertions.assertAll
 */
fun assertAll(executables: ExecutableStream) = Assertions.assertAll(executables.convert())

/**
 * @see Assertions.assertAll
 */
fun assertAll(
    heading: String?,
    executables: ExecutableStream
) = Assertions.assertAll(heading, executables.convert())

/**
 * [Collection] of functions to be executed.
 */
private typealias ExecutableCollection = Collection<() -> Unit>

private fun ExecutableCollection.convert() = map { Executable(it) }

/**
 * @see Assertions.assertAll
 */
fun assertAll(executables: ExecutableCollection) = Assertions.assertAll(executables.convert())

/**
 * @see Assertions.assertAll
 */
fun assertAll(
    heading: String?,
    executables: ExecutableCollection
) = Assertions.assertAll(heading, executables.convert())

/**
 * @see Assertions.assertAll
 */
fun assertAll(vararg executables: () -> Unit) = assertAll(executables.toList().stream())

/**
 * @see Assertions.assertAll
 */
fun assertAll(
    heading: String?,
    vararg executables: () -> Unit
) = assertAll(heading, executables.toList().stream())

/**
 * Example usage:
 * ```kotlin
 * val nullableString: String? = ...
 *
 * assertNull(nullableString)
 *
 * // The compiler won't allow even safe calls, since nullableString is always null.
 * // nullableString?.isNotEmpty()
 * ```
 * @see Assertions.assertNull
 */
@OptIn(ExperimentalContracts::class)
@API(status = MAINTAINED, since = "5.13.3")
fun assertNull(actual: Any?) {
    contract {
        returns() implies (actual == null)
    }

    Assertions.assertNull(actual)
}

/**
 * Example usage:
 * ```kotlin
 * val nullableString: String? = ...
 *
 * assertNull(nullableString, "Should be nullable")
 *
 * // The compiler won't allow even safe calls, since nullableString is always null.
 * // nullableString?.isNotEmpty()
 * ```
 * @see Assertions.assertNull
 */
@OptIn(ExperimentalContracts::class)
@API(status = MAINTAINED, since = "5.13.3")
fun assertNull(
    actual: Any?,
    message: String
) {
    contract {
        returns() implies (actual == null)
    }

    Assertions.assertNull(actual, message)
}

/**
 * Example usage:
 * ```kotlin
 * val nullableString: String? = ...
 *
 * assertNull(nullableString) { "Should be nullable" }
 *
 * // The compiler won't allow even safe calls, since nullableString is always null.
 * // nullableString?.isNotEmpty()
 * ```
 * @see Assertions.assertNull
 */
@OptIn(ExperimentalContracts::class)
@API(status = MAINTAINED, since = "5.13.3")
fun assertNull(
    actual: Any?,
    messageSupplier: () -> String
) {
    contract {
        returns() implies (actual == null)

        callsInPlace(messageSupplier, AT_MOST_ONCE)
    }

    Assertions.assertNull(actual, messageSupplier)
}

/**
 * Example usage:
 * ```kotlin
 * val nullableString: String? = ...
 *
 * assertNotNull(nullableString)
 *
 * // The compiler smart casts nullableString to a non-nullable object.
 * assertTrue(nullableString.isNotEmpty())
 * ```
 * @see Assertions.assertNotNull
 */
@OptIn(ExperimentalContracts::class)
@API(status = MAINTAINED, since = "5.13.3")
fun assertNotNull(actual: Any?) {
    contract {
        returns() implies (actual != null)
    }

    Assertions.assertNotNull(actual)
}

/**
 * Example usage:
 * ```kotlin
 * val nullableString: String? = ...
 *
 * assertNotNull(nullableString, "Should be non-nullable")
 *
 * // The compiler smart casts nullableString to a non-nullable object.
 * assertTrue(nullableString.isNotEmpty())
 * ```
 * @see Assertions.assertNotNull
 */
@OptIn(ExperimentalContracts::class)
@API(status = MAINTAINED, since = "5.13.3")
fun assertNotNull(
    actual: Any?,
    message: String
) {
    contract {
        returns() implies (actual != null)
    }

    Assertions.assertNotNull(actual, message)
}

/**
 * Example usage:
 * ```kotlin
 * val nullableString: String? = ...
 *
 * assertNotNull(nullableString) { "Should be non-nullable" }
 *
 * // The compiler smart casts nullableString to a non-nullable object.
 * assertTrue(nullableString.isNotEmpty())
 * ```
 * @see Assertions.assertNotNull
 */
@OptIn(ExperimentalContracts::class)
@API(status = MAINTAINED, since = "5.13.3")
fun assertNotNull(
    actual: Any?,
    messageSupplier: () -> String
) {
    contract {
        returns() implies (actual != null)

        callsInPlace(messageSupplier, AT_MOST_ONCE)
    }

    Assertions.assertNotNull(actual, messageSupplier)
}

/**
 * Example usage:
 * ```kotlin
 * val exception = assertThrows<IllegalArgumentException> {
 *     throw IllegalArgumentException("Talk to a duck")
 * }
 * assertEquals("Talk to a duck", exception.message)
 * ```
 * @see Assertions.assertThrows
 */
inline fun <reified T : Throwable> assertThrows(executable: () -> Unit): T {
    // no contract for `executable` because it is expected to throw an exception instead
    // of being executed completely (see https://youtrack.jetbrains.com/issue/KT-27748)
    val throwable: Throwable? =
        try {
            executable()
        } catch (caught: Throwable) {
            caught
        } as? Throwable

    return Assertions.assertThrows(T::class.java) {
        if (throwable != null) {
            throw throwable
        }
    }
}

/**
 * Example usage:
 * ```kotlin
 * val exception = assertThrows<IllegalArgumentException>("Should throw an Exception") {
 *     throw IllegalArgumentException("Talk to a duck")
 * }
 * assertEquals("Talk to a duck", exception.message)
 * ```
 * @see Assertions.assertThrows
 */
inline fun <reified T : Throwable> assertThrows(
    message: String,
    executable: () -> Unit
): T = assertThrows({ message }, executable)

/**
 * Example usage:
 * ```kotlin
 * val exception = assertThrows<IllegalArgumentException>({ "Should throw an Exception" }) {
 *     throw IllegalArgumentException("Talk to a duck")
 * }
 * assertEquals("Talk to a duck", exception.message)
 * ```
 * @see Assertions.assertThrows
 */
@OptIn(ExperimentalContracts::class)
inline fun <reified T : Throwable> assertThrows(
    noinline message: () -> String,
    executable: () -> Unit
): T {
    contract {
        callsInPlace(message, AT_MOST_ONCE)
        // no contract for `executable` because it is expected to throw an exception instead
        // of being executed completely (see https://youtrack.jetbrains.com/issue/KT-27748)
    }

    val throwable: Throwable? =
        try {
            executable()
        } catch (caught: Throwable) {
            caught
        } as? Throwable

    return Assertions.assertThrows(
        T::class.java,
        {
            if (throwable != null) {
                throw throwable
            }
        },
        message
    )
}

/**
 * Example usage:
 * ```kotlin
 * val result = assertDoesNotThrow {
 *     // Code block that is expected to not throw an exception
 * }
 * ```
 * @see Assertions.assertDoesNotThrow
 * @param R the result type of the [executable]
 */
@OptIn(ExperimentalContracts::class)
@API(status = STABLE, since = "5.11")
inline fun <R> assertDoesNotThrow(executable: () -> R): R {
    contract {
        callsInPlace(executable, EXACTLY_ONCE)
    }

    try {
        return executable()
    } catch (t: Throwable) {
        rethrowIfUnrecoverable(t)
        val suffix = t.message?.let { if (it.isNotBlank()) ": ${t.message}" else null } ?: ""
        throw assertionFailure()
            .reason("Unexpected exception thrown: ${t.javaClass.getName()}$suffix")
            .cause(t)
            .build()
    }
}

/**
 * Example usage:
 * ```kotlin
 * val result = assertDoesNotThrow("Should not throw an exception") {
 *     // Code block that is expected to not throw an exception
 * }
 * ```
 * @see Assertions.assertDoesNotThrow
 * @param R the result type of the [executable]
 */
@OptIn(ExperimentalContracts::class)
@API(status = STABLE, since = "5.11")
inline fun <R> assertDoesNotThrow(
    message: String,
    executable: () -> R
): R {
    contract {
        callsInPlace(executable, EXACTLY_ONCE)
    }

    return assertDoesNotThrow({ message }, executable)
}

/**
 * Example usage:
 * ```kotlin
 * val result = assertDoesNotThrow({ "Should not throw an exception" }) {
 *     // Code block that is expected to not throw an exception
 * }
 * ```
 * @see Assertions.assertDoesNotThrow
 * @param R the result type of the [executable]
 */
@OptIn(ExperimentalContracts::class)
@API(status = STABLE, since = "5.11")
inline fun <R> assertDoesNotThrow(
    noinline message: () -> String,
    executable: () -> R
): R {
    contract {
        callsInPlace(executable, EXACTLY_ONCE)
        callsInPlace(message, AT_MOST_ONCE)
    }

    try {
        return executable()
    } catch (t: Throwable) {
        rethrowIfUnrecoverable(t)
        val suffix = t.message?.let { if (it.isNotBlank()) ": ${t.message}" else null } ?: ""
        throw assertionFailure()
            .message(message())
            .reason("Unexpected exception thrown: ${t.javaClass.getName()}$suffix")
            .cause(t)
            .build()
    }
}

/**
 * Example usage:
 * ```kotlin
 * val result = assertTimeout(Duration.seconds(1)) {
 *     // Code block that is being timed.
 * }
 * ```
 * @see Assertions.assertTimeout
 * @param R the result of the [executable].
 */
@OptIn(ExperimentalContracts::class)
@API(status = STABLE, since = "5.11")
fun <R> assertTimeout(
    timeout: Duration,
    executable: () -> R
): R {
    contract {
        callsInPlace(executable, AT_MOST_ONCE)
    }

    return Assertions.assertTimeout(timeout, executable)
}

/**
 * Example usage:
 * ```kotlin
 * val result = assertTimeout(Duration.seconds(1), "Should only take one second") {
 *     // Code block that is being timed.
 * }
 * ```
 * @see Assertions.assertTimeout
 * @param R the result of the [executable].
 */
@OptIn(ExperimentalContracts::class)
@API(status = STABLE, since = "5.11")
fun <R> assertTimeout(
    timeout: Duration,
    message: String,
    executable: () -> R
): R {
    contract {
        callsInPlace(executable, AT_MOST_ONCE)
    }

    return Assertions.assertTimeout(timeout, executable, message)
}

/**
 * Example usage:
 * ```kotlin
 * val result = assertTimeout(Duration.seconds(1), { "Should only take one second" }) {
 *     // Code block that is being timed.
 * }
 * ```
 * @see Assertions.assertTimeout
 * @param R the result of the [executable].
 */
@OptIn(ExperimentalContracts::class)
@API(status = STABLE, since = "5.11")
fun <R> assertTimeout(
    timeout: Duration,
    message: () -> String,
    executable: () -> R
): R {
    contract {
        callsInPlace(executable, AT_MOST_ONCE)
        callsInPlace(message, AT_MOST_ONCE)
    }

    return Assertions.assertTimeout(timeout, executable, message)
}

/**
 * Example usage:
 * ```kotlin
 * val result = assertTimeoutPreemptively(Duration.seconds(1)) {
 *     // Code block that is being timed.
 * }
 * ```
 * @see Assertions.assertTimeoutPreemptively
 * @param R the result of the [executable].
 */
@API(status = STABLE, since = "5.11")
fun <R> assertTimeoutPreemptively(
    timeout: Duration,
    executable: () -> R
): R =
    // no contract for `executable` because it might be interrupted and throw an exception
    // (see https://youtrack.jetbrains.com/issue/KT-27748)
    Assertions.assertTimeoutPreemptively(timeout, executable)

/**
 * Example usage:
 * ```kotlin
 * val result = assertTimeoutPreemptively(Duration.seconds(1), "Should only take one second") {
 *     // Code block that is being timed.
 * }
 * ```
 * @see Assertions.assertTimeoutPreemptively
 * @param R the result of the [executable].
 */
@API(status = STABLE, since = "5.11")
fun <R> assertTimeoutPreemptively(
    timeout: Duration,
    message: String,
    executable: () -> R
): R =
    // no contract for `executable` because it might be interrupted and throw an exception
    // (see https://youtrack.jetbrains.com/issue/KT-27748)
    Assertions.assertTimeoutPreemptively(timeout, executable, message)

/**
 * Example usage:
 * ```kotlin
 * val result = assertTimeoutPreemptively(Duration.seconds(1), { "Should only take one second" }) {
 *     // Code block that is being timed.
 * }
 * ```
 * @see Assertions.assertTimeoutPreemptively
 * @param R the result of the [executable].
 */
@OptIn(ExperimentalContracts::class)
@API(status = STABLE, since = "5.11")
fun <R> assertTimeoutPreemptively(
    timeout: Duration,
    message: () -> String,
    executable: () -> R
): R {
    contract {
        callsInPlace(message, AT_MOST_ONCE)
        // no contract for `executable` because it might be interrupted and throw an exception
        // (see https://youtrack.jetbrains.com/issue/KT-27748)
    }

    return Assertions.assertTimeoutPreemptively(timeout, executable, message)
}

/**
 * Example usage:
 * ```kotlin
 * val maybeString: Any = ...
 *
 * assertInstanceOf<String>(maybeString)
 *
 * // The compiler smart casts maybeString to a String object.
 * assertTrue(maybeString.isNotEmpty())
 * ```
 * @see Assertions.assertInstanceOf
 * @since 5.11
 */
@OptIn(ExperimentalContracts::class)
@API(status = MAINTAINED, since = "5.13.3")
inline fun <reified T : Any> assertInstanceOf(
    actualValue: Any?,
    message: String? = null
): T {
    contract {
        returns() implies (actualValue is T)
    }
    return Assertions.assertInstanceOf(T::class.java, actualValue, message)
}

/**
 * Example usage:
 * ```kotlin
 * val maybeString: Any = ...
 *
 * assertInstanceOf<String>(maybeString) { "Should be a string" }
 *
 * // The compiler smart casts maybeString to a String object.
 * assertTrue(maybeString.isNotEmpty())
 * ```
 * @see Assertions.assertInstanceOf
 * @since 5.11
 */
@OptIn(ExperimentalContracts::class)
@API(status = MAINTAINED, since = "5.13.3")
inline fun <reified T : Any> assertInstanceOf(
    actualValue: Any?,
    noinline message: () -> String
): T {
    contract {
        returns() implies (actualValue is T)

        callsInPlace(message, AT_MOST_ONCE)
    }
    return Assertions.assertInstanceOf(T::class.java, actualValue, message)
}
