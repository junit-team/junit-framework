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

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode.CONCURRENT
import org.junit.jupiter.api.parallel.ResourceAccessMode.READ
import org.junit.jupiter.api.parallel.ResourceAccessMode.READ_WRITE
import org.junit.jupiter.api.parallel.ResourceLock
import org.junit.jupiter.api.parallel.ResourceLockTarget.CHILDREN

// tag::user_guide[]
@Execution(CONCURRENT)
@ResourceLock(value = "a", mode = READ, target = CHILDREN)
class ChildrenSharedResourcesDemo {
    @ResourceLock(value = "a", mode = READ_WRITE)
    @Test
    fun test1() {
        Thread.sleep(2000L)
    }

    @Test
    fun test2() {
        Thread.sleep(2000L)
    }

    @Test
    fun test3() {
        Thread.sleep(2000L)
    }

    @Test
    fun test4() {
        Thread.sleep(2000L)
    }

    @Test
    fun test5() {
        Thread.sleep(2000L)
    }
}
// end::user_guide[]
