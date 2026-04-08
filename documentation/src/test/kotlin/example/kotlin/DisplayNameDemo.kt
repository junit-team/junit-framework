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

// tag::user_guide[]
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

@DisplayName("A special test case")
class DisplayNameDemo {
    @Test
    @DisplayName("Custom test name containing spaces")
    fun testWithDisplayNameContainingSpaces() {
    }

    @Test
    @DisplayName("╯°□°）╯")
    fun testWithDisplayNameContainingSpecialCharacters() {
    }

    @Test
    @DisplayName("😱")
    fun testWithDisplayNameContainingEmoji() {
    }
}
// end::user_guide[]
