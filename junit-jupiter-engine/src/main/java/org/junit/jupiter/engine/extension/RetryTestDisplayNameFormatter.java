/*
 * Copyright 2015-2026 the original author or authors.
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v2.0 which
 * accompanies this distribution and is available at
 *
 * https://www.eclipse.org/legal/epl-v20.html
 */

package org.junit.jupiter.engine.extension;

import static org.junit.jupiter.api.RetryTest.CURRENT_ATTEMPT_PLACEHOLDER;
import static org.junit.jupiter.api.RetryTest.DISPLAY_NAME_PLACEHOLDER;
import static org.junit.jupiter.api.RetryTest.MAX_ATTEMPTS_PLACEHOLDER;

import org.junit.jupiter.api.RetryTest;

/**
 * Display name formatter for a {@link RetryTest @RetryTest}.
 *
 * @since 6.1
 */
class RetryTestDisplayNameFormatter {

	private final String pattern;
	private final String displayName;

	RetryTestDisplayNameFormatter(String pattern, String displayName) {
		this.pattern = pattern;
		this.displayName = displayName;
	}

	String format(int currentAttempt, int maxAttempts) {
		return this.pattern//
				.replace(DISPLAY_NAME_PLACEHOLDER, this.displayName)//
				.replace(CURRENT_ATTEMPT_PLACEHOLDER, String.valueOf(currentAttempt))//
				.replace(MAX_ATTEMPTS_PLACEHOLDER, String.valueOf(maxAttempts));
	}

}
