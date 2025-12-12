/*
 * Copyright 2015-2025 the original author or authors.
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v2.0 which
 * accompanies this distribution and is available at
 *
 * https://www.eclipse.org/legal/epl-v20.html
 */

package org.junit.platform.engine.support.store;

import static java.util.Collections.synchronizedList;
import static java.util.Objects.requireNonNull;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.platform.commons.test.ConcurrencyTestingUtils.executeConcurrently;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for {@link NamespacedHierarchicalStore}.
 *
 * @since 5.0
 */
public class NamespacedHierarchicalStoreTests {

	private final Object key = "key";
	private final Object value = "value";

	private final String namespace = "ns";

	private final NamespacedHierarchicalStore.CloseAction<String> closeAction = mock();
	private final NamespacedHierarchicalStore<String> grandParentStore = new NamespacedHierarchicalStore<>(null,
		closeAction);
	private final NamespacedHierarchicalStore<String> parentStore = grandParentStore.newChild();
	private final NamespacedHierarchicalStore<String> store = parentStore.newChild();

	@Nested
	class StoringValuesTests {

		@Test
		void getWithUnknownKeyReturnsNull() {
			assertNull(store.get(namespace, "unknown key"));
		}

		@Test
		void putAndGetWithSameKey() {
			store.put(namespace, key, value);
			assertEquals(value, store.get(namespace, key));
		}

		@Test
		void valueCanBeReplaced() {
			store.put(namespace, key, value);

			Object newValue = new Object();
			assertEquals(value, store.put(namespace, key, newValue));

			assertEquals(newValue, store.get(namespace, key));
		}

		@Test
		void valueIsComputedIfAbsent() {
			assertNull(store.get(namespace, key));
			assertEquals(value, store.computeIfAbsent(namespace, key, __ -> value));
			assertEquals(value, store.get(namespace, key));
		}

		@SuppressWarnings("deprecation")
		@Test
		void valueIsNotComputedIfPresentLocally() {
			store.put(namespace, key, value);

			assertEquals(value, store.getOrComputeIfAbsent(namespace, key, __ -> "a different value"));
			assertEquals(value, store.computeIfAbsent(namespace, key, __ -> "a different value"));
			assertEquals(value, store.get(namespace, key));
		}

		@SuppressWarnings("deprecation")
		@Test
		void valueIsNotComputedIfPresentInParent() {
			parentStore.put(namespace, key, value);

			assertEquals(value, store.getOrComputeIfAbsent(namespace, key, __ -> "a different value"));
			assertEquals(value, store.computeIfAbsent(namespace, key, __ -> "a different value"));
			assertEquals(value, store.get(namespace, key));
		}

		@SuppressWarnings("deprecation")
		@Test
		void valueIsNotComputedIfPresentInGrandParent() {
			grandParentStore.put(namespace, key, value);

			assertEquals(value, store.getOrComputeIfAbsent(namespace, key, __ -> "a different value"));
			assertEquals(value, store.computeIfAbsent(namespace, key, __ -> "a different value"));
			assertEquals(value, store.get(namespace, key));
		}

		@SuppressWarnings("deprecation")
		@Test
		void nullIsAValidValueToPut() {
			store.put(namespace, key, null);

			assertNull(store.getOrComputeIfAbsent(namespace, key, __ -> "a different value"));
			assertNull(store.get(namespace, key));

			assertEquals("a different value", store.computeIfAbsent(namespace, key, __ -> "a different value"));
			assertEquals("a different value", store.get(namespace, key));
		}

		@SuppressWarnings("deprecation")
		@Test
		void keysCanBeRemoved() {
			store.put(namespace, key, value);
			assertEquals(value, store.remove(namespace, key));
			assertNull(store.get(namespace, key));

			assertEquals("a different value", store.getOrComputeIfAbsent(namespace, key, __ -> "a different value"));
			assertEquals("a different value", store.remove(namespace, key));
			assertNull(store.get(namespace, key));

			assertEquals("another different value",
				store.computeIfAbsent(namespace, key, __ -> "another different value"));
			assertEquals("another different value", store.remove(namespace, key));
			assertNull(store.get(namespace, key));
		}

		@Test
		void sameKeyWithDifferentNamespaces() {
			Object value1 = createObject("value1");
			String namespace1 = "ns1";

			Object value2 = createObject("value2");
			String namespace2 = "ns2";

			store.put(namespace1, key, value1);
			store.put(namespace2, key, value2);

			assertEquals(value1, store.get(namespace1, key));
			assertEquals(value2, store.get(namespace2, key));
		}

		@Test
		void valueIsComputedIfAbsentInDifferentNamespace() {
			String namespace1 = "ns1";
			String namespace2 = "ns2";

			assertEquals(value, store.computeIfAbsent(namespace1, key, __ -> value));
			assertEquals(value, store.get(namespace1, key));

			assertNull(store.get(namespace2, key));
		}

