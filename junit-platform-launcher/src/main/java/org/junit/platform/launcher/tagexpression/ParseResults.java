/*
 * Copyright 2015-2026 the original author or authors.
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v2.0 which
 * accompanies this distribution and is available at
 *
 * https://www.eclipse.org/legal/epl-v20.html
 */

package org.junit.platform.launcher.tagexpression;

import java.util.Optional;

/**
 * @since 1.1
 */
class ParseResults {

	private ParseResults() {
		/* no-op */
	}

	static ParseResult success(TagExpression tagExpression) {
		return new ParseResult() {
			@Override
			public Optional<TagExpression> tagExpression() {
				return Optional.of(tagExpression);
			}
		};
	}

	static ParseResult error(String errorMessage) {
		return new ParseResult() {
			@Override
			public Optional<String> errorMessage() {
				return Optional.of(errorMessage);
			}
		};
	}

}
