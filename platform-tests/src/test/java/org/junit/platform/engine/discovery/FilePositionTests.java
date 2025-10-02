/*
 * Copyright 2015-2025 the original author or authors.
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v2.0 which
 * accompanies this distribution and is available at
 *
 * https://www.eclipse.org/legal/epl-v20.html
 */

package org.junit.platform.engine.discovery;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.EqualsAndHashCodeAssertions.assertEqualsAndHashCode;
import static org.junit.jupiter.params.provider.Arguments.arguments;
import static org.junit.platform.commons.test.PreconditionAssertions.assertPreconditionViolationFor;

import java.util.stream.Stream;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

/**
 * Unit tests for {@link FilePosition}.
 *
 * @since 1.7
 */
@DisplayName("FilePosition unit tests")
class FilePositionTests {

	@Test
	@DisplayName("factory method preconditions")
	void preconditions() {
		assertPreconditionViolationFor(() -> FilePosition.from(-1));
		assertPreconditionViolationFor(() -> FilePosition.from(0, -1));
	}

	@Test
	@DisplayName("create FilePosition from factory method with line number")
	void filePositionFromLine() {
		var filePosition = FilePosition.from(42);

		assertThat(filePosition.getLine()).isEqualTo(42);
		assertThat(filePosition.getColumn()).isEmpty();
	}

	@Test
	@DisplayName("create FilePosition from factory method with line number and column number")
	void filePositionFromLineAndColumn() {
		var filePosition = FilePosition.from(42, 99);

		assertThat(filePosition.getLine()).isEqualTo(42);
		assertThat(filePosition.getColumn()).contains(99);
	}

	/**
	 * @since 1.3
	 */
	@ParameterizedTest
	@MethodSource
	void filePositionFromQuery(String query, int expectedLine, int expectedColumn) {
		var optionalFilePosition = FilePosition.fromQuery(query);

		if (optionalFilePosition.isPresent()) {
			var filePosition = optionalFilePosition.get();

			assertThat(filePosition.getLine()).isEqualTo(expectedLine);
			assertThat(filePosition.getColumn().orElse(-1)).isEqualTo(expectedColumn);
		}
		else {
			assertEquals(-1, expectedColumn);
			assertEquals(-1, expectedLine);
		}
	}

	@SuppressWarnings("unused")
	static Stream<Arguments> filePositionFromQuery() {
		return Stream.of( //
			arguments(null, -1, -1), //
			arguments("?!", -1, -1), //
			arguments("line=ZZ", -1, -1), //
			arguments("line=42", 42, -1), //
			arguments("line=42&column=99", 42, 99), //
			arguments("line=42&column=ZZ", 42, -1), //
			arguments("line=42&abc=xyz&column=99", 42, 99), //
			arguments("1=3&foo=X&line=42&abc=xyz&column=99&enigma=393939", 42, 99), //
			// First one wins:
			arguments("line=42&line=555", 42, -1), //
			arguments("line=42&line=555&column=99&column=555", 42, 99) //
		);
	}

	@Test
	@DisplayName("equals() and hashCode() with column number cached by Integer.valueOf()")
	void equalsAndHashCode() {
		var same = FilePosition.from(42, 99);
		var sameSame = FilePosition.from(42, 99);
		var different = FilePosition.from(1, 2);

		assertEqualsAndHashCode(same, sameSame, different);
	}

	@Test
	@DisplayName("equals() and hashCode() with column number not cached by Integer.valueOf()")
	void equalsAndHashCodeWithColumnNumberNotCachedByJavaLangIntegerDotValueOf() {
		var same = FilePosition.from(42, 99999);
		var sameSame = FilePosition.from(42, 99999);
		var different = FilePosition.from(1, 2);

		assertEqualsAndHashCode(same, sameSame, different);
	}

}
