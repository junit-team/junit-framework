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

import java.util.Collection;

import org.apiguardian.api.API;
import org.jspecify.annotations.Nullable;
import org.junit.platform.engine.TestTag;

/**
 * A tag expression can be evaluated against a collection of
 * {@linkplain TestTag tags} to determine if they match the expression.
 *
 * @since 1.1
 */
@API(status = INTERNAL, since = "1.1")
public interface TagExpression {

	/**
	 * Attempt to parse a {@link TagExpression} from the supplied <em>tag
	 * expression string</em>.
	 *
	 * @param infixTagExpression the tag expression string to parse; may be
	 * {@code null}.
	 * @see ParseResult
	 */
	@API(status = INTERNAL, since = "1.1")
	static ParseResult parseFrom(@Nullable String infixTagExpression) {
		return new Parser().parse(infixTagExpression);
	}

	/**
	 * Evaluate this tag expression against the supplied collection of
	 * {@linkplain TestTag tags}.
	 *
	 * @param tags the tags this tag expression is to be evaluated against
	 * @return {@code true}, if the tags match this tag expression; {@code false}, otherwise
	 */
	boolean evaluate(Collection<TestTag> tags);

}
