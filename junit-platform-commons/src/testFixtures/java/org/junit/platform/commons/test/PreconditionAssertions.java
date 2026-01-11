/*
 * Copyright 2015-2026 the original author or authors.
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v2.0 which
 * accompanies this distribution and is available at
 *
 * https://www.eclipse.org/legal/epl-v20.html
 */

package org.junit.platform.commons.test;

import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

import org.assertj.core.api.ThrowableAssert.ThrowingCallable;
import org.assertj.core.api.ThrowableAssertAlternative;
import org.junit.platform.commons.PreconditionViolationException;
import org.junit.platform.commons.util.Preconditions;

/**
 * Collection of assertions for working with {@link Preconditions}.
 *
 * @since 1.14
 */
public final class PreconditionAssertions {

	private PreconditionAssertions() {
		/* no-op */
	}

	public static void assertPreconditionViolationNotNullFor(String name, ThrowingCallable throwingCallable) {
		assertPreconditionViolationFor(throwingCallable).withMessage("%s must not be null", name);
	}

	public static void assertPreconditionViolationNotBlankFor(String name, ThrowingCallable throwingCallable) {
		assertPreconditionViolationFor(throwingCallable).withMessageContaining("%s must not be blank", name);
	}

	public static void assertPreconditionViolationNotEmptyFor(String name, ThrowingCallable throwingCallable) {
		assertPreconditionViolationFor(throwingCallable).withMessage("%s must not be empty", name);
	}

	public static void assertPreconditionViolationNotNullOrBlankFor(String name, ThrowingCallable throwingCallable) {
		assertPreconditionViolationFor(throwingCallable).withMessage("%s must not be null or blank", name);
	}

	public static void assertPreconditionViolationNotNullOrEmptyFor(String name, ThrowingCallable throwingCallable) {
		assertPreconditionViolationFor(throwingCallable).withMessage("%s must not be null or empty", name);
	}

	public static ThrowableAssertAlternative<PreconditionViolationException> assertPreconditionViolationFor(
			ThrowingCallable throwingCallable) {

		return assertThatExceptionOfType(PreconditionViolationException.class).isThrownBy(throwingCallable);
	}

}
