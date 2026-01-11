/*
 * Copyright 2015-2026 the original author or authors.
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v2.0 which
 * accompanies this distribution and is available at
 *
 * https://www.eclipse.org/legal/epl-v20.html
 */

package standalone;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class JupiterParamsIntegration {

	@ParameterizedTest(name = "[{index}] argument={0}", quoteTextArguments = false)
	@ValueSource(strings = "test")
	void parameterizedTest(String argument) {
		assertEquals("test", argument);
	}
}
