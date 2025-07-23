/*
 * Copyright 2015-2025 the original author or authors.
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v2.0 which
 * accompanies this distribution and is available at
 *
 * https://www.eclipse.org/legal/epl-v20.html
 */

package org.junit.jupiter.api;

import static org.apiguardian.api.API.Status.STABLE;
import static org.junit.jupiter.api.AssertionUtils.getCanonicalName;

import java.util.function.Supplier;

import org.apiguardian.api.API;
import org.jspecify.annotations.Nullable;
import org.junit.platform.commons.util.StringUtils;
import org.opentest4j.AssertionFailedError;

/**
 * Builder for {@link AssertionFailedError AssertionFailedErrors}.
 *
 * <p>Using this builder ensures consistency in how failure message are formatted
 * within JUnit Jupiter and for custom user-defined assertions.
 *
 * @since 5.9
 * @see AssertionFailedError
 */
@API(status = STABLE, since = "5.9")
public class AssertionFailureBuilder {

	private @Nullable Object message;

	private @Nullable Throwable cause;

	private boolean mismatch;

	private @Nullable Object expected;

	private @Nullable Object actual;

	private @Nullable String reason;

	private boolean includeValuesInMessage = true;

	/**
	 * Create a new {@code AssertionFailureBuilder}.
	 */
	public static AssertionFailureBuilder assertionFailure() {
		return new AssertionFailureBuilder();
	}

	private AssertionFailureBuilder() {
	}

	/**
	 * Set the user-defined message of the assertion.
	 *
	 * <p>The {@code message} may be passed as a {@link Supplier} or plain
	 * {@link String}. If any other type is passed, it is converted to
	 * {@code String} as per {@link StringUtils#nullSafeToString(Object)}.
	 *
	 * @param message the user-defined failure message; may be {@code null}
	 * @return this builder for method chaining
	 */
	public AssertionFailureBuilder message(@Nullable Object message) {
		this.message = message;
		return this;
	}

	/**
	 * Set the reason why the assertion failed.
	 *
	 * @param reason the failure reason; may be {@code null}
	 * @return this builder for method chaining
	 */
	public AssertionFailureBuilder reason(@Nullable String reason) {
		this.reason = reason;
		return this;
	}

	/**
	 * Set the cause of the assertion failure.
	 *
	 * @param cause the failure cause; may be {@code null}
	 * @return this builder for method chaining
	 */
	public AssertionFailureBuilder cause(@Nullable Throwable cause) {
		this.cause = cause;
		return this;
	}

	/**
	 * Set the expected value of the assertion.
	 *
	 * @param expected the expected value; may be {@code null}
	 * @return this builder for method chaining
	 */
	public AssertionFailureBuilder expected(@Nullable Object expected) {
		this.mismatch = true;
		this.expected = expected;
		return this;
	}

	/**
	 * Set the actual value of the assertion.
	 *
	 * @param actual the actual value; may be {@code null}
	 * @return this builder for method chaining
	 */
	public AssertionFailureBuilder actual(@Nullable Object actual) {
		this.mismatch = true;
		this.actual = actual;
		return this;
	}

	/**
	 * Set whether to include the actual and expected values in the generated
	 * failure message.
	 *
	 * @param includeValuesInMessage whether to include the actual and expected
	 * values
	 * @return this builder for method chaining
	 */
	public AssertionFailureBuilder includeValuesInMessage(boolean includeValuesInMessage) {
		this.includeValuesInMessage = includeValuesInMessage;
		return this;
	}

	/**
	 * Build the {@link AssertionFailedError AssertionFailedError} and throw it.
	 *
	 * @throws AssertionFailedError always
	 */
	public void buildAndThrow() throws AssertionFailedError {
		throw build();
	}

	/**
	 * Build the {@link AssertionFailedError AssertionFailedError} without
	 * throwing it.
	 *
	 * @return the built assertion failure
	 */
	public AssertionFailedError build() {
		String reason = nullSafeGet(this.reason);
		if (mismatch && includeValuesInMessage) {
			reason = (reason == null ? "" : reason + ", ") + formatValues(expected, actual);
		}
		String message = nullSafeGet(this.message);
		if (reason != null) {
			message = buildPrefix(message) + reason;
		}
		return mismatch //
				? new AssertionFailedError(message, expected, actual, cause) //
				: new AssertionFailedError(message, cause);
	}

