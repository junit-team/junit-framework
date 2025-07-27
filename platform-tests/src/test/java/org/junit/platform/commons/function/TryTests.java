/*
 * Copyright 2015-2025 the original author or authors.
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v2.0 which
 * accompanies this distribution and is available at
 *
 * https://www.eclipse.org/legal/epl-v20.html
 */

package org.junit.platform.commons.function;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.fail;

import java.util.concurrent.Callable;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.Test;
import org.junit.platform.commons.JUnitException;

public class TryTests {

	@Test
	void successfulTriesCanBeTransformed() throws Exception {
		var success = Try.success("foo");

		assertThat(success.get()).isEqualTo("foo");
		assertThat(success.getNonNull()).isEqualTo("foo");
		assertThat(success.getOrThrow(RuntimeException::new)).isEqualTo("foo");
		assertThat(success.getNonNullOrThrow(RuntimeException::new)).isEqualTo("foo");
		assertThat(success.toOptional()).contains("foo");

		assertThat(success.andThen(v -> {
			assertThat(v).isEqualTo("foo");
			return Try.success("bar");
		}).get()).isEqualTo("bar");
		assertThat(success.andThenTry(v -> {
			assertThat(v).isEqualTo("foo");
			return "bar";
		}).get()).isEqualTo("bar");

		assertThat(success.orElse(() -> fail("should not be called"))).isSameAs(success);
		assertThat(success.orElseTry(() -> fail("should not be called"))).isSameAs(success);

		var value = new AtomicReference<String>();
		assertThat(success.ifSuccess(value::set)).isSameAs(success);
		assertThat(value.get()).isEqualTo("foo");
		assertThat(success.ifFailure(cause -> fail("should not be called"))).isSameAs(success);
	}

	@Test
	void failedTriesCanBeTransformed() throws Exception {
		var cause = new JUnitException("foo");
		var failure = Try.failure(cause);

		assertThat(assertThrows(JUnitException.class, failure::get)).isSameAs(cause);
		assertThat(assertThrows(RuntimeException.class, () -> failure.getOrThrow(RuntimeException::new))).isInstanceOf(
			RuntimeException.class).hasCause(cause);
		assertThat(failure.toOptional()).isEmpty();

		assertThat(failure.andThen(v -> fail("should not be called"))).isSameAs(failure);
		assertThat(failure.andThenTry(v -> fail("should not be called"))).isSameAs(failure);

		assertThat(failure.orElse(() -> Try.success("bar")).get()).isEqualTo("bar");
		assertThat(failure.orElseTry(() -> "bar").get()).isEqualTo("bar");

		assertThat(failure.ifSuccess(v -> fail("should not be called"))).isSameAs(failure);
		var exception = new AtomicReference<Exception>();
		assertThat(failure.ifFailure(exception::set)).isSameAs(failure);
		assertThat(exception.get()).isSameAs(cause);
	}

	@SuppressWarnings("DataFlowIssue")
	@Test
	void successfulTriesCanStoreNull() throws Exception {
		var success = Try.success(null);
		assertThat(success.get()).isNull();
		assertThrows(JUnitException.class, success::getNonNull);
		assertThat(success.getOrThrow(RuntimeException::new)).isNull();
		assertThrows(RuntimeException.class, () -> success.getNonNullOrThrow(RuntimeException::new));
		assertThat(success.toOptional()).isEmpty();
	}

	@Test
	void triesWithSameContentAreEqual() {
		var cause = new Exception();
		Callable<Object> failingCallable = () -> {
			throw cause;
		};

		var success = Try.call(() -> "foo");
		assertThat(success).isEqualTo(success).hasSameHashCodeAs(success);
		assertThat(success).isEqualTo(Try.success("foo"));
		assertThat(success).isNotEqualTo(Try.failure(cause));

		var failure = Try.call(failingCallable);
		assertThat(failure).isEqualTo(failure).hasSameHashCodeAs(failure);
		assertThat(failure).isNotEqualTo(Try.success("foo"));
		assertThat(failure).isEqualTo(Try.failure(cause));
	}

	@SuppressWarnings("DataFlowIssue")
	@Test
	void methodPreconditionsAreChecked() {
		assertThrows(JUnitException.class, () -> Try.call(null));

		var success = Try.success("foo");
		assertThrows(JUnitException.class, () -> success.andThen(null));
		assertThrows(JUnitException.class, () -> success.andThenTry(null));
		assertThrows(JUnitException.class, () -> success.ifSuccess(null));

		var failure = Try.failure(new Exception());
		assertThrows(JUnitException.class, () -> failure.orElse(null));
		assertThrows(JUnitException.class, () -> failure.orElseTry(null));
		assertThrows(JUnitException.class, () -> failure.ifFailure(null));
	}

}
