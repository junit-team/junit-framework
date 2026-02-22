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

import java.util.function.Function;

import com.github.difflib.text.DiffRowGenerator;

import org.opentest4j.AssertionFailedError;

final class RichDiffFormatter {
	public String format(AssertionFailedError assertionFailed) {
		StringBuilder builder = new StringBuilder();

		if (assertionFailed.isReasonDefined()) {
			builder.append(": ");
			builder.append(assertionFailed.getReason());
		}
		builder.append(System.lineSeparator());

		builder.append("+ actual - expected");
		builder.append(System.lineSeparator());

		var generator = DiffRowGenerator.create() //
				.lineNormalizer(Function.identity()) // Don't normalize lines
				.showInlineDiffs(false) //
				.build();

		var diffRows = generator.generateDiffRows( //
			assertionFailed.getExpected().getStringRepresentation().lines().toList(), //
			assertionFailed.getActual().getStringRepresentation().lines().toList() //
		);

		diffRows.forEach(diffRow -> {
			switch (diffRow.getTag()) {
				case INSERT -> {
					builder.append("+ ");
					builder.append(diffRow.getNewLine());
					builder.append(System.lineSeparator());
				}
				case DELETE -> {
					builder.append("- ");
					builder.append(diffRow.getOldLine());
					builder.append(System.lineSeparator());
				}
				case CHANGE -> {
					builder.append("+ ");
					builder.append(diffRow.getOldLine());
					builder.append(System.lineSeparator());
					builder.append("- ");
					builder.append(diffRow.getNewLine());
					builder.append(System.lineSeparator());
				}
				case EQUAL -> {
					builder.append("  ");
					builder.append(diffRow.getNewLine());
					builder.append(System.lineSeparator());
				}
			}
		});

		return builder.toString();
	}
}
