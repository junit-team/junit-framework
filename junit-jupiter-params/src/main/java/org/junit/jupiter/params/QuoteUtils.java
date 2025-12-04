/*
 * Copyright 2015-2025 the original author or authors.
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v2.0 which
 * accompanies this distribution and is available at
 *
 * https://www.eclipse.org/legal/epl-v20.html
 */

package org.junit.jupiter.params;

/**
 * Collection of utilities for quoting text.
 *
 * @since 6.0
 */
final class QuoteUtils {

	private QuoteUtils() {
		/* no-op */
	}

	public static String quote(CharSequence text) {
		if (text.isEmpty()) {
			return "\"\"";
		}
		StringBuilder builder = new StringBuilder();
		builder.append('"');
		for (int i = 0; i < text.length(); i++) {
			builder.append(escape(text.charAt(i), true));
		}
		builder.append('"');
		return builder.toString();
	}

	public static String quote(char ch) {
		return '\'' + escape(ch, false) + '\'';
	}

	private static String escape(char ch, boolean withinString) {
		return switch (ch) {
			case '"' -> withinString ? "\\\"" : "\"";
			case '\'' -> withinString ? "'" : "\\'";
			case '\\' -> "\\\\";
			case '\b' -> "\\b";
			case '\f' -> "\\f";
			case '\t' -> "\\t";
			case '\r' -> "\\r";
			case '\n' -> "\\n";
			default -> String.valueOf(ch);
		};
	}

}
