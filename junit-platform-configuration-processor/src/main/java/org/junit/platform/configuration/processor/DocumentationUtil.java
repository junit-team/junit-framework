/*
 * Copyright 2026 the original author or authors.
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v2.0 which
 * accompanies this distribution and is available at
 *
 * https://www.eclipse.org/legal/epl-v20.html
 */

package org.junit.platform.configuration.processor;

import java.util.regex.Pattern;

final class DocumentationUtil {

	private DocumentationUtil() {
		/* no-op */
	}

	static String extractFirstParagraph(String docComment) {
		// matches either a new paragraph, header or Javadoc tag without content (e.g. @see).
		var matcher = Pattern.compile("<p>|<h\\d>|[^{]@[a-z]+").matcher(docComment);
		var firstParagraph = !matcher.find() ? docComment : docComment.substring(0, matcher.start());
		return firstParagraph //
				// Replace newlines with space
				.replaceAll("[\n\r]", " ") //
				// Merge multiple spaces
				.replaceAll(" +", " ") //
				// Replace the `: {@value}` conventional syntax.
				.replaceAll(": \\{@value}\\.?", ".") //
				// Replace the `{@code example}` syntax.
				.replaceAll("\\{@code (.+?)}", "$1") //
				// Replace the `{@link(plain) reference}` syntax.
				.replaceAll("\\{@link(?:plain)? ([^ ]+?)}", "$1") //
				// Replace the `{@link(plain) reference plain}` syntax.
				.replaceAll("\\{@link(?:plain)? [^ ]+ (.+?)}", "$1") //
				.trim();
	}
}
