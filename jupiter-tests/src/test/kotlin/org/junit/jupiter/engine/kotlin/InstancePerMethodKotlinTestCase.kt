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

class InstancePerMethodKotlinTestCase {
    companion object {
        @JvmField
        val TEST_INSTANCES: MutableMap<Any, MutableMap<String, Int>> = LinkedHashMap()

        @JvmStatic
        @BeforeAll
        fun beforeAll() {
            increment(this, "beforeAll")
        }

        @JvmStatic
        @AfterAll
        fun afterAll() {
            increment(this, "afterAll")
        }

        private fun increment(
            instance: Any,
            name: String
        ) {
            TEST_INSTANCES
                .computeIfAbsent(instance, { _ -> LinkedHashMap() })
                .compute(name, { _, oldValue -> (oldValue ?: 0) + 1 })
        }
    }

    @BeforeEach
    fun beforeEach() {
        increment(this, "beforeEach")
    }

    @AfterEach
    fun afterEach() {
        increment(this, "afterEach")
    }

    @Test
    fun firstTest() {
        increment(this, "test")
    }

    @Test
    fun secondTest() {
        increment(this, "test")
    }
}
