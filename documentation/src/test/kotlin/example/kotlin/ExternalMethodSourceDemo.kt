/*
 * Copyright 2015-2026 the original author or authors.
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v2.0 which
 * accompanies this distribution and is available at
 *
 * https://www.eclipse.org/legal/epl-v20.html
 */

// tag::external_MethodSource_example[]
package example.kotlin

import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource
import java.util.stream.Stream

class ExternalMethodSourceDemo {
    @ParameterizedTest
    @MethodSource("example.kotlin.StringsProviders#tinyStrings")
    fun testWithExternalMethodSource(tinyString: String) {
        // test with tiny string
    }
}

object StringsProviders {
    @JvmStatic
    fun tinyStrings(): Stream<String> = Stream.of(".", "oo", "OOO")
}
// end::external_MethodSource_example[]
