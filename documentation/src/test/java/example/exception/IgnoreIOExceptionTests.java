/*
 * Copyright 2015-2026 the original author or authors.
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v2.0 which
 * accompanies this distribution and is available at
 *
 * https://www.eclipse.org/legal/epl-v20.html
 */

package example.exception;

import java.io.IOException;

import extensions.ExpectToFail;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(IgnoreIOExceptionExtension.class)
class IgnoreIOExceptionTests {

	@Test
	void shouldSucceed() throws IOException {
		throw new IOException("any");
	}

	@Test
	@ExpectToFail
	void shouldFail() {
		throw new RuntimeException("any");
	}
}
