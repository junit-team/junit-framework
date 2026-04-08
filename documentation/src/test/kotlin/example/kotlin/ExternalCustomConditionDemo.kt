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

// tag::user_guide_external_custom_condition[]
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.condition.EnabledIf

class ExternalCustomConditionDemo {
    @Test
    @EnabledIf("example.kotlin.ExternalCondition#customCondition")
    fun enabled() {
        // ...
    }
}

class ExternalCondition {
    companion object {
        @JvmStatic
        fun customCondition(): Boolean = true
    }
}
// end::user_guide_external_custom_condition[]
