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

import static java.util.Collections.emptyIterator;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.DynamicContainer.dynamicContainer;
import static org.junit.jupiter.api.DynamicTest.dynamicTest;
import static org.junit.platform.commons.test.PreconditionAssertions.assertPreconditionViolationFor;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.URI;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.function.Function;
import java.util.stream.Stream;

import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.function.Executable;
import org.junit.jupiter.api.function.ThrowingConsumer;
import org.junit.platform.commons.support.ReflectionSupport;
import org.opentest4j.AssertionFailedError;

/**
 * @since 5.0
 */
class DynamicTestTests {

	private static final Executable nix = () -> {
	};

	private final List<@Nullable String> assertedValues = new ArrayList<>();

	@SuppressWarnings("DataFlowIssue")
	@Test
	void streamFromStreamPreconditions() {
		ThrowingConsumer<Object> testExecutor = input -> {
		};
		Function<Object, String> displayNameGenerator = Object::toString;

		assertPreconditionViolationFor(() -> DynamicTest.stream((Stream<?>) null, displayNameGenerator, testExecutor));
		assertPreconditionViolationFor(() -> DynamicTest.stream(Stream.empty(), null, testExecutor));
		assertPreconditionViolationFor(() -> DynamicTest.stream(Stream.empty(), displayNameGenerator, null));
	}

	@SuppressWarnings("DataFlowIssue")
	@Test
	void streamFromIteratorPreconditions() {
		ThrowingConsumer<Object> testExecutor = input -> {
		};
		Function<Object, String> displayNameGenerator = Object::toString;

		assertPreconditionViolationFor(
			() -> DynamicTest.stream((Iterator<?>) null, displayNameGenerator, testExecutor));
		assertPreconditionViolationFor(() -> DynamicTest.stream(emptyIterator(), null, testExecutor));
		assertPreconditionViolationFor(() -> DynamicTest.stream(emptyIterator(), displayNameGenerator, null));
	}

	@SuppressWarnings("DataFlowIssue")
	@Test
	void streamFromStreamWithNamesPreconditions() {
		ThrowingConsumer<Object> testExecutor = input -> {
		};

		assertPreconditionViolationFor(() -> DynamicTest.stream((Stream<? extends Named<Object>>) null, testExecutor));
		assertPreconditionViolationFor(() -> DynamicTest.stream(Stream.empty(), null));
	}

	@SuppressWarnings("DataFlowIssue")
	@Test
	void streamFromIteratorWithNamesPreconditions() {
		ThrowingConsumer<Object> testExecutor = input -> {
		};

		assertPreconditionViolationFor(
			() -> DynamicTest.stream((Iterator<? extends Named<Object>>) null, testExecutor));
		assertPreconditionViolationFor(() -> DynamicTest.stream(emptyIterator(), null));
	}

	@SuppressWarnings("DataFlowIssue")
	@Test
	void streamFromStreamWithNamedExecutablesPreconditions() {
		assertPreconditionViolationFor(() -> DynamicTest.stream((Stream<DummyNamedExecutableForTests>) null));
	}

	@SuppressWarnings("DataFlowIssue")
	@Test
	void streamFromIteratorWithNamedExecutablesPreconditions() {
		assertPreconditionViolationFor(() -> DynamicTest.stream((Iterator<DummyNamedExecutableForTests>) null));
	}

	@Test
	void streamFromStream() throws Throwable {
		Stream<DynamicTest> stream = DynamicTest.stream(Stream.of("foo", "bar", "baz"), String::toUpperCase,
			this::throwingConsumer);
		assertStream(stream);
	}

	@Test
	void streamFromIterator() throws Throwable {
		Stream<DynamicTest> stream = DynamicTest.stream(List.of("foo", "bar", "baz").iterator(), String::toUpperCase,
			this::throwingConsumer);
		assertStream(stream);
	}

	@Test
	void streamFromStreamWithNames() throws Throwable {
		Stream<DynamicTest> stream = DynamicTest.stream(
			Stream.of(Named.of("FOO", "foo"), Named.of("BAR", "bar"), Named.of("BAZ", "baz")), this::throwingConsumer);
		assertStream(stream);
	}

	@Test
	void streamFromIteratorWithNames() throws Throwable {
		Stream<DynamicTest> stream = DynamicTest.stream(
			List.of(Named.of("FOO", "foo"), Named.of("BAR", "bar"), Named.of("BAZ", "baz")).iterator(),
			this::throwingConsumer);
		assertStream(stream);
	}

	@Test
	void streamFromStreamWithNamedExecutables() throws Throwable {
		Stream<DynamicTest> stream = DynamicTest.stream(
			Stream.of(new DummyNamedExecutableForTests("foo", this::throwingConsumer),
				new DummyNamedExecutableForTests("bar", this::throwingConsumer),
				new DummyNamedExecutableForTests("baz", this::throwingConsumer)));

		assertStream(stream);
	}

