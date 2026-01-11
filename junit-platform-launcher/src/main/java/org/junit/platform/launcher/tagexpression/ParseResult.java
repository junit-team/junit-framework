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

import static org.apiguardian.api.API.Status.INTERNAL;

import java.util.Optional;
import java.util.function.Function;

import org.apiguardian.api.API;

/**
 * The result of attempting to parse a {@link TagExpression}.
 *
 * <p>An instance of this type either contains a successfully parsed
 * {@link TagExpression} or an <em>error message</em> describing the parse
 * error.
 *
 * @since 1.1
 * @see TagExpression#parseFrom(String)
 */
@API(status = INTERNAL, since = "1.1")
public interface ParseResult {

	/**
	 * Return the parsed {@link TagExpression} or throw an exception with the
	 * contained parse error.
	 *
	 * @param exceptionCreator will be called with the error message in case
	 * this parse result contains a parse error; never {@code null}.
	 */
	default TagExpression tagExpressionOrThrow(Function<String, RuntimeException> exceptionCreator) {
		if (errorMessage().isPresent()) {
			throw exceptionCreator.apply(errorMessage().get());
		}
		return tagExpression().orElseThrow();
	}

	/**
	 * Return the contained parse error message, if any.
	 */
	default Optional<String> errorMessage() {
		return Optional.empty();
	}

	/**
	 * Return the contained {@link TagExpression}, if any.
	 */
	default Optional<TagExpression> tagExpression() {
		return Optional.empty();
	}

}
