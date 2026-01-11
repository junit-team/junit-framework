/*
 * Copyright 2015-2026 the original author or authors.
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v2.0 which
 * accompanies this distribution and is available at
 *
 * https://www.eclipse.org/legal/epl-v20.html
 */

package example;

// tag::external_MethodSource_example[]
import java.util.stream.Stream;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

class ExternalMethodSourceDemo {

	@ParameterizedTest
	@MethodSource("example.StringsProviders#tinyStrings")
	void testWithExternalMethodSource(String tinyString) {
		// test with tiny string
	}
}

class StringsProviders {

	static Stream<String> tinyStrings() {
		return Stream.of(".", "oo", "OOO");
	}
}
// end::external_MethodSource_example[]
