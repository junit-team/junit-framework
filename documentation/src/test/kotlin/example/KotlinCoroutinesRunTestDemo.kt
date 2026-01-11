/*
 * Copyright 2015-2026 the original author or authors.
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v2.0 which
 * accompanies this distribution and is available at
 *
 * https://www.eclipse.org/legal/epl-v20.html
 */

package example

// tag::user_guide[]
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test

class KotlinCoroutinesRunTestDemo {
    // end::user_guide[]
    // @formatter:off
    // tag::user_guide[]
    @Test
    fun coroutineTestUsingRunTest() = runTest {
        // ...
    }
    // end::user_guide[]
    // @formatter:on
    // tag::user_guide[]
}
// end::user_guide[]
