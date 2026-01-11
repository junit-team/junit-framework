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

import static java.util.Objects.requireNonNull;
import static org.junit.platform.launcher.tagexpression.Operator.Associativity.Left;
import static org.junit.platform.launcher.tagexpression.ParseStatus.missingOperatorBetween;
import static org.junit.platform.launcher.tagexpression.ParseStatus.missingRhsOperand;
import static org.junit.platform.launcher.tagexpression.ParseStatus.problemParsing;
import static org.junit.platform.launcher.tagexpression.ParseStatus.success;

import java.util.function.BiFunction;
import java.util.function.Function;

import org.jspecify.annotations.Nullable;

/**
 * @since 1.1
 */
class Operator {

	enum Associativity {
		Left, Right
	}

	interface TagExpressionCreator {
		ParseStatus createExpressionAndAddTo(Stack<TokenWith<TagExpression>> expressions, Token operatorToken);
	}

	static Operator nullaryOperator(String representation, int precedence) {
		return new Operator(representation, precedence, 0, null, (expressions, operatorToken) -> success());
	}

	@SuppressWarnings("SameParameterValue")
	static Operator unaryOperator(String representation, int precedence, Associativity associativity,
			Function<TagExpression, TagExpression> unaryExpression) {

		return new Operator(representation, precedence, 1, associativity, (expressions, operatorToken) -> {
			TokenWith<TagExpression> rhs = expressions.pop();
			if (operatorToken.isLeftOf(rhs.token())) {
				Token combinedToken = operatorToken.concatenate(rhs.token());
				expressions.push(new TokenWith<>(combinedToken, unaryExpression.apply(rhs.element())));
				return success();
			}
			return missingRhsOperand(operatorToken, representation);
		});
	}

	@SuppressWarnings("SameParameterValue")
	static Operator binaryOperator(String representation, int precedence, Associativity associativity,
			BiFunction<TagExpression, TagExpression, TagExpression> binaryExpression) {

		return new Operator(representation, precedence, 2, associativity, (expressions, operatorToken) -> {
			TokenWith<TagExpression> rhs = expressions.pop();
			TokenWith<TagExpression> lhs = expressions.pop();
			Token lhsToken = lhs.token();
			if (lhsToken.isLeftOf(operatorToken) && operatorToken.isLeftOf(rhs.token())) {
				Token combinedToken = lhsToken.concatenate(operatorToken).concatenate(rhs.token());
				expressions.push(new TokenWith<>(combinedToken, binaryExpression.apply(lhs.element(), rhs.element())));
				return success();
			}
			if (rhs.token().isLeftOf(operatorToken)) {
				return missingRhsOperand(operatorToken, representation);
			}
			if (operatorToken.isLeftOf(lhsToken)) {
				return missingOperatorBetween(lhs, rhs);
			}
			return problemParsing(operatorToken, representation);
		});
	}

	private final String representation;
	private final int precedence;
	private final int arity;

	private final @Nullable Associativity associativity;

	private final TagExpressionCreator tagExpressionCreator;

	private Operator(String representation, int precedence, int arity, @Nullable Associativity associativity,
			TagExpressionCreator tagExpressionCreator) {

		this.representation = representation;
		this.precedence = precedence;
		this.arity = arity;
		this.associativity = associativity;
		this.tagExpressionCreator = tagExpressionCreator;
	}

	boolean represents(String token) {
		return representation.equals(token);
	}

	String representation() {
		return representation;
	}

	boolean hasLowerPrecedenceThan(Operator operator) {
		return this.precedence < operator.precedence;
	}

	boolean hasSamePrecedenceAs(Operator operator) {
		return this.precedence == operator.precedence;
	}

	boolean isLeftAssociative() {
		return Left == associativity;
	}

	ParseStatus createAndAddExpressionTo(Stack<TokenWith<TagExpression>> expressions, Token operatorToken) {
		if (expressions.size() < arity) {
			String message = createMissingOperandMessage(expressions, operatorToken);
			return ParseStatus.errorAt(operatorToken, representation, message);
		}
		return tagExpressionCreator.createExpressionAndAddTo(expressions, operatorToken);
	}

	private String createMissingOperandMessage(Stack<TokenWith<TagExpression>> expressions, Token operatorToken) {
		if (1 == arity) {
			return missingOneOperand(associativity == Left ? "lhs" : "rhs");
		}

		if (2 == arity) {
			int mismatch = arity - expressions.size();
			if (2 == mismatch) {
				return "missing lhs and rhs operand";
			}
			return missingOneOperand(
				operatorToken.isLeftOf(requireNonNull(expressions.peek()).token()) ? "lhs" : "rhs");
		}
		return "missing operand";
	}

	private String missingOneOperand(String side) {
		return "missing " + side + " operand";
	}

}
