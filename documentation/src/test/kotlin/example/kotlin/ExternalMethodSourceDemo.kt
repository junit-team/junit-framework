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

// tag::external_MethodSource_example[]
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource

class ExternalMethodSourceDemo {
    @ParameterizedTest
    @MethodSource("example.StringsProviders#tinyStrings")
    fun testWithExternalMethodSource(tinyString: String) {
        // test with tiny string
    }
}
// end::external_MethodSource_example[]