		@Test
		void keyIsOnlyRemovedInGivenNamespace() {
			String namespace1 = "ns1";
			String namespace2 = "ns2";

			Object value1 = createObject("value1");
			Object value2 = createObject("value2");

			store.put(namespace1, key, value1);
			store.put(namespace2, key, value2);
			store.remove(namespace1, key);

			assertNull(store.get(namespace1, key));
			assertEquals(value2, store.get(namespace2, key));
		}

		@Test
		void getWithTypeSafetyAndInvalidRequiredTypeThrowsException() {
			Integer key = 42;
			String value = "enigma";
			store.put(namespace, key, value);

			Exception exception = assertThrows(NamespacedHierarchicalStoreException.class,
				() -> store.get(namespace, key, Number.class));
			assertEquals(
				"Object stored under key [42] is not of required type [java.lang.Number], but was [java.lang.String]: enigma",
				exception.getMessage());
		}

		@Test
		void getWithTypeSafety() {
			Integer key = 42;
			String value = "enigma";
			store.put(namespace, key, value);

			// The fact that we can declare this as a String suffices for testing the required type.
			String requiredTypeValue = store.get(namespace, key, String.class);
			assertEquals(value, requiredTypeValue);
		}

		@SuppressWarnings("DataFlowIssue")
		@Test
		void getWithTypeSafetyAndPrimitiveValueType() {
			String key = "enigma";
			int value = 42;
			store.put(namespace, key, value);

			// The fact that we can declare this as an int/Integer suffices for testing the required type.
			int requiredInt = store.get(namespace, key, int.class);
			Integer requiredInteger = store.get(namespace, key, Integer.class);
			assertEquals(value, requiredInt);
			assertEquals(value, requiredInteger.intValue());
		}

		@Test
		void getNullValueWithTypeSafety() {
			store.put(namespace, key, null);

			// The fact that we can declare this as a String suffices for testing the required type.
			String requiredTypeValue = store.get(namespace, key, String.class);
			assertNull(requiredTypeValue);
		}

		@SuppressWarnings("deprecation")
		@Test
		void getOrComputeIfAbsentWithTypeSafetyAndInvalidRequiredTypeThrowsException() {
			String key = "pi";
			Float value = 3.14f;

			// Store a Float...
			store.put(namespace, key, value);

			// But declare that our function creates a String...
			Function<String, String> defaultCreator = k -> "enigma";

			Exception exception = assertThrows(NamespacedHierarchicalStoreException.class,
				() -> store.getOrComputeIfAbsent(namespace, key, defaultCreator, String.class));
			assertEquals(
				"Object stored under key [pi] is not of required type [java.lang.String], but was [java.lang.Float]: 3.14",
				exception.getMessage());
		}

		@Test
		void computeIfAbsentWithTypeSafetyAndInvalidRequiredTypeThrowsException() {
			String key = "pi";
			Float value = 3.14f;

			// Store a Float...
			store.put(namespace, key, value);

			// But declare that our function creates a String...
			Function<String, String> defaultCreator = k -> "enigma";

			Exception exception = assertThrows(NamespacedHierarchicalStoreException.class,
				() -> store.computeIfAbsent(namespace, key, defaultCreator, String.class));
			assertEquals(
				"Object stored under key [pi] is not of required type [java.lang.String], but was [java.lang.Float]: 3.14",
				exception.getMessage());
		}

		@SuppressWarnings("deprecation")
		@Test
		void getOrComputeIfAbsentWithTypeSafety() {
			Integer key = 42;
			String value = "enigma";

			// The fact that we can declare this as a String suffices for testing the required type.
			String computedValue = store.getOrComputeIfAbsent(namespace, key, k -> value, String.class);
			assertEquals(value, computedValue);
		}

		@Test
		void computeIfAbsentWithTypeSafety() {
			Integer key = 42;
			String value = "enigma";

			// The fact that we can declare this as a String suffices for testing the required type.
			String computedValue = store.computeIfAbsent(namespace, key, __ -> value, String.class);
			assertEquals(value, computedValue);
		}

		@SuppressWarnings({ "DataFlowIssue", "deprecation" })
		@Test
		void getOrComputeIfAbsentWithTypeSafetyAndPrimitiveValueType() {
			String key = "enigma";
			int value = 42;

			// The fact that we can declare this as an int/Integer suffices for testing the required type.
			int computedInt = store.getOrComputeIfAbsent(namespace, key, k -> value, int.class);
			Integer computedInteger = store.getOrComputeIfAbsent(namespace, key, k -> value, Integer.class);
			assertEquals(value, computedInt);
			assertEquals(value, computedInteger.intValue());
		}

		@Test
		void computeIfAbsentWithTypeSafetyAndPrimitiveValueType() {
			String key = "enigma";
			int value = 42;

			// The fact that we can declare this as an int/Integer suffices for testing the required type.
			int computedInt = store.computeIfAbsent(namespace, key, k -> value, int.class);
			Integer computedInteger = store.computeIfAbsent(namespace, key, k -> value, Integer.class);
			assertEquals(value, computedInt);
			assertEquals(value, computedInteger.intValue());
		}

