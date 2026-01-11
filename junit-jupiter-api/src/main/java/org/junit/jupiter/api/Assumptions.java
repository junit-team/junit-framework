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

import static org.apiguardian.api.API.Status.STABLE;

import java.util.function.BooleanSupplier;
import java.util.function.Supplier;

import org.apiguardian.api.API;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.function.Executable;
import org.junit.platform.commons.annotation.Contract;
import org.junit.platform.commons.util.ExceptionUtils;
import org.junit.platform.commons.util.StringUtils;
import org.opentest4j.TestAbortedException;

/**
 * {@code Assumptions} is a collection of utility methods that support
 * conditional test execution based on <em>assumptions</em>.
 *
 * <p>In direct contrast to failed {@linkplain Assertions assertions},
 * failed assumptions do not result in a test <em>failure</em>; rather,
 * a failed assumption results in a test being <em>aborted</em>. However,
 * failed assertions and other exceptions thrown by tests take precedence over
 * failed assumptions when both are thrown during the execution of a test
 * (for example, by different lifecycle methods), regardless of the order they
 * are thrown in. In such cases, the test will be reported as <em>failed</em>
 * rather than <em>aborted</em>.
 *
 * <p>Assumptions are typically used whenever it does not make sense to
 * continue execution of a given test method &mdash; for example, if the
 * test depends on something that does not exist in the current runtime
 * environment.
 *
 * <p>Although it is technically possible to extend this class, extension is
 * strongly discouraged. The JUnit Team highly recommends that the methods
 * defined in this class be used via <em>static imports</em>.
 *
 * @since 5.0
 * @see TestAbortedException
 * @see Assertions
 */
@API(status = STABLE, since = "5.0")
public class Assumptions {

	/**
	 * Protected constructor allowing subclassing but not direct instantiation.
	 *
	 * @since 5.3
	 */
	protected Assumptions() {
		/* no-op */
	}

	// --- assumeTrue ----------------------------------------------------------

	/**
	 * Validate the given assumption.
	 *
	 * @param assumption the assumption to validate
	 * @throws TestAbortedException if the assumption is not {@code true}
	 */
	@Contract("false -> fail")
	public static void assumeTrue(boolean assumption) throws TestAbortedException {
		assumeTrue(assumption, "assumption is not true");
	}

	/**
	 * Validate the given assumption.
	 *
	 * @param assumptionSupplier the supplier of the assumption to validate
	 * @throws TestAbortedException if the assumption is not {@code true}
	 */
	public static void assumeTrue(BooleanSupplier assumptionSupplier) throws TestAbortedException {
		assumeTrue(assumptionSupplier.getAsBoolean(), "assumption is not true");
	}

	/**
	 * Validate the given assumption.
	 *
	 * @param assumptionSupplier the supplier of the assumption to validate
	 * @param message the message to be included in the {@code TestAbortedException}
	 * if the assumption is invalid
	 * @throws TestAbortedException if the assumption is not {@code true}
	 */
	public static void assumeTrue(BooleanSupplier assumptionSupplier, @Nullable String message)
			throws TestAbortedException {
		assumeTrue(assumptionSupplier.getAsBoolean(), message);
	}

	/**
	 * Validate the given assumption.
	 *
	 * @param assumption the assumption to validate
	 * @param messageSupplier the supplier of the message to be included in
	 * the {@code TestAbortedException} if the assumption is invalid
	 * @throws TestAbortedException if the assumption is not {@code true}
	 */
	@Contract("false, _ -> fail")
	public static void assumeTrue(boolean assumption, Supplier<@Nullable String> messageSupplier)
			throws TestAbortedException {
		if (!assumption) {
			throwAssumptionFailed(messageSupplier.get());
		}
	}

	/**
	 * Validate the given assumption.
	 *
	 * @param assumption the assumption to validate
	 * @param message the message to be included in the {@code TestAbortedException}
	 * if the assumption is invalid
	 * @throws TestAbortedException if the assumption is not {@code true}
	 */
	@Contract("false, _ -> fail")
	public static void assumeTrue(boolean assumption, @Nullable String message) throws TestAbortedException {
		if (!assumption) {
			throwAssumptionFailed(message);
		}
	}

