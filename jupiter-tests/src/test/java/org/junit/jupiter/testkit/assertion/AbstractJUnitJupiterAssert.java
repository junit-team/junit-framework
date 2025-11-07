/*
 * Copyright 2015-2025 the original author or authors.
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v2.0 which
 * accompanies this distribution and is available at
 *
 * https://www.eclipse.org/legal/epl-v20.html
 */

package org.junit.jupiter.testkit.assertion;

import org.assertj.core.api.AbstractAssert;

/**
 * A very basic extension of the AbstractAssert, used to add a quantity to assertions.
 * By storing this value in a field we don't have to refer back to it every time.
 *
 * <p>Instead of <code>assertThat(results).hasTests().thatStarted(3).thenFailed(3)</code> we can write
 * <code>assertThat(results).hasNumberOfTests(3).thatStarted().andAllOfThemFailed()</code>.</p>
 *
 * @param <SELF> the "self" type of this assertion class. Please read
 *          &quot;<a href="https://web.archive.org/web/20130721224442/http:/passion.forco.de/content/emulating-self-types-using-java-generics-simplify-fluent-api-implementation" target="_blank">
 *          Emulating 'self types' using Java Generics to simplify fluent API implementation</a>&quot;
 *          for more details.
 * @param <ACTUAL> the type of the "actual" value.
 */
abstract class AbstractJUnitJupiterAssert<SELF extends AbstractAssert<SELF, ACTUAL>, ACTUAL>
		extends AbstractAssert<SELF, ACTUAL> {

	protected final int expected;

	protected AbstractJUnitJupiterAssert(ACTUAL actual, Class<?> self, int expected) {
		super(actual, self);
		this.expected = expected;
	}

}
