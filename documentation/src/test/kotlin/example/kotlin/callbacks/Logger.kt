/*
 * Copyright 2015-2026 the original author or authors.
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v2.0 which
 * accompanies this distribution and is available at
 *
 * https://www.eclipse.org/legal/epl-v20.html
 */

package example.kotlin.callbacks

import org.junit.jupiter.api.extension.Extension
import java.util.logging.Logger

private val logger = Logger.getLogger("example.kotlin.callbacks.Logger")

fun beforeAllMethod(text: String) = log { "@BeforeAll $text" }

fun beforeEachCallback(extension: Extension) = log { "  ${extension.javaClass.simpleName}.beforeEach()" }

fun beforeEachMethod(text: String) = log { "    @BeforeEach $text" }

fun testMethod(text: String) = log { "      @Test $text" }

fun afterEachMethod(text: String) = log { "    @AfterEach $text" }

fun afterEachCallback(extension: Extension) = log { "  ${extension.javaClass.simpleName}.afterEach()" }

fun afterAllMethod(text: String) = log { "@AfterAll $text" }

private fun log(supplier: () -> String) {
    logger.info(supplier)
}
