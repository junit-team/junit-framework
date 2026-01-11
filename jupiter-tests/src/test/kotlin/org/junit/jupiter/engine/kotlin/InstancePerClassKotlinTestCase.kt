/*
 * Copyright 2015-2026 the original author or authors.
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v2.0 which
 * accompanies this distribution and is available at
 *
 * https://www.eclipse.org/legal/epl-v20.html
 */

package org.junit.jupiter.engine.kotlin

import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.TestInstance.Lifecycle.PER_CLASS

@TestInstance(PER_CLASS)
class InstancePerClassKotlinTestCase {
    companion object {
        @JvmField
        val TEST_INSTANCES: MutableMap<Any, MutableMap<String, Int>> = HashMap()
    }

    @BeforeAll
    fun beforeAll() {
        increment("beforeAll")
    }

    @BeforeEach
    fun beforeEach() {
        increment("beforeEach")
    }

    @AfterEach
    fun afterEach() {
        increment("afterEach")
    }

    @AfterAll
    fun afterAll() {
        increment("afterAll")
    }

    @Test
    fun firstTest() {
        increment("test")
    }

    @Test
    fun secondTest() {
        increment("test")
    }

    private fun increment(name: String) {
        TEST_INSTANCES
            .computeIfAbsent(this, { _ -> HashMap() })
            .compute(name, { _, oldValue -> (oldValue ?: 0) + 1 })
    }
}
