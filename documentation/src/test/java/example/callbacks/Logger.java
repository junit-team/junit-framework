/*
 * Copyright 2015-2026 the original author or authors.
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v2.0 which
 * accompanies this distribution and is available at
 *
 * https://www.eclipse.org/legal/epl-v20.html
 */

package example.callbacks;

import java.util.function.Supplier;

import org.junit.jupiter.api.extension.Extension;

class Logger {

	static final java.util.logging.Logger logger = java.util.logging.Logger.getLogger(Logger.class.getName());

	static void beforeAllMethod(String text) {
		log(() -> "@BeforeAll " + text);
	}

	static void beforeEachCallback(Extension extension) {
		log(() -> "  " + extension.getClass().getSimpleName() + ".beforeEach()");
	}

	static void beforeEachMethod(String text) {
		log(() -> "    @BeforeEach " + text);
	}

	static void testMethod(String text) {
		log(() -> "      @Test " + text);
	}

	static void afterEachMethod(String text) {
		log(() -> "    @AfterEach " + text);
	}

	static void afterEachCallback(Extension extension) {
		log(() -> "  " + extension.getClass().getSimpleName() + ".afterEach()");
	}

	static void afterAllMethod(String text) {
		log(() -> "@AfterAll " + text);
	}

	private static void log(Supplier<String> supplier) {
		// System.err.println(supplier.get());
		logger.info(supplier);
	}

}
