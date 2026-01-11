/*
 * Copyright 2015-2026 the original author or authors.
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v2.0 which
 * accompanies this distribution and is available at
 *
 * https://www.eclipse.org/legal/epl-v20.html
 */

package org.junit.jupiter.api;

import static java.util.concurrent.Executors.newSingleThreadExecutor;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.AssertionFailureBuilder.assertionFailure;
import static org.junit.jupiter.api.Assertions.assertLinesMatch;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;

import org.junit.platform.commons.PreconditionViolationException;
import org.opentest4j.AssertionFailedError;

class AssertionFailureBuilderTest {

	@Test
	void doesNotTrimByDefault() {
		var error = AssertionsFacade.fail();
		assertStackTraceMatch(error,
			"""
					\\Qorg.junit.jupiter.api.AssertionFailureBuilder.build(AssertionFailureBuilder.java:\\E.+
					\\Qorg.junit.jupiter.api.AssertionFailureBuilderTest$AssertionsFacade.fail(AssertionFailureBuilderTest.java:\\E.+
					\\Qorg.junit.jupiter.api.AssertionFailureBuilderTest.doesNotTrimByDefault(AssertionFailureBuilderTest.java:\\E.+
					>>>>
					""");
	}

	@Test
	void trimsUpToAssertionsFacade() {
		var error = AssertionsFacade.failWithTrimmedStacktrace(AssertionsFacade.class, 0);
		assertStackTraceMatch(error,
			"""
					\\Qorg.junit.jupiter.api.AssertionFailureBuilderTest.trimsUpToAssertionsFacade(AssertionFailureBuilderTest.java:\\E.+
					>>>>
					""");
	}

	@Test
	void trimsUpToAssertionsFacadeKeepingOne() {
		var error = AssertionsFacade.failWithTrimmedStacktrace(AssertionsFacade.class, 1);
		assertStackTraceMatch(error,
			"""
					\\Qorg.junit.jupiter.api.AssertionFailureBuilderTest$AssertionsFacade.failWithTrimmedStacktrace(AssertionFailureBuilderTest.java:\\E.+
					\\Qorg.junit.jupiter.api.AssertionFailureBuilderTest.trimsUpToAssertionsFacadeKeepingOne(AssertionFailureBuilderTest.java:\\E.+
					>>>>
					""");
	}

	@Test
	void trimsUpToAssertionFailureBuilder() {
		var error = AssertionsFacade.failWithTrimmedStacktrace(AssertionFailureBuilder.class, 0);
		assertStackTraceMatch(error,
			"""
					\\Qorg.junit.jupiter.api.AssertionFailureBuilderTest$AssertionsFacade.failWithTrimmedStacktrace(AssertionFailureBuilderTest.java:\\E.+
					\\Qorg.junit.jupiter.api.AssertionFailureBuilderTest.trimsUpToAssertionFailureBuilder(AssertionFailureBuilderTest.java:\\E.+
					>>>>
					""");
	}

	@Test
	void ignoresClassNotInStackTrace() {
		var error = AssertionsFacade.failWithTrimmedStacktrace(String.class, 0);
		assertStackTraceMatch(error,
			"""
					\\Qorg.junit.jupiter.api.AssertionFailureBuilder.build(AssertionFailureBuilder.java:\\E.+
					\\Qorg.junit.jupiter.api.AssertionFailureBuilderTest$AssertionsFacade.failWithTrimmedStacktrace(AssertionFailureBuilderTest.java:\\E.+
					\\Qorg.junit.jupiter.api.AssertionFailureBuilderTest.ignoresClassNotInStackTrace(AssertionFailureBuilderTest.java:\\E.+
					>>>>
					""");
	}

	@Test
	void canTrimToEmptyStacktrace() throws ExecutionException, InterruptedException {
		try (ExecutorService service = newSingleThreadExecutor()) {
			// Ensure that the stacktrace starts at Thread.
			var error = service.submit(() -> AssertionsFacade.failWithTrimmedStacktrace(Thread.class, 0)).get();
			assertThat(error.getStackTrace()).isEmpty();
		}
	}

	@Test
	void mustRetainNonNegativeNumberOfFrames() {
		var exception = assertThrows(PreconditionViolationException.class, //
			() -> assertionFailure().retainStackTraceElements(-1));
		assertThat(exception).hasMessage("retainStackTraceElements must have a non-negative value");
	}

	private static void assertStackTraceMatch(AssertionFailedError assertionFailedError, String expectedLines) {
		List<String> stackStraceAsLines = Arrays.stream(assertionFailedError.getStackTrace()) //
				.map(StackTraceElement::toString) //
				.toList();
		assertLinesMatch(expectedLines.lines().toList(), stackStraceAsLines);
	}

	static class AssertionsFacade {
		static AssertionFailedError fail() {
			return assertionFailure().build();
		}

		static AssertionFailedError failWithTrimmedStacktrace(Class<?> to, int retain) {
			return AssertionFailureBuilder.assertionFailure() //
					.trimStacktrace(to) //
					.retainStackTraceElements(retain) //
					.build();
		}

	}
}
