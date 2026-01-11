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

import java.util.List;

import org.jspecify.annotations.Nullable;

/**
 * @since 1.1
 */
class Parser {

	private final Tokenizer tokenizer = new Tokenizer();

	ParseResult parse(@Nullable String infixTagExpression) {
		return constructExpressionFrom(tokensDerivedFrom(infixTagExpression));
	}

	private List<Token> tokensDerivedFrom(@Nullable String infixTagExpression) {
		return tokenizer.tokenize(infixTagExpression);
	}

	private ParseResult constructExpressionFrom(List<Token> tokens) {
		return new ShuntingYard(tokens).execute();
	}

}
