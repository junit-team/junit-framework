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

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.params.provider.Arguments.arguments;

import java.util.stream.Stream;

import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

class ParserErrorTests {

	private final Parser parser = new Parser();

	@Test
	void emptyExpression() {
		assertThat(parseErrorFromParsing("")).contains("empty tag expression");
	}

	@Test
	void missingClosingParenthesis() {
		assertThat(parseErrorFromParsing("(")).contains("missing closing parenthesis for '(' at index <0>");
		assertThat(parseErrorFromParsing("( foo & bar")).contains("missing closing parenthesis for '(' at index <0>");
	}

	@Test
	void missingOpeningParenthesis() {
		assertThat(parseErrorFromParsing(")")).contains("missing opening parenthesis for ')' at index <0>");
		assertThat(parseErrorFromParsing(" foo | bar)")).contains("missing opening parenthesis for ')' at index <10>");
	}

	@Test
	void partialUnaryOperator() {
		assertThat(parseErrorFromParsing("!")).contains("missing rhs operand for '!' at index <0>");
	}

	@Test
	void partialBinaryOperator() {
		assertThat(parseErrorFromParsing("& foo")).contains("missing lhs operand for '&' at index <0>");
		assertThat(parseErrorFromParsing("foo |")).contains("missing rhs operand for '|' at index <4>");
	}

	@ParameterizedTest
	@MethodSource("data")
	void acceptanceTests(String tagExpression, String parseError) {
		assertThat(parseErrorFromParsing(tagExpression)).contains(parseError);
	}

	@SuppressWarnings("unused")
	private static Stream<Arguments> data() {
		// @formatter:off
		return Stream.of(
				arguments("&", "missing lhs and rhs operand for '&' at index <0>"),
				arguments("|", "missing lhs and rhs operand for '|' at index <0>"),
				arguments("| |", "missing lhs and rhs operand for '|' at index <0>"),
				arguments("!", "missing rhs operand for '!' at index <0>"),
				arguments("foo bar", "missing operator between 'foo' at index <2> and 'bar' at index <4>"),
				arguments("foo bar |", "missing rhs operand for '|' at index <8>"),
				arguments("foo bar | baz", "missing operator between 'foo' at index <2> and '(bar | baz)' at index <4>"),
				arguments("foo bar &", "missing rhs operand for '&' at index <8>"),
				arguments("foo & (bar !)", "missing rhs operand for '!' at index <11>"),
				arguments("( foo & bar ) )", "missing opening parenthesis for ')' at index <14>"),
				arguments("( ( foo & bar )", "missing closing parenthesis for '(' at index <0>"),

				arguments("foo & (bar baz) |", "missing operator between 'bar' at index <9> and 'baz' at index <11>"),

				arguments("foo & (bar baz) &", "missing operator between 'bar' at index <9> and 'baz' at index <11>"),
				arguments("foo & (bar |baz) &", "missing rhs operand for '&' at index <17>"),

				arguments("foo | (bar baz) &", "missing rhs operand for '&' at index <16>"),
				arguments("foo | (bar baz) &quux", "missing operator between 'bar' at index <9> and '(baz & quux)' at index <11>"),

				arguments("foo & |", "missing rhs operand for '&' at index <4>"),
				arguments("foo !& bar", "missing rhs operand for '!' at index <4>"),
				arguments("foo !| bar", "missing rhs operand for '!' at index <4>")
		);
		// @formatter:on
	}

	private @Nullable String parseErrorFromParsing(String tagExpression) {
		try {
			var parseResult = parser.parse(tagExpression);
			parseResult.tagExpressionOrThrow(RuntimeException::new);
			return null;
		}
		catch (RuntimeException ex) {
			return ex.getMessage();
		}
	}
}