		@SuppressWarnings("deprecation")
		@Test
		void getOrComputeIfAbsentWithExceptionThrowingCreatorFunction() {
			var e = assertThrows(RuntimeException.class, () -> store.getOrComputeIfAbsent(namespace, key, __ -> {
				throw new RuntimeException("boom");
			}));
			assertSame(e, assertThrows(RuntimeException.class, () -> store.get(namespace, key)));
			assertSame(e, assertThrows(RuntimeException.class, () -> store.remove(namespace, key)));
		}

		@Test
		void computeIfAbsentWithExceptionThrowingCreatorFunction() {
			assertThrows(RuntimeException.class, () -> store.computeIfAbsent(namespace, key, __ -> {
				throw new RuntimeException("boom");
			}));
			assertNull(store.get(namespace, key));
			assertNull(store.remove(namespace, key));
		}

		@Test
		void removeWithTypeSafetyAndInvalidRequiredTypeThrowsException() {
			Integer key = 42;
			String value = "enigma";
			store.put(namespace, key, value);

			Exception exception = assertThrows(NamespacedHierarchicalStoreException.class,
				() -> store.remove(namespace, key, Number.class));
			assertEquals(
				"Object stored under key [42] is not of required type [java.lang.Number], but was [java.lang.String]: enigma",
				exception.getMessage());
		}

		@Test
		void removeWithTypeSafety() {
			Integer key = 42;
			String value = "enigma";
			store.put(namespace, key, value);

			// The fact that we can declare this as a String suffices for testing the required type.
			String removedValue = store.remove(namespace, key, String.class);
			assertEquals(value, removedValue);
			assertNull(store.get(namespace, key));
		}

		@SuppressWarnings("DataFlowIssue")
		@Test
		void removeWithTypeSafetyAndPrimitiveValueType() {
			String key = "enigma";
			int value = 42;
			store.put(namespace, key, value);

			// The fact that we can declare this as an int suffices for testing the required type.
			int requiredInt = store.remove(namespace, key, int.class);
			assertEquals(value, requiredInt);

			store.put(namespace, key, value);
			// The fact that we can declare this as an Integer suffices for testing the required type.
			Integer requiredInteger = store.get(namespace, key, Integer.class);
			assertEquals(value, requiredInteger.intValue());
		}

		@Test
		void removeNullValueWithTypeSafety() {
			Integer key = 42;
			store.put(namespace, key, null);

			// The fact that we can declare this as a String suffices for testing the required type.
			String removedValue = store.remove(namespace, key, String.class);
			assertNull(removedValue);
			assertNull(store.get(namespace, key));
		}

		@SuppressWarnings("deprecation")
		@Test
		void simulateRaceConditionInGetOrComputeIfAbsent() throws Exception {
			int threads = 10;
			AtomicInteger counter = new AtomicInteger();
			List<Object> values;

			try (var localStore = new NamespacedHierarchicalStore<>(null)) {
				values = executeConcurrently(threads, //
					() -> requireNonNull(
						localStore.getOrComputeIfAbsent(namespace, key, it -> counter.incrementAndGet())));
			}

			assertEquals(1, counter.get());
			assertThat(values).hasSize(threads).containsOnly(1);
		}

		@Test
		void simulateRaceConditionInComputeIfAbsent() throws Exception {
			int threads = 10;
			AtomicInteger counter = new AtomicInteger();
			List<Object> values;

			try (var localStore = new NamespacedHierarchicalStore<>(null)) {
				values = executeConcurrently(threads, //
					() -> requireNonNull(localStore.computeIfAbsent(namespace, key, it -> counter.incrementAndGet())));
			}

			assertEquals(1, counter.get());
			assertThat(values).hasSize(threads).containsOnly(1);
		}
	}

	@Nested
	class InheritedValuesTests {

		@Test
		void valueFromParentIsVisible() {
			parentStore.put(namespace, key, value);
			assertEquals(value, store.get(namespace, key));
		}

		@Test
		void valueFromParentCanBeOverriddenInChild() {
			parentStore.put(namespace, key, value);

			Object otherValue = new Object();
			store.put(namespace, key, otherValue);
			assertEquals(otherValue, store.get(namespace, key));

			assertEquals(value, parentStore.get(namespace, key));
		}
	}

	@Nested
	class CompositeNamespaceTests {

		@Test
		void additionNamespacePartMakesADifference() {

			String ns1 = "part1/part2";
			String ns2 = "part1";

			Object value2 = createObject("value2");

			parentStore.put(ns1, key, value);
			parentStore.put(ns2, key, value2);

			assertEquals(value, store.get(ns1, key));
			assertEquals(value2, store.get(ns2, key));
		}

	}

	@Nested
	class CloseActionTests {