	/**
	 * Validate the given assumption.
	 *
	 * @param assumptionSupplier the supplier of the assumption to validate
	 * @param messageSupplier the supplier of the message to be included in
	 * the {@code TestAbortedException} if the assumption is invalid
	 * @throws TestAbortedException if the assumption is not {@code true}
	 */
	public static void assumeTrue(BooleanSupplier assumptionSupplier, Supplier<@Nullable String> messageSupplier)
			throws TestAbortedException {

		assumeTrue(assumptionSupplier.getAsBoolean(), messageSupplier);
	}

	// --- assumeFalse ---------------------------------------------------------

	/**
	 * Validate the given assumption.
	 *
	 * @param assumption the assumption to validate
	 * @throws TestAbortedException if the assumption is not {@code false}
	 */
	@Contract("true -> fail")
	public static void assumeFalse(boolean assumption) throws TestAbortedException {
		assumeFalse(assumption, "assumption is not false");
	}

	/**
	 * Validate the given assumption.
	 *
	 * @param assumptionSupplier the supplier of the assumption to validate
	 * @throws TestAbortedException if the assumption is not {@code false}
	 */
	public static void assumeFalse(BooleanSupplier assumptionSupplier) throws TestAbortedException {
		assumeFalse(assumptionSupplier.getAsBoolean(), "assumption is not false");
	}

	/**
	 * Validate the given assumption.
	 *
	 * @param assumptionSupplier the supplier of the assumption to validate
	 * @param message the message to be included in the {@code TestAbortedException}
	 * if the assumption is invalid
	 * @throws TestAbortedException if the assumption is not {@code false}
	 */
	public static void assumeFalse(BooleanSupplier assumptionSupplier, @Nullable String message)
			throws TestAbortedException {
		assumeFalse(assumptionSupplier.getAsBoolean(), message);
	}

	/**
	 * Validate the given assumption.
	 *
	 * @param assumption the assumption to validate
	 * @param messageSupplier the supplier of the message to be included in
	 * the {@code TestAbortedException} if the assumption is invalid
	 * @throws TestAbortedException if the assumption is not {@code false}
	 */
	@Contract("true, _ -> fail")
	public static void assumeFalse(boolean assumption, Supplier<@Nullable String> messageSupplier)
			throws TestAbortedException {
		if (assumption) {
			throwAssumptionFailed(messageSupplier.get());
		}
	}

	/**
	 * Validate the given assumption.
	 *
	 * @param assumption the assumption to validate
	 * @param message the message to be included in the {@code TestAbortedException}
	 * if the assumption is invalid
	 * @throws TestAbortedException if the assumption is not {@code false}
	 */
	@Contract("true, _ -> fail")
	public static void assumeFalse(boolean assumption, @Nullable String message) throws TestAbortedException {
		if (assumption) {
			throwAssumptionFailed(message);
		}
	}

	/**
	 * Validate the given assumption.
	 *
	 * @param assumptionSupplier the supplier of the assumption to validate
	 * @param messageSupplier the supplier of the message to be included in
	 * the {@code TestAbortedException} if the assumption is invalid
	 * @throws TestAbortedException if the assumption is not {@code false}
	 */
	public static void assumeFalse(BooleanSupplier assumptionSupplier, Supplier<@Nullable String> messageSupplier)
			throws TestAbortedException {

		assumeFalse(assumptionSupplier.getAsBoolean(), messageSupplier);
	}

	// --- assumingThat --------------------------------------------------------

