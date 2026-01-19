/*
 * Copyright 2015-2026 the original author or authors.
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v2.0 which
 * accompanies this distribution and is available at
 *
 * https://www.eclipse.org/legal/epl-v20.html
 */

package org.junit.platform.console.output;

import com.github.difflib.text.DiffRowGenerator;

import org.junit.platform.commons.util.ExceptionUtils;
import org.opentest4j.AssertionFailedError;

final class RichDiffFormatter {
	public String format(AssertionFailedError assertionFailed) {
		if (!(assertionFailed.isActualDefined() && assertionFailed.isExpectedDefined())) {
			return ExceptionUtils.readStackTrace(assertionFailed);
		}

		StringBuilder builder = new StringBuilder();

		builder.append(assertionFailed.getClass().getSimpleName());
		if (assertionFailed.isReasonDefined()) {
			builder.append(": ");
			builder.append(assertionFailed.getReason());
		}
		builder.append(System.lineSeparator());

		builder.append("+ actual - expected");
		builder.append(System.lineSeparator());

		var generator = DiffRowGenerator.create().mergeOriginalRevised(true).build();

		// TODO: But how to render the stacktrace?

		builder.append(generator.generateDiffRows(assertionFailed.getExpected().toString().lines().toList(),
			assertionFailed.getActual().toString().lines().toList()));

		return builder.toString();
	}
}