		@BeforeEach
		void prerequisites() {
			assertNotClosed();
		}

		@Test
		void callsCloseActionInReverseInsertionOrderWhenClosingStore() throws Throwable {
			store.put(namespace, "key1", "value1");
			store.put(namespace, "key2", "value2");
			store.put(namespace, "key3", "value3");
			verifyNoInteractions(closeAction);

			store.close();
			assertClosed();

			var inOrder = inOrder(closeAction);
			inOrder.verify(closeAction).close(namespace, "key3", "value3");
			inOrder.verify(closeAction).close(namespace, "key2", "value2");
			inOrder.verify(closeAction).close(namespace, "key1", "value1");

			verifyNoMoreInteractions(closeAction);
		}

		@Test
		void doesNotCallCloseActionForRemovedValues() {
			store.put(namespace, key, value);
			store.remove(namespace, key);

			store.close();
			assertClosed();

			verifyNoInteractions(closeAction);
		}

		@Test
		void doesNotCallCloseActionForReplacedValues() throws Throwable {
			store.put(namespace, key, "value1");
			store.put(namespace, key, "value2");

			store.close();
			assertClosed();

			verify(closeAction).close(namespace, key, "value2");
			verifyNoMoreInteractions(closeAction);
		}

		@Test
		void doesNotCallCloseActionForNullValues() {
			store.put(namespace, key, null);

			store.close();
			assertClosed();

			verifyNoInteractions(closeAction);
		}

		@SuppressWarnings("deprecation")
		@Test
		void doesNotCallCloseActionForValuesThatThrowExceptionsDuringCleanup() throws Throwable {
			store.put(namespace, "key1", "value1");
			assertThrows(RuntimeException.class, () -> store.computeIfAbsent(namespace, "key2", __ -> {
				throw new RuntimeException("boom");
			}));
			assertThrows(RuntimeException.class, () -> store.getOrComputeIfAbsent(namespace, "key2", __ -> {
				throw new RuntimeException("boom");
			}));
			store.put(namespace, "key3", "value3");

			store.close();
			assertClosed();

			var inOrder = inOrder(closeAction);
			inOrder.verify(closeAction).close(namespace, "key3", "value3");
			inOrder.verify(closeAction).close(namespace, "key1", "value1");
			inOrder.verifyNoMoreInteractions();
		}

		@SuppressWarnings("deprecation")
		@Test
		void abortsCloseIfAnyStoredValueThrowsAnUnrecoverableExceptionDuringCleanup() throws Throwable {
			store.put(namespace, "key1", "value1");
			assertThrows(OutOfMemoryError.class, () -> store.getOrComputeIfAbsent(namespace, "key2", __ -> {
				throw new OutOfMemoryError("boom");
			}));
			store.put(namespace, "key3", "value3");

			assertThrows(OutOfMemoryError.class, store::close);
			assertClosed();

			verifyNoInteractions(closeAction);

			store.close();
			assertClosed();
		}

		@Test
		void closesStoreEvenIfCloseActionThrowsException() throws Throwable {
			store.put(namespace, key, value);
			doThrow(IllegalStateException.class).when(closeAction).close(namespace, key, value);

			assertThrows(IllegalStateException.class, store::close);
			assertClosed();

			verify(closeAction).close(namespace, key, value);
			verifyNoMoreInteractions(closeAction);

			store.close();
			assertClosed();
		}

		@Test
		void closesStoreEvenIfCloseActionThrowsUnrecoverableException() throws Throwable {
			store.put(namespace, key, value);
			doThrow(OutOfMemoryError.class).when(closeAction).close(namespace, key, value);

			assertThrows(OutOfMemoryError.class, store::close);
			assertClosed();

			verify(closeAction).close(namespace, key, value);
			verifyNoMoreInteractions(closeAction);

			store.close();
			assertClosed();
		}

		@Test
		void closesStoreEvenIfNoCloseActionIsConfigured() {
			@SuppressWarnings("resource")
			var localStore = new NamespacedHierarchicalStore<>(null);
			assertThat(localStore.isClosed()).isFalse();
			localStore.close();
			assertThat(localStore.isClosed()).isTrue();
		}

		@Test
		void closeIsIdempotent() throws Throwable {
			store.put(namespace, key, value);

			verifyNoInteractions(closeAction);

			store.close();
			assertClosed();

			verify(closeAction, times(1)).close(namespace, key, value);

			store.close();
			assertClosed();

			verifyNoMoreInteractions(closeAction);
		}

		/**
		 * @see <a href="https://github.com/junit-team/junit-framework/issues/3944">#3944</a>
		 */
		@SuppressWarnings("deprecation")
		@Test
		void acceptsQueryAfterClose() {
			store.put(namespace, key, value);
			store.close();
			assertClosed();

			assertThat(store.get(namespace, key)).isEqualTo(value);
			assertThat(store.get(namespace, key, String.class)).isEqualTo(value);
			assertThat(store.getOrComputeIfAbsent(namespace, key, __ -> "new")).isEqualTo(value);
			assertThat(store.getOrComputeIfAbsent(namespace, key, __ -> "new", String.class)).isEqualTo(value);
			assertThat(store.computeIfAbsent(namespace, key, __ -> "new")).isEqualTo(value);
			assertThat(store.computeIfAbsent(namespace, key, __ -> "new", String.class)).isEqualTo(value);
		}

