/*
 * Copyright 2015-2026 the original author or authors.
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v2.0 which
 * accompanies this distribution and is available at
 *
 * https://www.eclipse.org/legal/epl-v20.html
 */

package example.kotlin.sharedresources

import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode.CONCURRENT
import org.junit.jupiter.api.parallel.ResourceAccessMode.READ
import org.junit.jupiter.api.parallel.ResourceAccessMode.READ_WRITE
import org.junit.jupiter.api.parallel.ResourceLock
import org.junit.jupiter.api.parallel.Resources.SYSTEM_PROPERTIES
import java.util.Properties

// tag::user_guide[]
@Execution(CONCURRENT)
class StaticSharedResourcesDemo {
    private lateinit var backup: Properties

    @BeforeEach
    fun backup() {
        backup = Properties()
        backup.putAll(System.getProperties())
    }

    @AfterEach
    fun restore() {
        System.setProperties(backup)
    }

    @Test
    @ResourceLock(value = SYSTEM_PROPERTIES, mode = READ)
    fun customPropertyIsNotSetByDefault() {
        assertNull(System.getProperty("my.prop"))
    }

    @Test
    @ResourceLock(value = SYSTEM_PROPERTIES, mode = READ_WRITE)
    fun canSetCustomPropertyToApple() {
        System.setProperty("my.prop", "apple")
        assertEquals("apple", System.getProperty("my.prop"))
    }

    @Test
    @ResourceLock(value = SYSTEM_PROPERTIES, mode = READ_WRITE)
    fun canSetCustomPropertyToBanana() {
        System.setProperty("my.prop", "banana")
        assertEquals("banana", System.getProperty("my.prop"))
    }
}
// end::user_guide[]
