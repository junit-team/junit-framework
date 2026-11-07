/*
 * Copyright 2015-2026 the original author or authors.
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v2.0 which
 * accompanies this distribution and is available at
 *
 * https://www.eclipse.org/legal/epl-v20.html
 */

package org.junit.jupiter.params

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestReporter
import org.junit.jupiter.api.extension.ParameterResolutionException
import org.junit.jupiter.engine.AbstractJupiterTestEngineTests
import org.junit.jupiter.params.provider.Arguments.arguments
import org.junit.jupiter.params.provider.CsvSource
import org.junit.jupiter.params.provider.MethodSource
import org.junit.platform.engine.reporting.ReportEntry
import org.junit.platform.testkit.engine.EngineExecutionResults
import org.junit.platform.testkit.engine.EventConditions.finishedWithFailure
import org.junit.platform.testkit.engine.TestExecutionResultConditions.instanceOf
import org.junit.platform.testkit.engine.TestExecutionResultConditions.message

/**
 * Integration tests for Kotlin `@JvmInline` value classes whose underlying
 * type is not generic and therefore erased to a concrete JVM type, e.g.
 * `String` or `long`, in the signature of a `@ParameterizedTest` method.
 *
 * @see <a href="https://github.com/junit-team/junit-framework/issues/6109">#6109</a>
 */
class ParameterizedTestKotlinValueClassIntegrationTests : AbstractJupiterTestEngineTests() {
    @Test
    fun `given String-backed value class arguments when the parameter declares the value class then the test receives it`() {
        val results = executeTestsForClass(StringBackedValueClassTestCase::class)

        assertAllTestsPassed(results, 2)
        assertThat(getPublishedValues(results)).containsExactly("1.9", "2.0")
    }

    @Test
    fun `given primitive-backed value class arguments when the parameter declares the value class then the test receives it`() {
        val results = executeTestsForClass(PrimitiveBackedValueClassTestCase::class)

        assertAllTestsPassed(results, 2)
        assertThat(getPublishedValues(results)).containsExactly("UserId(value=42)", "42")
    }

    @Test
    fun `given value class and regular arguments when mixed in one signature then all are resolved`() {
        val results = executeTestsForClass(MixedArgumentsTestCase::class)

        assertAllTestsPassed(results, 1)
        assertThat(getPublishedValues(results)).containsExactly("1.9|kotlin|UserId(value=7)|3")
    }

    @Test
    fun `given a nullable value class parameter when null or a value is provided then both are accepted`() {
        val results = executeTestsForClass(NullableValueClassTestCase::class)

        assertAllTestsPassed(results, 2)
    }

    @Test
    fun `given the underlying value as argument when the parameter declares the value class then it keeps working`() {
        val results = executeTestsForClass(UnderlyingValueArgumentsTestCase::class)

        assertAllTestsPassed(results, 2)
        assertThat(getPublishedValues(results)).containsExactly("1.9", "UserId(value=42)")
    }

    @Test
    fun `given a value class argument when its underlying type does not match the parameter type then conversion fails`() {
        val results = executeTestsForClass(MismatchingUnderlyingTypeTestCase::class)

        results.testEvents().assertStatistics { it.started(1).failed(1) }
        results.testEvents().failed().assertThatEvents().haveExactly(
            1,
            finishedWithFailure(
                instanceOf(ParameterResolutionException::class.java),
                message {
                    it.contains("Error converting parameter at index 0") &&
                        it.contains("No built-in converter for source type ${LanguageVersion::class.java.name}")
                }
            )
        )
    }

    @Test
    fun `given a value class argument when the parameter type is a supertype of its underlying type then it is not unboxed`() {
        val results = executeTestsForClass(SupertypeOfUnderlyingTypeTestCase::class)

        results.testEvents().assertStatistics { it.started(1).failed(1) }
        results.testEvents().failed().assertThatEvents().haveExactly(
            1,
            finishedWithFailure(
                instanceOf(ParameterResolutionException::class.java),
                message {
                    it.contains("No built-in converter for source type ${LanguageVersion::class.java.name}") &&
                        it.contains("target type java.lang.CharSequence")
                }
            )
        )
    }