		@SuppressWarnings("deprecation")
		@Test
		void rejectsModificationAfterClose() {
			store.close();
			assertClosed();

			assertThrows(NamespacedHierarchicalStoreException.class, () -> store.put(namespace, key, value));
			assertThrows(NamespacedHierarchicalStoreException.class, () -> store.remove(namespace, key));
			assertThrows(NamespacedHierarchicalStoreException.class, () -> store.remove(namespace, key, int.class));

			// Since key does not exist, an invocation of getOrComputeIfAbsent(...) or computeIfAbsent(...) will attempt
			// to compute a new value.
			assertThrows(NamespacedHierarchicalStoreException.class,
				() -> store.getOrComputeIfAbsent(namespace, key, __ -> "new"));
			assertThrows(NamespacedHierarchicalStoreException.class,
				() -> store.getOrComputeIfAbsent(namespace, key, __ -> "new", String.class));
			assertThrows(NamespacedHierarchicalStoreException.class,
				() -> store.computeIfAbsent(namespace, key, __ -> "new"));
			assertThrows(NamespacedHierarchicalStoreException.class,
				() -> store.computeIfAbsent(namespace, key, __ -> "new", String.class));
		}

		private void assertNotClosed() {
			assertThat(store.isClosed()).as("closed").isFalse();
		}

		private void assertClosed() {
			assertThat(store.isClosed()).as("closed").isTrue();
		}

	}

	private static Object createObject(String display) {
		return new Object() {

			@Override
			public String toString() {
				return display;
			}
		};
	}

	/**
	 * Helper class that forces hash collisions in ConcurrentHashMap.
	 * This ensures different keys end up in the same bucket, exposing
	 * potential deadlocks when map locks are held.
	 */
	private static final class CollidingKey {
		private final int value;

		CollidingKey(int value) {
			this.value = value;
		}

		@Override
		public boolean equals(Object o) {
			return o instanceof CollidingKey && this.value == ((CollidingKey) o).value;
		}

		@Override
		public int hashCode() {
			// Force all CollidingKey instances to have the same hash code
			return 42;
		}
	}

	/**
	 * <a href="https://github.com/junit-team/junit-framework/issues/5171">#5171</a>
	 * <a href="https://github.com/junit-team/junit-framework/pull/5209">#5209</a>
	 */
	@Nested
	class ConcurrencyIssue5171 {

		@Test
		void computeIfAbsentDoesNotDeadlockWithCollidingKeys() throws Exception {
			var store = new NamespacedHierarchicalStore<String>(null);
			var latch1 = new CountDownLatch(1);
			var latch2 = new CountDownLatch(1);

			var thread1 = new Thread(() -> store.computeIfAbsent("ns", new CollidingKey(1), key -> {
				latch1.countDown();
				try {
					// Wait for second thread to start its computation
					latch2.await();
				}
				catch (InterruptedException e) {
					Thread.currentThread().interrupt();
					throw new RuntimeException(e);
				}
				return "value1";
			}));

			var thread2 = new Thread(() -> {
				try {
					// Wait for first thread to start its computation
					latch1.await();
					store.computeIfAbsent("ns", new CollidingKey(2), key -> {
						latch2.countDown();
						return "value2";
					});
				}
				catch (InterruptedException e) {
					Thread.currentThread().interrupt();
					throw new RuntimeException(e);
				}
			});

			thread1.start();
			thread2.start();

			// Wait with timeout to detect deadlocks
			thread1.join(5000);
			thread2.join(5000);

			assertFalse(thread1.isAlive(), "Thread1 should have completed (no deadlock)");
			assertFalse(thread2.isAlive(), "Thread2 should have completed (no deadlock)");
		}

		@Test
		void computeIfAbsentWithNullParentValueAndLocalNullValue() {
			var parent = new NamespacedHierarchicalStore<String>(null);
			var child = parent.newChild();

			// Parent has null value
			parent.put("ns", "key", null);

			// Child also has null value (overrides parent null)
			child.put("ns", "key", null);

			// computeIfAbsent should compute new value even though child has null
			var counter = new AtomicInteger();
			var result = child.computeIfAbsent("ns", "key", __ -> {
				counter.incrementAndGet();
				return "computed-value";
			});

			assertEquals("computed-value", result);
			assertEquals(1, counter.get());
			assertEquals("computed-value", child.get("ns", "key"));
			// Parent should still have null
			assertNull(parent.get("ns", "key"));
		}

