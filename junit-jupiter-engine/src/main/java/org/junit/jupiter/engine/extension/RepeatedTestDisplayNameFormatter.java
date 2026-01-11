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

import static org.junit.jupiter.api.RepeatedTest.CURRENT_REPETITION_PLACEHOLDER;
import static org.junit.jupiter.api.RepeatedTest.DISPLAY_NAME_PLACEHOLDER;
import static org.junit.jupiter.api.RepeatedTest.TOTAL_REPETITIONS_PLACEHOLDER;

import org.junit.jupiter.api.RepeatedTest;

/**
 * Display name formatter for a {@link RepeatedTest @RepeatedTest}.
 *
 * @since 5.0
 */
class RepeatedTestDisplayNameFormatter {

	private final String pattern;
	private final String displayName;

	RepeatedTestDisplayNameFormatter(String pattern, String displayName) {
		this.pattern = pattern;
		this.displayName = displayName;
	}

	String format(int currentRepetition, int totalRepetitions) {
		return this.pattern//
				.replace(DISPLAY_NAME_PLACEHOLDER, this.displayName)//
				.replace(CURRENT_REPETITION_PLACEHOLDER, String.valueOf(currentRepetition))//
				.replace(TOTAL_REPETITIONS_PLACEHOLDER, String.valueOf(totalRepetitions));
	}

}