	@Test
	void streamFromIteratorWithNamedExecutables() throws Throwable {
		Stream<DynamicTest> stream = DynamicTest.stream(
			List.of(new DummyNamedExecutableForTests("foo", this::throwingConsumer),
				new DummyNamedExecutableForTests("bar", this::throwingConsumer),
				new DummyNamedExecutableForTests("baz", this::throwingConsumer)).iterator());

		assertStream(stream);
	}

	private void assertStream(Stream<DynamicTest> stream) throws Throwable {
		List<DynamicTest> dynamicTests = stream.toList();

		assertThat(dynamicTests).extracting(DynamicTest::getDisplayName).containsExactly("FOO", "BAR", "BAZ");

		assertThat(assertedValues).isEmpty();

		dynamicTests.get(0).getExecutable().execute();
		assertThat(assertedValues).containsExactly("foo");

		dynamicTests.get(1).getExecutable().execute();
		assertThat(assertedValues).containsExactly("foo", "bar");

		Throwable t = assertThrows(Throwable.class, () -> dynamicTests.get(2).getExecutable().execute());
		assertThat(t).hasMessage("Baz!");
		assertThat(assertedValues).containsExactly("foo", "bar");
	}

	private void throwingConsumer(@Nullable String str) throws Throwable {
		if ("baz".equals(str)) {
			throw new Throwable("Baz!");
		}
		this.assertedValues.add(str);
	}

	@Test
	void reflectiveOperationsThrowingAssertionFailedError() {
		Throwable t48 = assertThrows(AssertionFailedError.class,
			() -> dynamicTest("1 == 48", this::assert1Equals48Directly).getExecutable().execute());
		assertThat(t48).hasMessage("expected: <1> but was: <48>");

		Throwable t49 = assertThrows(AssertionFailedError.class, () -> dynamicTest("1 == 49",
			this::assert1Equals49ReflectivelyAndUnwrapInvocationTargetException).getExecutable().execute());
		assertThat(t49).hasMessage("expected: <1> but was: <49>");
	}

	@Test
	void reflectiveOperationThrowingInvocationTargetException() {
		Throwable t50 = assertThrows(InvocationTargetException.class,
			() -> dynamicTest("1 == 50", this::assert1Equals50Reflectively).getExecutable().execute());
		assertThat(t50.getCause()).hasMessage("expected: <1> but was: <50>");
	}

	@Test
	void sourceUriIsNotPresentByDefault() {
		DynamicTest test = dynamicTest("foo", nix);
		assertThat(test.getTestSourceUri()).isNotPresent();
		assertThat(test.toString()).isEqualTo("DynamicTest [displayName = 'foo', testSourceUri = null]");
		DynamicContainer container = dynamicContainer("bar", Stream.of(test));
		assertThat(container.getTestSourceUri()).isNotPresent();
		assertThat(container.toString()).isEqualTo("DynamicContainer [displayName = 'bar', testSourceUri = null]");
	}

	@Test
	void sourceUriIsReturnedWhenSupplied() {
		URI testSourceUri = URI.create("any://test");
		DynamicTest test = dynamicTest("foo", testSourceUri, nix);
		URI containerSourceUri = URI.create("other://container");
		DynamicContainer container = dynamicContainer("bar", containerSourceUri, Stream.of(test));

		assertThat(test.getTestSourceUri()).containsSame(testSourceUri);
		assertThat(test.toString()).isEqualTo("DynamicTest [displayName = 'foo', testSourceUri = any://test]");
		assertThat(container.getTestSourceUri()).containsSame(containerSourceUri);
		assertThat(container.toString()).isEqualTo(
			"DynamicContainer [displayName = 'bar', testSourceUri = other://container]");
	}

	private void assert1Equals48Directly() {
		Assertions.assertEquals(1, 48);
	}

	private void assert1Equals49ReflectivelyAndUnwrapInvocationTargetException() throws Throwable {
		Method method = Assertions.class.getMethod("assertEquals", int.class, int.class);
		ReflectionSupport.invokeMethod(method, null, 1, 49);
	}

	private void assert1Equals50Reflectively() throws Throwable {
		Method method = Assertions.class.getMethod("assertEquals", int.class, int.class);
		method.invoke(null, 1, 50);
	}

	record DummyNamedExecutableForTests(String name, ThrowingConsumer<String> consumer) implements NamedExecutable {

		@Override
		public String getName() {
			return name.toUpperCase(Locale.ROOT);
		}

		@Override
		public void execute() throws Throwable {
			consumer.accept(name);
		}
	}

}
