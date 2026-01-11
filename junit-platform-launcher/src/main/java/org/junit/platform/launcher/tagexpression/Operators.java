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

import static java.util.function.Function.identity;
import static java.util.stream.Collectors.toMap;
import static org.junit.platform.launcher.tagexpression.Operator.Associativity.Left;
import static org.junit.platform.launcher.tagexpression.Operator.Associativity.Right;

import java.util.Map;
import java.util.stream.Stream;

import org.jspecify.annotations.Nullable;

/**
 * @since 1.1
 */
class Operators {

	private static final Operator Not = Operator.unaryOperator("!", 3, Right, TagExpressions::not);
	private static final Operator And = Operator.binaryOperator("&", 2, Left, TagExpressions::and);
	private static final Operator Or = Operator.binaryOperator("|", 1, Left, TagExpressions::or);

	private final Map<String, Operator> representationToOperator = Stream.of(Not, And, Or).collect(
		toMap(Operator::representation, identity()));

	@Nullable
	Operator operatorFor(String token) {
		return representationToOperator.get(token);
	}

}