    private fun assertAllTestsPassed(
        results: EngineExecutionResults,
        numTests: Long
    ) {
        results.testEvents().assertStatistics {
            it.started(numTests).succeeded(numTests)
        }
    }

    private fun getPublishedValues(results: EngineExecutionResults) =
        results
            .allEvents()
            .reportingEntryPublished()
            .map { it.getRequiredPayload(ReportEntry::class.java) }
            .map(ReportEntry::getKeyValuePairs)
            .map { it["value"] }

    @Suppress("JUnitMalformedDeclaration")
    class StringBackedValueClassTestCase {
        @ParameterizedTest
        @MethodSource("languageVersions")
        fun test(
            languageVersion: LanguageVersion,
            reporter: TestReporter
        ) {
            reporter.publishEntry(languageVersion.version)
        }

        companion object {
            @JvmStatic
            fun languageVersions() = listOf(LanguageVersion("1.9"), LanguageVersion("2.0"))
        }
    }

    @Suppress("JUnitMalformedDeclaration")
    class PrimitiveBackedValueClassTestCase {
        @ParameterizedTest
        @MethodSource("userIds")
        fun test(
            userId: UserId,
            reporter: TestReporter
        ) {
            reporter.publishEntry(userId.toString())
        }

        @ParameterizedTest
        @MethodSource("unsignedInts")
        fun test(
            value: UInt,
            reporter: TestReporter
        ) {
            reporter.publishEntry(value.toString())
        }

        companion object {
            @JvmStatic
            fun userIds() = listOf(UserId(42))

            @JvmStatic
            fun unsignedInts() = listOf(42u)
        }
    }

    @Suppress("JUnitMalformedDeclaration")
    class MixedArgumentsTestCase {
        @ParameterizedTest
        @MethodSource("arguments")
        fun test(
            languageVersion: LanguageVersion,
            language: String,
            userId: UserId,
            count: Int,
            reporter: TestReporter
        ) {
            reporter.publishEntry("${languageVersion.version}|$language|$userId|$count")
        }

        companion object {
            @JvmStatic
            fun arguments() = listOf(arguments(LanguageVersion("1.9"), "kotlin", UserId(7), 3))
        }
    }

    @Suppress("JUnitMalformedDeclaration")
    class NullableValueClassTestCase {
        @ParameterizedTest
        @MethodSource("languageVersions")
        fun test(languageVersion: LanguageVersion?) {
            if (languageVersion == null) {
                assertNull(languageVersion)
            } else {
                assertEquals("2.0", languageVersion.version)
            }
        }

        companion object {
            @JvmStatic
            fun languageVersions() = listOf(arguments(null), arguments(LanguageVersion("2.0")))
        }
    }

    @Suppress("JUnitMalformedDeclaration")
    class UnderlyingValueArgumentsTestCase {
        @ParameterizedTest
        @CsvSource("1.9")
        fun test(
            languageVersion: LanguageVersion,
            reporter: TestReporter
        ) {
            reporter.publishEntry(languageVersion.version)
        }

        @ParameterizedTest
        @CsvSource("42")
        fun test(
            userId: UserId,
            reporter: TestReporter
        ) {
            reporter.publishEntry(userId.toString())
        }
    }

    @Suppress("JUnitMalformedDeclaration")
    class MismatchingUnderlyingTypeTestCase {
        @ParameterizedTest
        @MethodSource("languageVersions")
        fun test(count: Int) {
            error("should not be called with $count")
        }

        companion object {
            @JvmStatic
            fun languageVersions() = listOf(LanguageVersion("1.9"))
        }
    }

    @Suppress("JUnitMalformedDeclaration")
    class SupertypeOfUnderlyingTypeTestCase {
        @ParameterizedTest
        @MethodSource("languageVersions")
        fun test(version: CharSequence) {
            error("should not be called with $version")
        }

        companion object {
            @JvmStatic
            fun languageVersions() = listOf(LanguageVersion("1.9"))
        }
    }

    @JvmInline
    value class LanguageVersion(
        val version: String
    )

    @JvmInline
    value class UserId(
        val value: Long
    )
}
