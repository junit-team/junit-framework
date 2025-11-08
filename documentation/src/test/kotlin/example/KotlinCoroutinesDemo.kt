/*
 * Copyright 2015-2025 the original author or authors.
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v2.0 which
 * accompanies this distribution and is available at
 *
 * https://www.eclipse.org/legal/epl-v20.html
 */
package example

// tag::user_guide[]
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

// end::user_guide[]
// @formatter:off
@Suppress("JUnitMalformedDeclaration")
// tag::user_guide[]
class KotlinCoroutinesDemo {
    // end::user_guide[]
    // @formatter:on
    // tag::user_guide[]
    @BeforeEach
    fun regularSetUp() {
    }

    @BeforeEach
    suspend fun coroutineSetUp() {
    }

    @Test
    fun regularTest() {
    }

    @Test
    suspend fun coroutineTest() {
    }
}
// end::user_guide[]
