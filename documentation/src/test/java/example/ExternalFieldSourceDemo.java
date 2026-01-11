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

import java.util.List;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.FieldSource;

class ExternalFieldSourceDemo {

	// tag::external_field_FieldSource_example[]
	@ParameterizedTest
	@FieldSource("example.FruitUtils#tropicalFruits")
	void testWithExternalFieldSource(String tropicalFruit) {
		// test with tropicalFruit
	}
	// end::external_field_FieldSource_example[]
}

class FruitUtils {

	public static final List<String> tropicalFruits = List.of("pineapple", "kiwi");

}
