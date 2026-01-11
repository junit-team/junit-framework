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

import static java.lang.Integer.MIN_VALUE;
import static java.util.Objects.requireNonNull;
import static org.junit.platform.launcher.tagexpression.Operator.nullaryOperator;
import static org.junit.platform.launcher.tagexpression.ParseStatus.emptyTagExpression;
import static org.junit.platform.launcher.tagexpression.ParseStatus.missingClosingParenthesis;
import static org.junit.platform.launcher.tagexpression.ParseStatus.missingOpeningParenthesis;
import static org.junit.platform.launcher.tagexpression.ParseStatus.success;
import static org.junit.platform.launcher.tagexpression.TagExpressions.any;
import static org.junit.platform.launcher.tagexpression.TagExpressions.none;
import static org.junit.platform.launcher.tagexpression.TagExpressions.tag;

import java.util.List;

/**
 * This is based on a modified version of the
 * <a href="https://en.wikipedia.org/wiki/Shunting-yard_algorithm">
 * Shunting-yard algorithm</a>.
 *
 * @since 1.1
 */
class ShuntingYard {

	private static final Operator RightParenthesis = nullaryOperator(")", -1);
	private static final Operator LeftParenthesis = nullaryOperator("(", -2);
	private static final Operator Sentinel = nullaryOperator("sentinel", MIN_VALUE);
	private static final Token SentinelToken = new Token(-1, "");

	private final Operators validOperators = new Operators();
	private final Stack<TokenWith<TagExpression>> expressions = new DequeStack<>();
	private final Stack<TokenWith<Operator>> operators = new DequeStack<>();
	private final List<Token> tokens;

	ShuntingYard(List<Token> tokens) {
		this.tokens = tokens;
		pushOperatorAt(SentinelToken, Sentinel);
	}

	public ParseResult execute() {
		// @formatter:off
		ParseStatus parseStatus = processTokens()
				.process(this::consumeRemainingOperators)
				.process(this::ensureOnlySingleExpressionRemains);
		// @formatter:on
		if (parseStatus.isError()) {
			return ParseResults.error(requireNonNull(parseStatus.errorMessage));
		}
		return ParseResults.success(expressions.pop().element());
	}

	private ParseStatus processTokens() {
		ParseStatus parseStatus = success();
		for (Token token : tokens) {
			parseStatus = parseStatus.process(() -> process(token));
		}
		return parseStatus;
	}

	private ParseStatus process(Token token) {
		String trimmed = token.string();
		if (LeftParenthesis.represents(trimmed)) {
			pushOperatorAt(token, LeftParenthesis);
			return success();
		}
		if (RightParenthesis.represents(trimmed)) {
			return findMatchingLeftParenthesis(token);
		}
		var operator = validOperators.operatorFor(trimmed);
		if (operator != null) {
			return findOperands(token, operator);
		}
		pushExpressionAt(token, convertLeafTokenToExpression(trimmed));
		return success();
	}

	private TagExpression convertLeafTokenToExpression(String trimmed) {
		if ("any()".equalsIgnoreCase(trimmed)) {
			return any();
		}
		if ("none()".equalsIgnoreCase(trimmed)) {
			return none();
		}
		return tag(trimmed);
	}

	private ParseStatus findMatchingLeftParenthesis(Token token) {
		while (!operators.isEmpty()) {
			TokenWith<Operator> tokenWithWithOperator = operators.pop();
			Operator operator = tokenWithWithOperator.element();
			if (LeftParenthesis.equals(operator)) {
				return success();
			}
			ParseStatus parseStatus = operator.createAndAddExpressionTo(expressions, tokenWithWithOperator.token());
			if (parseStatus.isError()) {
				return parseStatus;
			}
		}
		return missingOpeningParenthesis(token, RightParenthesis.representation());
	}

	private ParseStatus findOperands(Token token, Operator currentOperator) {
		while (currentOperator.hasLowerPrecedenceThan(previousOperator())
				|| (currentOperator.hasSamePrecedenceAs(previousOperator()) && currentOperator.isLeftAssociative())) {
			TokenWith<Operator> tokenWithWithOperator = operators.pop();
			ParseStatus parseStatus = tokenWithWithOperator.element().createAndAddExpressionTo(expressions,
				tokenWithWithOperator.token());
			if (parseStatus.isError()) {
				return parseStatus;
			}
		}
		pushOperatorAt(token, currentOperator);
		return success();
	}

	private Operator previousOperator() {
		return operators.peek().element();
	}

	private void pushExpressionAt(Token token, TagExpression tagExpression) {
		expressions.push(new TokenWith<>(token, tagExpression));
	}

	private void pushOperatorAt(Token token, Operator operator) {
		operators.push(new TokenWith<>(token, operator));
	}

	private ParseStatus consumeRemainingOperators() {
		while (!operators.isEmpty()) {
			TokenWith<Operator> tokenWithWithOperator = operators.pop();
			Operator operator = tokenWithWithOperator.element();
			if (LeftParenthesis.equals(operator)) {
				return missingClosingParenthesis(tokenWithWithOperator.token(), operator.representation());
			}
			ParseStatus parseStatus = operator.createAndAddExpressionTo(expressions, tokenWithWithOperator.token());
			if (parseStatus.isError()) {
				return parseStatus;
			}
		}
		return success();
	}

	private ParseStatus ensureOnlySingleExpressionRemains() {
		if (expressions.size() == 1) {
			return success();
		}
		if (expressions.isEmpty()) {
			return emptyTagExpression();
		}
		TokenWith<TagExpression> rhs = expressions.pop();
		TokenWith<TagExpression> lhs = expressions.pop();
		return ParseStatus.missingOperatorBetween(lhs, rhs);
	}

}
