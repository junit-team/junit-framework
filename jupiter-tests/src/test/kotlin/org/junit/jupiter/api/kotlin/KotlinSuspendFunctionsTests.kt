/*
 * Copyright 2015-2025 the original author or authors.
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v2.0 which
 * accompanies this distribution and is available at
 *
 * https://www.eclipse.org/legal/epl-v20.html
 */
package org.junit.jupiter.api.kotlin

import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DynamicTest
import org.junit.jupiter.api.DynamicTest.dynamicTest
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestFactory
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.TestInstance.Lifecycle.PER_CLASS
import org.junit.jupiter.api.TestReporter
import org.junit.jupiter.engine.AbstractJupiterTestEngineTests
import org.junit.jupiter.params.AfterParameterizedClassInvocation
import org.junit.jupiter.params.BeforeParameterizedClassInvocation
import org.junit.jupiter.params.Parameter
import org.junit.jupiter.params.ParameterizedClass
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import org.junit.platform.engine.reporting.ReportEntry
import org.junit.platform.testkit.engine.EngineExecutionResults
import java.util.stream.Stream

class KotlinSuspendFunctionsTests : AbstractJupiterTestEngineTests() {
    @Test
    fun suspendingTestMethodsAreSupported() {
        val results = executeTestsForClass(TestMethodTestCase::class)
        assertAllTestsPassed(results, 1)
        assertThat(getPublishedEvents(results)).containsExactly("test")
    }

    @Test
    fun suspendingOpenTestMethodsAreSupported() {
        val results = executeTestsForClass(OpenTestMethodTestCase::class)
        assertAllTestsPassed(results, 1)
        assertThat(getPublishedEvents(results)).containsExactly("test")
    }

    @Test
    fun suspendingTestTemplateMethodsAreSupported() {
        val results = executeTestsForClass(TestTemplateTestCase::class)
        assertAllTestsPassed(results, 2)
        assertThat(getPublishedEvents(results)).containsExactly("foo", "bar")
    }

    @Test
    fun suspendingTestFactoryMethodsAreSupported() {
        val results = executeTestsForClass(TestFactoryTestCase::class)
        assertAllTestsPassed(results, 2)
        assertThat(getPublishedEvents(results)).containsExactly("test", "foo", "bar")
    }

    @Test
    fun suspendingLifecycleMethodsAreSupported() {
        val results = executeTestsForClass(LifecycleMethodsTestCase::class)
        assertAllTestsPassed(results, 1)
        assertThat(getPublishedEvents(results)).containsExactly("beforeAll", "beforeEach", "test", "afterEach", "afterAll")
    }

    @Test
    fun suspendingParameterizedLifecycleMethodsAreSupported() {
        val results = executeTestsForClass(ParameterizedLifecycleMethodsTestCase::class)
        assertAllTestsPassed(results, 2)
        assertThat(
            getPublishedEvents(results)
        ).containsExactly("beforeInvocation[1]", "test[1]", "afterInvocation[1]", "beforeInvocation[2]", "test[2]", "afterInvocation[2]")
    }

    private fun assertAllTestsPassed(
        results: EngineExecutionResults,
        numTests: Long
    ) {
        results.testEvents().assertStatistics {
            it.started(numTests).succeeded(numTests)
        }
    }

    private fun getPublishedEvents(results: EngineExecutionResults) =
        results
            .allEvents()
            .reportingEntryPublished() //
            .map { it.getRequiredPayload(ReportEntry::class.java) } //
            .map(ReportEntry::getKeyValuePairs)
            .map { it["value"] }

    @Suppress("JUnitMalformedDeclaration")
    class TestMethodTestCase {
        @Test
        suspend fun test(reporter: TestReporter) {
            suspendingPublish(reporter, "test")
        }
    }

    @Suppress("JUnitMalformedDeclaration")
    class TestTemplateTestCase {
        @ParameterizedTest
        @ValueSource(strings = ["foo", "bar"])
        suspend fun test(
            message: String,
            reporter: TestReporter
        ) {
            suspendingPublish(reporter, message)
        }
    }

    class TestFactoryTestCase {
        @TestFactory
        suspend fun test(reporter: TestReporter): Stream<DynamicTest> {
            suspendingPublish(reporter, "test")
            return Stream.of("foo", "bar").map {
                dynamicTest(it) {
                    runBlocking {
                        suspendingPublish(reporter, it)
                    }
                }
            }
        }
    }

    @Suppress("JUnitMalformedDeclaration")
    @TestInstance(PER_CLASS)
    class LifecycleMethodsTestCase {
        @BeforeAll
        suspend fun beforeAll(reporter: TestReporter) {
            suspendingPublish(reporter, "beforeAll")
        }

        @BeforeEach
        suspend fun beforeEach(reporter: TestReporter) {
            suspendingPublish(reporter, "beforeEach")
        }

        @Test
        suspend fun test(reporter: TestReporter) {
            suspendingPublish(reporter, "test")
        }

        @AfterEach
        suspend fun afterEach(reporter: TestReporter) {
            suspendingPublish(reporter, "afterEach")
        }

        @AfterAll
        suspend fun afterAll(reporter: TestReporter) {
            suspendingPublish(reporter, "afterAll")
        }
    }

    @Suppress("JUnitMalformedDeclaration", "RedundantSuspendModifier")
    @ParameterizedClass
    @ValueSource(ints = [1, 2])
    @TestInstance(PER_CLASS)
    class ParameterizedLifecycleMethodsTestCase {
        @BeforeParameterizedClassInvocation
        suspend fun beforeInvocation() {}

        @BeforeParameterizedClassInvocation
        suspend fun beforeInvocation(
            parameter: Int,
            reporter: TestReporter
        ) {
            suspendingPublish(reporter, "beforeInvocation[$parameter]")
        }

        @AfterParameterizedClassInvocation
        suspend fun afterInvocation() {}

        @AfterParameterizedClassInvocation
        suspend fun afterInvocation(
            parameter: Int,
            reporter: TestReporter
        ) {
            suspendingPublish(reporter, "afterInvocation[$parameter]")
        }

        @Parameter
        var parameter: Int = 0

        @Test
        suspend fun test(reporter: TestReporter) {
            suspendingPublish(reporter, "test[$parameter]")
        }
    }

    @Suppress("JUnitMalformedDeclaration")
    open class OpenTestMethodTestCase {
        @Test // https://github.com/junit-team/junit-framework/issues/5102
        open suspend fun `check getRandomPositiveInt`(reporter: TestReporter) {
            suspendingPublish(reporter, "test")
        }
    }

    @Suppress("RedundantSuspendModifier")
    companion object {
        suspend fun suspendingPublish(
            reporter: TestReporter,
            message: String
        ) {
            reporter.publishEntry(message)
        }
    }
}
