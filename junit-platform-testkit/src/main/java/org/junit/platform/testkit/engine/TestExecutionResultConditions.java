/*
 * Copyright 2015-2026 the original author or authors.
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v2.0 which
 * accompanies this distribution and is available at
 *
 * https://www.eclipse.org/legal/epl-v20.html
 */

package org.junit.platform.testkit.engine;

import static java.util.function.Predicate.isEqual;
import static org.apiguardian.api.API.Status.MAINTAINED;
import static org.junit.platform.commons.util.FunctionUtils.where;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Predicate;

import org.apiguardian.api.API;
import org.assertj.core.api.Assertions;
import org.assertj.core.api.Condition;
import org.jspecify.annotations.Nullable;
import org.junit.platform.commons.util.FunctionUtils;
import org.junit.platform.commons.util.Preconditions;
import org.junit.platform.engine.TestExecutionResult;
import org.junit.platform.engine.TestExecutionResult.Status;

/**
 * Collection of AssertJ {@linkplain Condition conditions} for
 * {@link TestExecutionResult}.
 *
 * @since 1.4
 * @see EventConditions
 */
@API(status = MAINTAINED, since = "1.7")
public final class TestExecutionResultConditions {

	private TestExecutionResultConditions() {
		/* no-op */
	}

	/**
	 * Create a new {@link Condition} that matches if and only if a
	 * {@link TestExecutionResult}'s {@linkplain TestExecutionResult#getStatus()
	 * status} is equal to the supplied {@link Status Status}.
	 */
	public static Condition<TestExecutionResult> status(Status expectedStatus) {
		return new Condition<>(where(TestExecutionResult::getStatus, isEqual(expectedStatus)), "status is %s",
			expectedStatus);
	}

	/**
	 * Create a new {@link Condition} that matches if and only if a
	 * {@link TestExecutionResult}'s
	 * {@linkplain TestExecutionResult#getThrowable() throwable} matches all
	 * supplied conditions.
	 */
	@SafeVarargs
	@SuppressWarnings("varargs")
	public static Condition<TestExecutionResult> throwable(Condition<Throwable>... conditions) {
		List<Condition<TestExecutionResult>> list = Arrays.stream(conditions)//
				.map(TestExecutionResultConditions::throwable)//
				.toList();

		return Assertions.allOf(list);
	}

	/**
	 * Create a new {@link Condition} that matches if and only if a
	 * {@link Throwable}'s {@linkplain Throwable#getCause() cause} matches all
	 * supplied conditions.
	 *
	 * @see #rootCause(Condition...)
	 * @see #suppressed(int, Condition...)
	 */
	@SafeVarargs
	@SuppressWarnings("varargs")
	public static Condition<Throwable> cause(Condition<Throwable>... conditions) {
		List<Condition<Throwable>> list = Arrays.stream(conditions)//
				.map(TestExecutionResultConditions::cause)//
				.toList();

		return Assertions.allOf(list);
	}

	/**
	 * Create a new {@link Condition} that matches if and only if a
	 * {@link Throwable}'s root {@linkplain Throwable#getCause() cause} matches
	 * all supplied conditions.
	 *
	 * @since 1.11
	 * @see #cause(Condition...)
	 * @see #suppressed(int, Condition...)
	 */
	@API(status = MAINTAINED, since = "1.13.3")
	@SafeVarargs
	@SuppressWarnings("varargs")
	public static Condition<Throwable> rootCause(Condition<Throwable>... conditions) {
		List<Condition<Throwable>> list = Arrays.stream(conditions)//
				.map(TestExecutionResultConditions::rootCause)//
				.toList();

		return Assertions.allOf(list);
	}

	/**
	 * Create a new {@link Condition} that matches if and only if a
	 * {@link Throwable}'s {@linkplain Throwable#getSuppressed() suppressed
	 * throwable} at the supplied index matches all supplied conditions.
	 *
	 * @see #cause(Condition...)
	 * @see #rootCause(Condition...)
	 */
	@SafeVarargs
	@SuppressWarnings("varargs")
	public static Condition<Throwable> suppressed(int index, Condition<Throwable>... conditions) {
		List<Condition<Throwable>> list = Arrays.stream(conditions)//
				.map(condition -> suppressed(index, condition))//
				.toList();

		return Assertions.allOf(list);
	}

	/**
	 * Create a new {@link Condition} that matches if and only if a
	 * {@link Throwable} is an {@linkplain Class#isInstance(Object) instance of}
	 * the supplied {@link Class}.
	 */
	public static Condition<Throwable> instanceOf(Class<? extends Throwable> expectedType) {
		return new Condition<>(expectedType::isInstance, "instance of %s", expectedType.getName());
	}

	/**
	 * Create a new {@link Condition} that matches if and only if a
	 * {@link Throwable}'s {@linkplain Throwable#getMessage() message} is equal
	 * to the supplied {@link String}.
	 */
	public static Condition<Throwable> message(String expectedMessage) {
		return new Condition<>(
			FunctionUtils.<Throwable, @Nullable String> where(Throwable::getMessage, isEqual(expectedMessage)),
			"message is '%s'", expectedMessage);
	}

	/**
	 * Create a new {@link Condition} that matches if and only if a
	 * {@link Throwable}'s {@linkplain Throwable#getMessage() message} matches
	 * the supplied {@link Predicate}.
	 */
	public static Condition<Throwable> message(Predicate<String> expectedMessagePredicate) {
		return new Condition<>(
			FunctionUtils.<Throwable, @Nullable String> where(Throwable::getMessage, expectedMessagePredicate),
			"message matches predicate");
	}

	private static Condition<TestExecutionResult> throwable(Condition<? super Throwable> condition) {
		return new Condition<>(
			where(TestExecutionResult::getThrowable,
				throwable -> throwable.isPresent() && condition.matches(throwable.get())),
			"throwable matches %s", condition);
	}

	private static Condition<Throwable> cause(Condition<Throwable> condition) {
		return new Condition<>(throwable -> condition.matches(throwable.getCause()), "throwable cause matches %s",
			condition);
	}

	private static Condition<Throwable> rootCause(Condition<Throwable> condition) {
		Predicate<Throwable> predicate = throwable -> {
			Preconditions.notNull(throwable, "Throwable must not be null");
			Preconditions.notNull(throwable.getCause(), "Throwable does not have a cause");
			Throwable rootCause = getRootCause(throwable, new ArrayList<>());
			return condition.matches(rootCause);
		};
		return new Condition<>(predicate, "throwable root cause matches %s", condition);
	}

	/**
	 * Get the root cause of the supplied {@link Throwable}, or the supplied
	 * {@link Throwable} if it has no cause.
	 */
	private static Throwable getRootCause(Throwable throwable, List<Throwable> causeChain) {
		// If we have already seen the current Throwable, that means we have
		// encountered recursion in the cause chain and therefore return the last
		// Throwable in the cause chain, which was the root cause before the recursion.
		if (causeChain.contains(throwable)) {
			return causeChain.get(causeChain.size() - 1);
		}
		Throwable cause = throwable.getCause();
		if (cause == null) {
			return throwable;
		}
		// Track current Throwable before recursing.
		causeChain.add(throwable);
		return getRootCause(cause, causeChain);
	}

	private static Condition<Throwable> suppressed(int index, Condition<Throwable> condition) {
		return new Condition<>(
			throwable -> throwable.getSuppressed().length > index
					&& condition.matches(throwable.getSuppressed()[index]),
			"suppressed throwable at index %d matches %s", index, condition);
	}

}