	private static @Nullable String nullSafeGet(@Nullable Object messageOrSupplier) {
		if (messageOrSupplier == null) {
			return null;
		}
		if (messageOrSupplier instanceof Supplier<?> supplier) {
			Object message = supplier.get();
			return StringUtils.nullSafeToString(message);
		}
		return StringUtils.nullSafeToString(messageOrSupplier);
	}

	private static String buildPrefix(@Nullable String message) {
		return (StringUtils.isNotBlank(message) ? message + " ==> " : "");
	}

	private static String formatValues(@Nullable Object expected, @Nullable Object actual) {
		String expectedString = toString(expected);
		String actualString = toString(actual);
		if (expectedString.equals(actualString)) {
			return "expected: %s but was: %s".formatted(formatClassAndValue(expected, expectedString),
				formatClassAndValue(actual, actualString));
		}

		// Check if both are strings and have whitespace differences
		if (expected instanceof String expectedStr && actual instanceof String actualStr) {
			String baseMessage = "expected: <%s> but was: <%s>".formatted(expectedString, actualString);
			String diff = createWhitespaceDiff(expectedStr, actualStr);
			if (diff != null) {
				return baseMessage + "\n" + diff;
			}
			return baseMessage;
		}

		return "expected: <%s> but was: <%s>".formatted(expectedString, actualString);
	}

	/**
	 * Creates a diff showing whitespace differences between two strings.
	 * Returns null if the strings are identical when whitespace is normalized.
	 */
	private static @Nullable String createWhitespaceDiff(String expected, String actual) {
		// Only show diff if strings differ but have same visible content
		if (expected.replaceAll("\\s+", " ").trim().equals(actual.replaceAll("\\s+", " ").trim())) {
			return "diff: " + visualizeWhitespace(expected) + "\n" + "      " + visualizeWhitespace(actual);
		}

		// Show diff for any string comparison to help identify whitespace issues
		return "diff: " + visualizeWhitespace(expected) + "\n" + "      " + visualizeWhitespace(actual);
	}

	/**
	 * Converts whitespace characters to their visual representations.
	 */
	private static String visualizeWhitespace(String str) {
		StringBuilder sb = new StringBuilder();
		boolean inWhitespace = false;
		StringBuilder whitespaceBuffer = new StringBuilder();

		for (int i = 0; i < str.length(); i++) {
			char c = str.charAt(i);

			if (isVisualizableWhitespace(c)) {
				if (!inWhitespace) {
					inWhitespace = true;
					whitespaceBuffer.setLength(0);
					whitespaceBuffer.append("[");
				}
				whitespaceBuffer.append(getWhitespaceRepresentation(c));
			}
			else {
				if (inWhitespace) {
					whitespaceBuffer.append("]");
					sb.append(whitespaceBuffer.toString());
					inWhitespace = false;
				}
				sb.append(c);
			}
		}

		// Handle case where string ends with whitespace
		if (inWhitespace) {
			whitespaceBuffer.append("]");
			sb.append(whitespaceBuffer.toString());
		}

		return sb.toString();
	}

	/**
	 * Checks if a character should be visualized as whitespace
	 */
	private static boolean isVisualizableWhitespace(char c) {
		return c == ' ' || c == '\t' || c == '\n' || c == '\r' || c == '\f' || (Character.isWhitespace(c) && c != ' ');
	}

	/**
	 * Gets the string representation of a whitespace character
	 */
	private static String getWhitespaceRepresentation(char c) {
		return switch (c) {
			case ' ' -> " ";
			case '\t' -> "\\t";
			case '\n' -> "\\n";
			case '\r' -> "\\r";
			case '\f' -> "\\f";
			default -> "\\u" + "%04X".formatted((int) c);
		};
	}

	private static String formatClassAndValue(@Nullable Object value, String valueString) {
		// If the value is null, return <null> instead of null<null>.
		if (value == null) {
			return "<null>";
		}
		String classAndHash = getClassName(value) + toHash(value);
		// if it's a class, there's no need to repeat the class name contained in the valueString.
		return (value instanceof Class ? "<" + classAndHash + ">" : classAndHash + "<" + valueString + ">");
	}

	private static String toString(@Nullable Object obj) {
		if (obj instanceof Class<?> clazz) {
			return getCanonicalName(clazz);
		}
		return StringUtils.nullSafeToString(obj);
	}

	private static String toHash(@Nullable Object obj) {
		return (obj == null ? "" : "@" + Integer.toHexString(System.identityHashCode(obj)));
	}

	private static String getClassName(@Nullable Object obj) {
		return (obj == null ? "null"
				: obj instanceof Class<?> clazz ? getCanonicalName(clazz) : obj.getClass().getName());
	}

}
