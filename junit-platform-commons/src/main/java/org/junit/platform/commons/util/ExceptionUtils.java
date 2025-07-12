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

import static org.apiguardian.api.API.Status.INTERNAL;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Deque;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Predicate;

import org.apiguardian.api.API;
import org.junit.platform.commons.annotation.Contract;

/**
 * Collection of utilities for working with exceptions.
 *
 * <h2>DISCLAIMER</h2>
 *
 * <p>These utilities are intended solely for usage within the JUnit framework
 * itself. <strong>Any usage by external parties is not supported.</strong>
 * Use at your own risk!
 *
 * @since 1.0
 */
@API(status = INTERNAL, since = "1.0")
public final class ExceptionUtils {

	private static final String JUNIT_PLATFORM_LAUNCHER_PACKAGE_PREFIX = "org.junit.platform.launcher.";

	private static final Predicate<String> STACK_TRACE_ELEMENT_FILTER = ClassNamePatternFilterUtils //
			.excludeMatchingClassNames("org.junit.*,jdk.internal.reflect.*,sun.reflect.*");

	private ExceptionUtils() {
		/* no-op */
	}

	/**
	 * Throw the supplied {@link Throwable}, <em>masked</em> as an
	 * unchecked exception.
	 *
	 * <p>The supplied {@code Throwable} will not be wrapped. Rather, it
	 * will be thrown <em>as is</em> using an exploit of the Java language
	 * that relies on a combination of generics and type erasure to trick
	 * the Java compiler into believing that the thrown exception is an
	 * unchecked exception even if it is a checked exception.
	 *
	 * <h4>Warning</h4>
	 *
	 * <p>This method should be used sparingly.
	 *
	 * @param t the {@code Throwable} to throw as an unchecked exception;
	 * never {@code null}
	 * @return this method always throws an exception and therefore never
	 * returns anything; the return type is merely present to allow this
	 * method to be supplied as the operand in a {@code throw} statement
	 */
	@Contract("_ -> fail")
	public static RuntimeException throwAsUncheckedException(Throwable t) {
		Preconditions.notNull(t, "Throwable must not be null");
		// The following line will never actually return an exception but rather
		// throw t masked as a RuntimeException.
		return ExceptionUtils.throwAs(t);
	}

	@Contract("_ -> fail")
	@SuppressWarnings({ "unchecked", "TypeParameterUnusedInFormals" })
	private static <T extends Throwable> T throwAs(Throwable t) throws T {
		throw (T) t;
	}

	/**
	 * Read the stacktrace of the supplied {@link Throwable} into a String.
	 */
	public static String readStackTrace(Throwable throwable) {
		Preconditions.notNull(throwable, "Throwable must not be null");
		StringWriter stringWriter = new StringWriter();
		try (PrintWriter printWriter = new PrintWriter(stringWriter)) {
			throwable.printStackTrace(printWriter);
		}
		return stringWriter.toString();
	}

	/**
	 * Prune the stack trace of the supplied {@link Throwable} by removing
	 * {@linkplain StackTraceElement stack trace elements} from the {@code org.junit},
	 * {@code jdk.internal.reflect}, and {@code sun.reflect} packages. If a
	 * {@code StackTraceElement} matching one of the supplied {@code classNames}
	 * is encountered, all subsequent elements in the stack trace will be retained.
	 *
	 * <p>Additionally, all elements prior to and including the first JUnit Platform
	 * Launcher call will be removed.
	 *
	 * @param throwable the {@code Throwable} whose stack trace should be pruned;
	 * never {@code null}
	 * @param classNames the class names that should stop the pruning if encountered;
	 * never {@code null}
	 *
	 * @since 1.10
	 */
	@API(status = INTERNAL, since = "1.10")
	public static void pruneStackTrace(Throwable throwable, List<String> classNames) {
		Preconditions.notNull(throwable, "Throwable must not be null");
		Preconditions.notNull(classNames, "List of class names must not be null");

		List<StackTraceElement> stackTrace = Arrays.asList(throwable.getStackTrace());
		List<StackTraceElement> prunedStackTrace = new ArrayList<>();

		Collections.reverse(stackTrace);

		for (int i = 0; i < stackTrace.size(); i++) {
			StackTraceElement element = stackTrace.get(i);
			String className = element.getClassName();

			if (classNames.contains(className)) {
				// Include all elements called by the test
				prunedStackTrace.addAll(stackTrace.subList(i, stackTrace.size()));
				break;
			}
			else if (className.startsWith(JUNIT_PLATFORM_LAUNCHER_PACKAGE_PREFIX)) {
				prunedStackTrace.clear();
			}
			else if (STACK_TRACE_ELEMENT_FILTER.test(className)) {
				prunedStackTrace.add(element);
			}
		}

		Collections.reverse(prunedStackTrace);
		throwable.setStackTrace(prunedStackTrace.toArray(new StackTraceElement[0]));
	}

	/**
	 * Find all causes and suppressed exceptions in the stack trace of the
	 * supplied {@link Throwable}.
	 *
	 * @param rootThrowable the {@code Throwable} to explore; never {@code null}
	 * @return an immutable list of all throwables found, including the supplied
	 * one; never {@code null}
	 *
	 * @since 1.10
	 */
	@API(status = INTERNAL, since = "1.10")
	public static List<Throwable> findNestedThrowables(Throwable rootThrowable) {
		Preconditions.notNull(rootThrowable, "Throwable must not be null");

		Set<Throwable> visited = new LinkedHashSet<>();
		Deque<Throwable> toVisit = new ArrayDeque<>();
		toVisit.add(rootThrowable);

		while (!toVisit.isEmpty()) {
			Throwable current = toVisit.remove();
			boolean isFirstVisit = visited.add(current);
			if (isFirstVisit) {
				Throwable cause = current.getCause();
				if (cause != null) {
					toVisit.add(cause);
				}
				toVisit.addAll(Arrays.asList(current.getSuppressed()));
			}
		}

		return List.copyOf(visited);
	}

}