		@Test
		void computeIfAbsentWithConcurrentNullValueResolution() throws Exception {
			int threadCount = 5;
			var store = new NamespacedHierarchicalStore<String>(null);
			var values = synchronizedList(new ArrayList<String>());
			var nullCount = new AtomicInteger();

			List<Thread> threads = new ArrayList<>();
			for (int i = 0; i < threadCount; i++) {
				threads.add(new Thread(() -> {
					var value = store.computeIfAbsent("ns", "key", __ -> {
						// Return null sometimes to test null handling
						if (nullCount.incrementAndGet() == 1) {
							return "null";
						}
						return "non-null-value";
					});
					values.add(value.toString());
				}));
			}

			threads.forEach(Thread::start);
			for (Thread thread : threads) {
				thread.join();
			}

			// First thread's creator might return null, causing exception
			// Subsequent threads should retry and get a non-null value
			assertThat(values).contains("null");
			assertThat(values).hasSize(5);
		}

		@Test
		void computeIfAbsentRemovesFailedEntryOnException() {
			var store = new NamespacedHierarchicalStore<String>(null);
			var exceptionCount = new AtomicInteger();

			// First call throws exception
			assertThrows(RuntimeException.class, () -> store.computeIfAbsent("ns", "key", __ -> {
				exceptionCount.incrementAndGet();
				throw new RuntimeException("First attempt fails");
			}));

			// Second call should succeed (failed entry was removed)
			assertDoesNotThrow(() -> {
				var result = store.computeIfAbsent("ns", "key", __ -> "success");
				assertEquals("success", result);
			});

			assertEquals(1, exceptionCount.get());
			assertEquals("success", store.get("ns", "key"));
		}

		@Test
		void computeIfAbsentWithInterruptedThreadDoesNotLeaveCorruptState() throws Exception {
			var store = new NamespacedHierarchicalStore<String>(null);
			var latch = new CountDownLatch(1);

			var thread = new Thread(() -> {
				try {
					store.computeIfAbsent("ns", "key", __ -> {
						latch.countDown();
						try {
							Thread.sleep(10000); // Sleep indefinitely
						}
						catch (InterruptedException e) {
							Thread.currentThread().interrupt();
							throw new RuntimeException("Interrupted during computation");
						}
						return "value";
					});
				}
				catch (RuntimeException e) {
					// Expected interruption
				}
			});

			thread.start();
			latch.await(); // Wait for thread to start computation
			Thread.sleep(100); // Give it a moment to get into the sleep
			thread.interrupt();
			thread.join(5000);

			// After interruption, another thread should be able to compute
			assertDoesNotThrow(() -> {
				var result = store.computeIfAbsent("ns", "key", __ -> "new-value");
				assertEquals("new-value", result);
			});
		}

		@Test
		void computeIfAbsentWithSameKeyDifferentNamespacesConcurrently() throws Exception {
			int threadCount = 10;
			var store = new NamespacedHierarchicalStore<String>(null);
			var counters = new AtomicInteger[2];
			counters[0] = new AtomicInteger();
			counters[1] = new AtomicInteger();

			List<Thread> threads = new ArrayList<>();
			for (int i = 0; i < threadCount; i++) {
				final int namespaceIndex = i % 2;
				threads.add(new Thread(() -> {
					var value = store.computeIfAbsent("ns" + namespaceIndex, "key",
						__ -> "value-" + namespaceIndex + "-" + counters[namespaceIndex].incrementAndGet());
					assertEquals("value-" + namespaceIndex + "-1", value);
				}));
			}

			threads.forEach(Thread::start);
			for (Thread thread : threads) {
				thread.join();
			}

			// Each namespace should have been initialized only once
			assertEquals(1, counters[0].get());
			assertEquals(1, counters[1].get());
			assertEquals("value-0-1", store.get("ns0", "key"));
			assertEquals("value-1-1", store.get("ns1", "key"));
		}

		@Test
		void computeIfAbsentWithHeavyContentionAndDifferentBuckets() throws Exception {
			int threadCount = 20;
			var store = new NamespacedHierarchicalStore<String>(null);
			var counters = new AtomicInteger[threadCount];
			for (int i = 0; i < threadCount; i++) {
				counters[i] = new AtomicInteger();
			}

			List<Thread> threads = new ArrayList<>();
			for (int i = 0; i < threadCount; i++) {
				final int keyIndex = i;
				threads.add(new Thread(() -> {
					var value = store.computeIfAbsent("ns", "key" + keyIndex,
						__ -> "value-" + keyIndex + "-" + counters[keyIndex].incrementAndGet());
					assertEquals("value-" + keyIndex + "-1", value);
				}));
			}

			threads.forEach(Thread::start);
			for (Thread thread : threads) {
				thread.join();
			}

			// Each key should have been initialized only once
			for (int i = 0; i < threadCount; i++) {
				assertEquals(1, counters[i].get());
				assertEquals("value-" + i + "-1", store.get("ns", "key" + i));
			}
		}