	/**
	 * Execute the supplied {@link Executable}, but only if the supplied
	 * assumption is valid.
	 *
	 * <p>Unlike the other assumption methods, this method will not abort the test.
	 * If the assumption is invalid, this method does nothing. If the assumption is
	 * valid and the {@code executable} throws an exception, it will be treated like
	 * a regular test <em>failure</em>. That exception will be rethrown <em>as is</em>
	 * but {@link ExceptionUtils#throwAsUncheckedException masked} as an unchecked
	 * exception.
	 *
	 * @param assumptionSupplier the supplier of the assumption to validate
	 * @param executable the block of code to execute if the assumption is valid
	 * @see #assumingThat(boolean, Executable)
	 */
	public static void assumingThat(BooleanSupplier assumptionSupplier, Executable executable) {
		assumingThat(assumptionSupplier.getAsBoolean(), executable);
	}

	/**
	 * Execute the supplied {@link Executable}, but only if the supplied
	 * assumption is valid.
	 *
	 * <p>Unlike the other assumption methods, this method will not abort the test.
	 * If the assumption is invalid, this method does nothing. If the assumption is
	 * valid and the {@code executable} throws an exception, it will be treated like
	 * a regular test <em>failure</em>. That exception will be rethrown <em>as is</em>
	 * but {@link ExceptionUtils#throwAsUncheckedException masked} as an unchecked
	 * exception.
	 *
	 * @param assumption the assumption to validate
	 * @param executable the block of code to execute if the assumption is valid
	 * @see #assumingThat(BooleanSupplier, Executable)
	 */
	public static void assumingThat(boolean assumption, Executable executable) {
		if (assumption) {
			try {
				executable.execute();
			}
			catch (Throwable t) {
				throw ExceptionUtils.throwAsUncheckedException(t);
			}
		}
	}

	// --- abort ---------------------------------------------------------------

	/**
	 * <em>Abort</em> the test <em>without</em> a message.
	 *
	 * <p>Although aborting with an explicit message is recommended, this may be
	 * useful when maintaining legacy code.
	 *
	 * <p>See Javadoc for {@link #abort(String)} for an explanation of this
	 * method's generic return type {@code V}.
	 *
	 * @throws TestAbortedException always
	 * @since 5.9
	 */
	@Contract(" -> fail")
	@API(status = STABLE, since = "5.9")
	@SuppressWarnings("TypeParameterUnusedInFormals")
	public static <V> V abort() {
		throw new TestAbortedException();
	}

	/**
	 * <em>Abort</em> the test with the given {@code message}.
	 *
	 * <p>The generic return type {@code V} allows this method to be used
	 * directly as a single-statement lambda expression, thereby avoiding the
	 * need to implement a code block with an explicit return value. Since this
	 * method throws a {@link TestAbortedException} before its return statement,
	 * this method never actually returns a value to its caller. The following
	 * example demonstrates how this may be used in practice.
	 *
	 * <pre>{@code
	 * Stream.of().map(entry -> abort("assumption not met"));
	 * }</pre>
	 *
	 * @param message the message to be included in the {@code TestAbortedException}
	 * @throws TestAbortedException always
	 * @since 5.9
	 */
	@Contract("_ -> fail")
	@API(status = STABLE, since = "5.9")
	@SuppressWarnings("TypeParameterUnusedInFormals")
	public static <V> V abort(String message) {
		throw new TestAbortedException(message);
	}

	/**
	 * <em>Abort</em> the test with the supplied message.
	 *
	 * <p>See Javadoc for {@link #abort(String)} for an explanation of this
	 * method's generic return type {@code V}.
	 *
	 * @param messageSupplier the supplier of the message to be included in the
	 * {@code TestAbortedException}
	 * @throws TestAbortedException always
	 * @since 5.9
	 */
	@Contract("_ -> fail")
	@API(status = STABLE, since = "5.9")
	@SuppressWarnings("TypeParameterUnusedInFormals")
	public static <V> V abort(Supplier<String> messageSupplier) {
		throw new TestAbortedException(messageSupplier.get());
	}

	@Contract("_ -> fail")
	private static void throwAssumptionFailed(@Nullable String message) {
		throw new TestAbortedException(
			StringUtils.isNotBlank(message) ? "Assumption failed: " + message : "Assumption failed");
	}

}
