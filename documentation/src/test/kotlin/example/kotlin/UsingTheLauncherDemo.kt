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

import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
import org.junit.platform.engine.CancellationToken
import org.junit.platform.engine.FilterResult
import org.junit.platform.engine.TestDescriptor
import org.junit.platform.engine.TestExecutionResult
import org.junit.platform.engine.TestExecutionResult.Status.FAILED
import org.junit.platform.engine.discovery.ClassNameFilter.includeClassNamePatterns
import org.junit.platform.engine.discovery.DiscoverySelectors.selectClass
import org.junit.platform.engine.discovery.DiscoverySelectors.selectPackage
import org.junit.platform.launcher.LauncherDiscoveryListener
import org.junit.platform.launcher.LauncherSessionListener
import org.junit.platform.launcher.PostDiscoveryFilter
import org.junit.platform.launcher.TestExecutionListener
import org.junit.platform.launcher.TestIdentifier
import org.junit.platform.launcher.core.LauncherConfig
import org.junit.platform.launcher.core.LauncherDiscoveryRequestBuilder
import org.junit.platform.launcher.core.LauncherExecutionRequestBuilder
import org.junit.platform.launcher.core.LauncherFactory
import org.junit.platform.launcher.listeners.SummaryGeneratingListener
import org.junit.platform.reporting.legacy.xml.LegacyXmlReportGeneratingListener
import java.io.PrintWriter
import java.nio.file.Path

/**
 * @since 5.0
 */
class UsingTheLauncherDemo {
    @Tag("exclude")
    @Test
    @Suppress("UNUSED_VARIABLE")
    fun execution() {
        // tag::execution[]
        val discoveryRequest =
            LauncherDiscoveryRequestBuilder
                .request()
                .selectors(
                    selectPackage("com.example.mytests"),
                    selectClass(MyTestClass::class.java)
                ).filters(
                    includeClassNamePatterns(".*Tests")
                )
                // end::execution[]
                .configurationParameter("enableHttpServer", "false")
                // tag::execution[]
                .build()

        val listener = SummaryGeneratingListener()

        LauncherFactory.openSession().use { session ->
            val launcher = session.launcher
            // Register one or more listeners of your choice.
            launcher.registerTestExecutionListeners(listener)
            // Discover tests and build a test plan.
            val testPlan = launcher.discover(discoveryRequest)
            // Execute the test plan.
            launcher.execute(testPlan)
            // Alternatively, execute the discovery request directly.
            launcher.execute(discoveryRequest)
        }

        val summary = listener.summary
        // Do something with the summary...

        // end::execution[]
    }

    @Test
    fun launcherConfig() {
        val reportsDir = Path.of("target", "xml-reports")
        val out = PrintWriter(System.out)
        // tag::launcherConfig[]
        val launcherConfig =
            LauncherConfig
                .builder()
                .enableTestEngineAutoRegistration(false)
                .enableLauncherSessionListenerAutoRegistration(false)
                .enableLauncherDiscoveryListenerAutoRegistration(false)
                .enablePostDiscoveryFilterAutoRegistration(false)
                .enableTestExecutionListenerAutoRegistration(false)
                .addTestEngines(CustomTestEngine())
                .addLauncherSessionListeners(CustomLauncherSessionListener())
                .addLauncherDiscoveryListeners(CustomLauncherDiscoveryListener())
                .addPostDiscoveryFilters(CustomPostDiscoveryFilter())
                .addTestExecutionListeners(LegacyXmlReportGeneratingListener(reportsDir, out))
                .addTestExecutionListeners(CustomTestExecutionListener())
                .build()

        val discoveryRequest =
            LauncherDiscoveryRequestBuilder
                .request()
                .selectors(selectPackage("com.example.mytests"))
                .build()

        LauncherFactory.openSession(launcherConfig).use { session ->
            session.launcher.execute(discoveryRequest)
        }
        // end::launcherConfig[]
    }

    @Test
    fun cancellationDirect() {
        // tag::cancellation-direct[]
        val cancellationToken = CancellationToken.create() // <1>

        val failFastListener =
            object : TestExecutionListener { // <2>
                override fun executionFinished(
                    identifier: TestIdentifier,
                    result: TestExecutionResult
                ) {
                    if (result.status == FAILED) {
                        cancellationToken.cancel()
                    }
                }
            }

        val executionRequest =
            LauncherDiscoveryRequestBuilder
                .request()
                .selectors(selectClass(MyTestClass::class.java))
                .forExecution()
                .cancellationToken(cancellationToken) // <3>
                .listeners(failFastListener) // <4>
                .build()

        LauncherFactory.openSession().use { session ->
            session.launcher.execute(executionRequest) // <5>
        }
        // end::cancellation-direct[]
    }

    @Test
    fun cancellationFromDiscoveryRequest() {
        val cancellationToken = CancellationToken.create()

        val failFastListener =
            object : TestExecutionListener {
                override fun executionFinished(
                    identifier: TestIdentifier,
                    result: TestExecutionResult
                ) {
                    if (result.status == FAILED) {
                        cancellationToken.cancel()
                    }
                }
            }

        // tag::cancellation-discovery-request[]
        val discoveryRequest =
            LauncherDiscoveryRequestBuilder
                .request()
                .selectors(selectClass(MyTestClass::class.java))
                .build() // <1>

        val executionRequest =
            LauncherExecutionRequestBuilder
                .request(discoveryRequest) // <2>
                .cancellationToken(cancellationToken) // <3>
                .listeners(failFastListener) // <4>
                .build()

        LauncherFactory.openSession().use { session ->
            session.launcher.execute(executionRequest) // <5>
        }
        // end::cancellation-discovery-request[]
    }

    @Test
    fun cancellationFromTestPlan() {
        val cancellationToken = CancellationToken.create()

        val failFastListener =
            object : TestExecutionListener {
                override fun executionFinished(
                    identifier: TestIdentifier,
                    result: TestExecutionResult
                ) {
                    if (result.status == FAILED) {
                        cancellationToken.cancel()
                    }
                }
            }

        // tag::cancellation-test-plan[]
        val discoveryRequest =
            LauncherDiscoveryRequestBuilder
                .request()
                .selectors(selectClass(MyTestClass::class.java))
                .build() // <1>

        LauncherFactory.openSession().use { session ->
            val launcher = session.launcher
            val testPlan = launcher.discover(discoveryRequest) // <2>
            val executionRequest =
                LauncherExecutionRequestBuilder
                    .request(testPlan) // <3>
                    .cancellationToken(cancellationToken) // <4>
                    .listeners(failFastListener) // <5>
                    .build()
            launcher.execute(executionRequest) // <6>
        }
        // end::cancellation-test-plan[]
    }
}

class MyTestClass

class CustomTestExecutionListener : TestExecutionListener

class CustomLauncherSessionListener : LauncherSessionListener

class CustomLauncherDiscoveryListener : LauncherDiscoveryListener

class CustomPostDiscoveryFilter : PostDiscoveryFilter {
    override fun apply(testDescriptor: TestDescriptor): FilterResult = FilterResult.included("includes everything")
}
