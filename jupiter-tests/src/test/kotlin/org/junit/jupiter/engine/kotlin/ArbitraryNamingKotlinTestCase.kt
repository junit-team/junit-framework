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

import org.junit.jupiter.api.Test

class ArbitraryNamingKotlinTestCase {
    companion object {
        @JvmField
        val METHOD_NAME = "\uD83E\uDD86 ~|~test with a really, (really) terrible name & that needs to be changed!~|~"
    }

    @Suppress("DANGEROUS_CHARACTERS")
    @Test
    fun `🦆 ~|~test with a really, (really) terrible name & that needs to be changed!~|~`() { }

    @Test
    fun `test name ends with parentheses()`() { }
}
