/*
 * Copyright 2015-2026 the original author or authors.
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v2.0 which
 * accompanies this distribution and is available at
 *
 * https://www.eclipse.org/legal/epl-v20.html
 */

package example.kotlin.testinterface

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

// tag::user_guide[]
class TestInterfaceDemo :
    TestLifecycleLogger,
    TimeExecutionLogger,
    TestInterfaceDynamicTestsDemo {
    @Test
    fun isEqualValue() {
        assertEquals(1, "a".length, "is always equal")
    }
}
// end::user_guide[]
