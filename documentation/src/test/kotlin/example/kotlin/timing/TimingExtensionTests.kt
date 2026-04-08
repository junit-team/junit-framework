/*
 * Copyright 2015-2026 the original author or authors.
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v2.0 which
 * accompanies this distribution and is available at
 *
 * https://www.eclipse.org/legal/epl-v20.html
 */

package example.kotlin.timing

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith

// tag::user_guide[]
@ExtendWith(TimingExtension::class)
class TimingExtensionTests {
    @Test
    fun sleep20ms() {
        Thread.sleep(20)
    }

    @Test
    fun sleep50ms() {
        Thread.sleep(50)
    }
}
// end::user_guide[]