		@Test
		void computeIfAbsentWithRecursiveComputationInDifferentNamespace() {
			var store = new NamespacedHierarchicalStore<String>(null);

			// Test that computing in one namespace doesn't block computing in another
			// even when the computations are recursive
			Function<String, String> recursiveCreator1 = key -> {
				store.computeIfAbsent("ns2", key, k -> "ns2-value");
				return "ns1-value";
			};

			Function<String, String> recursiveCreator2 = key -> {
				store.computeIfAbsent("ns1", key, k -> "ns1-value");
				return "ns2-value";
			};

			// Should not deadlock
			assertDoesNotThrow(() -> {
				var result1 = store.computeIfAbsent("ns1", "key", recursiveCreator1);
				var result2 = store.computeIfAbsent("ns2", "key", recursiveCreator2);
				assertEquals("ns1-value", result1);
				assertEquals("ns2-value", result2);
			});
		}

		@Test
		void computeIfAbsentPreservesOrderOfOperations() throws Exception {
			var store = new NamespacedHierarchicalStore<String>(null);
			var order = new ArrayList<String>();
			var latch = new CountDownLatch(1);

			Thread thread1 = new Thread(() -> {
				store.computeIfAbsent("ns", "key", __ -> {
					order.add("thread1-compute-start");
					try {
						latch.await();
					}
					catch (InterruptedException e) {
						Thread.currentThread().interrupt();
					}
					order.add("thread1-compute-end");
					return "value1";
				});
				order.add("thread1-done");
			});

			Thread thread2 = new Thread(() -> {
				order.add("thread2-start");
				latch.countDown();
				var result = store.computeIfAbsent("ns", "key", __ -> {
					order.add("thread2-compute");
					return "value2";
				});
				order.add("thread2-done");
			});

			thread1.start();
			Thread.sleep(50); // Ensure thread1 starts first
			thread2.start();

			thread1.join(5000);
			thread2.join(5000);

			// Verify thread2 waits for thread1 to complete
			// thread1-compute-start should happen first
			// thread2-start can happen while thread1 is waiting
			// thread1-compute-end should happen before thread2-done
			assertThat(order.indexOf("thread1-compute-start")).isLessThan(order.indexOf("thread1-compute-end"));
			assertThat(order.indexOf("thread1-compute-end")).isLessThan(order.indexOf("thread2-done"));

			// thread2 should not execute its compute function
			assertThat(order).doesNotContain("thread2-compute");
		}

		@Test
		void computeIfAbsentWithExceptionInMemoizingSupplierPropagation() {
			var store = new NamespacedHierarchicalStore<String>(null);

			// First call installs a MemoizingSupplier that throws, then removes it
			assertThrows(RuntimeException.class, () -> store.computeIfAbsent("ns", "key", __ -> {
				throw new RuntimeException("Boom!");
			}));

			// Subsequent calls should NOT get the same exception - the failed entry was removed
			// They should be able to retry the computation
			var exception2 = assertThrows(RuntimeException.class, () -> store.computeIfAbsent("ns", "key", __ -> {
				throw new RuntimeException("Boom again!");
			}));
			assertEquals("Boom again!", exception2.getMessage());

			// Since the entry was removed, get should return null
			assertNull(store.get("ns", "key"));

			// Remove should return null since nothing is stored
			assertNull(store.remove("ns", "key"));

			// Now a successful computation should work
			var result = store.computeIfAbsent("ns", "key", __ -> "success");
			assertEquals("success", result);
			assertEquals("success", store.get("ns", "key"));
		}

	}

	@SuppressWarnings("deprecation")
	@Test
	void getOrComputeIfAbsentDoesNotDeadlockWithCollidingKeys() throws Exception {
		var store = new NamespacedHierarchicalStore<String>(null);
		var latch1 = new CountDownLatch(1);
		var latch2 = new CountDownLatch(1);

		var thread1 = new Thread(() -> {
			store.getOrComputeIfAbsent("ns", new CollidingKey(1), key -> {
				latch1.countDown();
				try {
					// Wait for second thread to start its computation
					latch2.await();
				}
				catch (InterruptedException e) {
					Thread.currentThread().interrupt();
					throw new RuntimeException(e);
				}
				return "value1";
			});
		});

		var thread2 = new Thread(() -> {
			try {
				// Wait for first thread to start its computation
				latch1.await();
				store.getOrComputeIfAbsent("ns", new CollidingKey(2), key -> {
					latch2.countDown();
					return "value2";
				});
			}
			catch (InterruptedException e) {
				Thread.currentThread().interrupt();
				throw new RuntimeException(e);
			}
		});

		thread1.start();
		thread2.start();

		// Wait with timeout to detect deadlocks
		thread1.join(5000);
		thread2.join(5000);

		assertFalse(thread1.isAlive(), "Thread1 should have completed (no deadlock)");
		assertFalse(thread2.isAlive(), "Thread2 should have completed (no deadlock)");
	}

