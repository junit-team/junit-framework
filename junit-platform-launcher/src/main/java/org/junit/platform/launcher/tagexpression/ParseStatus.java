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

import java.util.function.Supplier;

import org.jspecify.annotations.Nullable;

/**
 * @since 1.1
 */
class ParseStatus {

	static ParseStatus success() {
		return error(null);
	}

	static ParseStatus problemParsing(Token token, String representation) {
		return errorAt(token, representation, "problem parsing");
	}

	static ParseStatus missingOpeningParenthesis(Token token, String representation) {
		return errorAt(token, representation, "missing opening parenthesis");
	}

	static ParseStatus missingClosingParenthesis(Token token, String representation) {
		return errorAt(token, representation, "missing closing parenthesis");
	}

	static ParseStatus missingRhsOperand(Token token, String representation) {
		return errorAt(token, representation, "missing rhs operand");
	}

	static ParseStatus errorAt(Token token, String operatorRepresentation, String message) {
		return error(
			message + " for '" + operatorRepresentation + "' at index " + format(token.strippedTokenStartIndex()));
	}

	static ParseStatus missingOperatorBetween(TokenWith<TagExpression> lhs, TokenWith<TagExpression> rhs) {
		String lhsString = "'" + lhs.element() + "' at index " + format(lhs.token().lastCharacterIndex());
		String rhsString = "'" + rhs.element() + "' at index " + format(rhs.token().strippedTokenStartIndex());
		return error("missing operator between " + lhsString + " and " + rhsString);
	}

	static ParseStatus emptyTagExpression() {
		return error("empty tag expression");
	}

	private static String format(int indexInTagExpression) {
		return "<" + indexInTagExpression + ">";
	}

	private static ParseStatus error(@Nullable String errorMessage) {
		return new ParseStatus(errorMessage);
	}

	final @Nullable String errorMessage;

	private ParseStatus(@Nullable String errorMessage) {
		this.errorMessage = errorMessage;
	}

	public ParseStatus process(Supplier<ParseStatus> step) {
		if (isSuccess()) {
			return step.get();
		}
		return this;
	}

	public boolean isError() {
		return !isSuccess();
	}

	private boolean isSuccess() {
		return errorMessage == null;
	}

}
