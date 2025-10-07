/*
 * Copyright 2015-2025 the original author or authors.
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v2.0 which
 * accompanies this distribution and is available at
 *
 * https://www.eclipse.org/legal/epl-v20.html
 */

package org.junit.platform.commons.util;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.platform.commons.test.PreconditionAssertions.assertPreconditionViolationFor;
import static org.junit.platform.commons.util.ExceptionUtils.findNestedThrowables;
import static org.junit.platform.commons.util.ExceptionUtils.pruneStackTrace;
import static org.junit.platform.commons.util.ExceptionUtils.readStackTrace;
import static org.junit.platform.commons.util.ExceptionUtils.throwAsUncheckedException;

import java.io.IOException;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.junit.platform.commons.JUnitException;

/**
 * Unit tests for {@link ExceptionUtils}.
 *
 * @since 1.0
 */
@SuppressWarnings("ThrowableNotThrown")
class ExceptionUtilsTests {

	@SuppressWarnings("DataFlowIssue")
	@Test
	void throwAsUncheckedExceptionWithNullException() {
		assertPreconditionViolationFor(() -> throwAsUncheckedException(null));
	}

	@Test
	void throwAsUncheckedExceptionWithCheckedException() {
		assertThrows(IOException.class, () -> throwAsUncheckedException(new IOException()));
	}

	@Test
	void throwAsUncheckedExceptionWithUncheckedException() {
		assertThrows(RuntimeException.class, () -> throwAsUncheckedException(new NumberFormatException()));
	}

	@SuppressWarnings("DataFlowIssue")
	@Test
	void readStackTraceForNullThrowable() {
		assertPreconditionViolationFor(() -> readStackTrace(null));
	}

	@Test
	void readStackTraceForLocalJUnitException() {
		try {
			throw new JUnitException("expected");
		}
		catch (JUnitException e) {
			// @formatter:off
			assertThat(readStackTrace(e))
				.startsWith(JUnitException.class.getName() + ": expected")
				.contains("at " + ExceptionUtilsTests.class.getName());
			// @formatter:on
		}
	}

	@ParameterizedTest
	@ValueSource(strings = { "org.junit.", "jdk.internal.reflect.", "sun.reflect." })
	void pruneStackTraceOfCallsFromSpecificPackage(String shouldBePruned) {
		JUnitException exception = new JUnitException("expected");

		pruneStackTrace(exception, List.of());

		assertThat(exception.getStackTrace()) //
				.noneMatch(element -> element.toString().contains(shouldBePruned));
	}

	@Test
	void pruneStackTraceOfAllLauncherCalls() {
		JUnitException exception = new JUnitException("expected");

		pruneStackTrace(exception, List.of());

		assertThat(exception.getStackTrace()) //
				.noneMatch(element -> element.toString().contains("org.junit.platform.launcher."));
	}

	@Test
	void pruneStackTraceOfEverythingPriorToFirstLauncherCall() {
		JUnitException exception = new JUnitException("expected");
		StackTraceElement[] stackTrace = exception.getStackTrace();
		stackTrace[stackTrace.length - 1] = new StackTraceElement("org.example.Class", "method", "file", 123);
		exception.setStackTrace(stackTrace);

		pruneStackTrace(exception, List.of());

		assertThat(exception.getStackTrace()) //
				.noneMatch(element -> element.toString().contains("org.example.Class.method(file:123)"));
	}

	@Test
	void findSuppressedExceptionsAndCausesOfThrowable() {
		Throwable t1 = new Throwable("#1");
		Throwable t2 = new Throwable("#2");
		Throwable t3 = new Throwable("#3");
		Throwable t4 = new Throwable("#4");
		Throwable t5 = new Throwable("#5");
		t1.initCause(t2);
		t2.initCause(t3);
		t1.addSuppressed(t4);
		t2.addSuppressed(t5);

		assertThat(findNestedThrowables(t1)) //
				.extracting(Throwable::getMessage) //
				.containsExactlyInAnyOrder("#1", "#2", "#3", "#4", "#5");
	}

	@Test
	void avoidCyclesWhileSearchingForNestedThrowables() {
		Throwable t1 = new Throwable();
		Throwable t2 = new Throwable(t1);
		Throwable t3 = new Throwable(t1);
		t1.initCause(t2);
		t1.addSuppressed(t3);
		t2.addSuppressed(t3);

		assertThat(findNestedThrowables(t1)).hasSize(3);
	}

}