	@Test
	void computeIfAbsentOverridesParentNullValue() {
		var parent = new NamespacedHierarchicalStore<String>(null);
		var child = parent.newChild();

		// Store null in parent
		parent.put("ns", "key", null);

		// Initially child should see null from parent
		assertNull(child.get("ns", "key"));

		// computeIfAbsent should treat null as "logically absent" and compute a new value
		var result = child.computeIfAbsent("ns", "key", __ -> "value");
		assertEquals("value", result);

		// Subsequent get should return the computed value
		assertEquals("value", child.get("ns", "key"));

		// Parent should still have null
		assertNull(parent.get("ns", "key"));
	}

	@Test
	void computeIfAbsentWithRecursiveStoreAccess() throws Exception {
		// This test simulates the AssertJ scenario where computeIfAbsent
		// calls functions that also access the store
		var store = new NamespacedHierarchicalStore<String>(null);

		var recursiveCounter = new AtomicInteger();

		// This mimics AssertJ's SoftAssertionsExtension where the creator
		// function also accesses the store
		Function<String, String> recursiveCreator = key -> {
			recursiveCounter.incrementAndGet();
			// Access store while computing value (like AssertJ does)
			store.put("other", "key", "nested");
			return "value";
		};

		// Should not throw "Recursive update" exception
		assertDoesNotThrow(() -> {
			var result = store.computeIfAbsent("ns", "key", recursiveCreator);
			assertEquals("value", result);
		});

		assertEquals(1, recursiveCounter.get());
		assertEquals("nested", store.get("other", "key"));
	}

	@Test
	void computeIfAbsentWithExceptionThrowingCreatorDoesNotLeaveCorruptState() {
		var store = new NamespacedHierarchicalStore<String>(null);

		RuntimeException exception = new RuntimeException("Boom!");

		// First call fails
		assertThrows(RuntimeException.class, () -> store.computeIfAbsent("ns", "key", __ -> {
			throw exception;
		}));

		// Subsequent calls should be able to retry
		assertDoesNotThrow(() -> {
			var result = store.computeIfAbsent("ns", "key", __ -> "success");
			assertEquals("success", result);
		});

		// Final state should be correct
		assertEquals("success", store.get("ns", "key"));
	}

	@Test
	void computeIfAbsentMaintainsAtomicInitializationUnderConcurrency() throws Exception {
		int threadCount = 10;
		var store = new NamespacedHierarchicalStore<String>(null);
		var counter = new AtomicInteger();
		var values = synchronizedList(new ArrayList<String>());

		List<Thread> threads = new ArrayList<>();
		for (int i = 0; i < threadCount; i++) {
			threads.add(new Thread(() -> {
				var value = store.computeIfAbsent("ns", "key", __ -> {
					// Simulate expensive initialization
					try {
						Thread.sleep(10);
					}
					catch (InterruptedException e) {
						Thread.currentThread().interrupt();
					}
					return "value-" + counter.incrementAndGet();
				});
				values.add((String) value);
			}));
		}

		threads.forEach(Thread::start);
		for (Thread thread : threads) {
			thread.join();
		}

		// Only one thread should have initialized the value
		assertEquals(1, counter.get());

		// All threads should get the same value
		var expectedValue = "value-1";
		assertEquals(threadCount, values.size());
		for (String value : values) {
			assertEquals(expectedValue, value);
		}
	}

	@Test
	void computeIfAbsentPreservesValueWhenParentHasNonNullValue() {
		var parent = new NamespacedHierarchicalStore<String>(null);
		var child = parent.newChild();

		// Parent has non-null value
		parent.put("ns", "key", "parent-value");

		// computeIfAbsent should return parent's value, not compute new one
		var counter = new AtomicInteger();
		var result = child.computeIfAbsent("ns", "key", __ -> {
			counter.incrementAndGet();
			return "child-value";
		});

		assertEquals("parent-value", result);
		assertEquals(0, counter.get()); // Creator should not be called
	}

	@Test
	void computeIfAbsentClosedStoreThrowsException() {
		var store = new NamespacedHierarchicalStore<String>(null);
		store.close();

		assertThrows(NamespacedHierarchicalStoreException.class,
			() -> store.computeIfAbsent("ns", "key", __ -> "value"));
	}

	@Test
	void computeIfAbsentWithTypeSafetyAndConcurrentAccess() throws Exception {
		int threadCount = 5;
		var store = new NamespacedHierarchicalStore<String>(null);
		var counter = new AtomicInteger();

		List<Thread> threads = new ArrayList<>();
		for (int i = 0; i < threadCount; i++) {
			threads.add(new Thread(() -> {
				var value = store.computeIfAbsent("ns", "key", __ -> counter.incrementAndGet(), Integer.class);
				assertNotNull(value);
				assertEquals(Integer.class, value.getClass());
			}));
		}

		threads.forEach(Thread::start);
		for (Thread thread : threads) {
			thread.join();
		}

		assertEquals(1, counter.get());
	}
}
