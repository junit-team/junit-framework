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

import static java.util.Objects.requireNonNull;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
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

import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
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

		@Test
		void simulateRaceConditionInComputeIfAbsentWithCollidingKeys() throws Exception {
			// 20 threads: 10 will access key1, 10 will access key2
			int threads = 20;
			int threadsPerKey = threads / 2;

			// Both keys have the same hashCode, forcing them into the same bucket
			var key1 = new CollidingKey("k1");
			var key2 = new CollidingKey("k2");
			var chooser = new AtomicInteger();

			// Track how many times each key's defaultCreator is invoked
			var creatorCallsForKey1 = new AtomicInteger();
			var creatorCallsForKey2 = new AtomicInteger();

			try (var localStore = new NamespacedHierarchicalStore<String>(null)) {
				executeConcurrently(threads, () -> {
					// Alternate between key1 and key2
					CollidingKey key = (chooser.getAndIncrement() % 2 == 0 ? key1 : key2);

					// Each key's value is an AtomicInteger counter
					AtomicInteger counter = (AtomicInteger) localStore.computeIfAbsent(namespace, key, __ -> {
						if (key.equals(key1)) {
							creatorCallsForKey1.incrementAndGet();
						}
						else {
							creatorCallsForKey2.incrementAndGet();
						}
						return new AtomicInteger();
					});

					// Each thread increments the shared counter for its key
					counter.incrementAndGet();
					return 1;
				});

				assertThat(creatorCallsForKey1.get()).as(
					"defaultCreator for key1 should be called exactly once").isEqualTo(1);
				assertThat(creatorCallsForKey2.get()).as(
					"defaultCreator for key2 should be called exactly once").isEqualTo(1);

				AtomicInteger counter1 = (AtomicInteger) requireNonNull(localStore.get(namespace, key1));
				AtomicInteger counter2 = (AtomicInteger) requireNonNull(localStore.get(namespace, key2));
				assertThat(counter1.get()).as("all %d threads for key1 should have incremented the same counter",
					threadsPerKey).isEqualTo(threadsPerKey);
				assertThat(counter2.get()).as("all %d threads for key2 should have incremented the same counter",
					threadsPerKey).isEqualTo(threadsPerKey);
			}
		}

		@Test
		void computeIfAbsentWithCollidingKeysDoesNotBlockConcurrentAccess() throws Exception {
			try (var localStore = new NamespacedHierarchicalStore<String>(null)) {
				var key1ComputationStarted = new CountDownLatch(1);
				var key2ComputationStarted = new CountDownLatch(1);
				var key1Result = new AtomicReference<Object>();
				var key2Result = new AtomicReference<Object>();
				var key2WasBlocked = new AtomicBoolean(false);

				Thread thread1 = new Thread(() -> {
					Object result = localStore.computeIfAbsent(namespace, new CollidingKey("key1"), __ -> {
						key1ComputationStarted.countDown();
						try {
							// Wait to ensure thread2 has a chance to start its computation
							if (!key2ComputationStarted.await(500, TimeUnit.MILLISECONDS)) {
								key2WasBlocked.set(true);
							}
						}
						catch (InterruptedException e) {
							Thread.currentThread().interrupt();
						}
						return "value1";
					});
					key1Result.set(result);
				});

				Thread thread2 = new Thread(() -> {
					try {
						key1ComputationStarted.await(1, TimeUnit.SECONDS);
					}
					catch (InterruptedException e) {
						Thread.currentThread().interrupt();
						return;
					}
					Object result = localStore.computeIfAbsent(namespace, new CollidingKey("key2"), __ -> {
						key2ComputationStarted.countDown();
						return "value2";
					});
					key2Result.set(result);
				});

				thread1.start();
				thread2.start();
				thread1.join(2000);
				thread2.join(2000);

				assertThat(key1Result.get()).as("key1 result").isEqualTo("value1");
				assertThat(key2Result.get()).as("key2 result").isEqualTo("value2");
				assertThat(key2WasBlocked).as(
					"computeIfAbsent for key2 should not be blocked by key1's defaultCreator").isFalse();
			}
		}

		@SuppressWarnings("deprecation")
		@Test
		void getOrComputeIfAbsentDoesNotDeadlockWithCollidingKeys() throws Exception {
			try (var localStore = new NamespacedHierarchicalStore<String>(null)) {
				var firstComputationStarted = new CountDownLatch(1);
				var secondComputationAllowedToFinish = new CountDownLatch(1);
				var firstThreadTimedOut = new AtomicBoolean(false);

				Thread first = new Thread(
					() -> localStore.getOrComputeIfAbsent(namespace, new CollidingKey("k1"), __ -> {
						firstComputationStarted.countDown();
						try {
							if (!secondComputationAllowedToFinish.await(200, TimeUnit.MILLISECONDS)) {
								firstThreadTimedOut.set(true);
							}
						}
						catch (InterruptedException e) {
							Thread.currentThread().interrupt();
						}
						return "value1";
					}));

				Thread second = new Thread(() -> {
					try {
						firstComputationStarted.await();
					}
					catch (InterruptedException e) {
						Thread.currentThread().interrupt();
					}
					localStore.getOrComputeIfAbsent(namespace, new CollidingKey("k2"), __ -> {
						secondComputationAllowedToFinish.countDown();
						return "value2";
					});
				});

				first.start();
				second.start();

				first.join(1000);
				second.join(1000);

				assertThat(firstThreadTimedOut).as(
					"getOrComputeIfAbsent should not block subsequent computations on colliding keys").isFalse();
			}
		}

		@Test
		void computeIfAbsentDoesNotDeadlockWithCollidingKeys() throws Exception {
			try (var localStore = new NamespacedHierarchicalStore<String>(null)) {
				var firstComputationStarted = new CountDownLatch(1);
				var secondComputationAllowedToFinish = new CountDownLatch(1);
				var firstThreadTimedOut = new AtomicBoolean(false);

				Thread first = new Thread(() -> localStore.computeIfAbsent(namespace, new CollidingKey("k1"), __ -> {
					firstComputationStarted.countDown();
					try {
						if (!secondComputationAllowedToFinish.await(200, TimeUnit.MILLISECONDS)) {
							firstThreadTimedOut.set(true);
						}
					}
					catch (InterruptedException e) {
						Thread.currentThread().interrupt();
					}
					return "value1";
				}));

				Thread second = new Thread(() -> {
					try {
						firstComputationStarted.await();
					}
					catch (InterruptedException e) {
						Thread.currentThread().interrupt();
					}
					localStore.computeIfAbsent(namespace, new CollidingKey("k2"), __ -> {
						secondComputationAllowedToFinish.countDown();
						return "value2";
					});
				});

				first.start();
				second.start();

				first.join(1000);
				second.join(1000);

				assertThat(firstThreadTimedOut).as(
					"computeIfAbsent should not block subsequent computations on colliding keys").isFalse();
			}
		}

		@Test
		void getDoesNotSeeTransientExceptionFromComputeIfAbsent() throws Exception {
			try (var localStore = new NamespacedHierarchicalStore<String>(null)) {
				var computeStarted = new CountDownLatch(1);
				var getCanProceed = new CountDownLatch(1);
				var computeCanThrow = new CountDownLatch(1);
				var exceptionSeenByGet = new AtomicBoolean(false);
				var getReturnedNull = new AtomicBoolean(false);

				Thread computeThread = new Thread(() -> {
					try {
						localStore.computeIfAbsent(namespace, key, __ -> {
							computeStarted.countDown();
							try {
								// Wait for the get thread to be ready
								computeCanThrow.await(1, TimeUnit.SECONDS);
							}
							catch (InterruptedException e) {
								Thread.currentThread().interrupt();
							}
							throw new RuntimeException("boom");
						});
					}
					catch (RuntimeException expected) {
						// Expected - the exception should propagate back to this thread
					}
					finally {
						getCanProceed.countDown();
					}
				});

				Thread getThread = new Thread(() -> {
					try {
						computeStarted.await(1, TimeUnit.SECONDS);
						// Signal compute thread to throw
						computeCanThrow.countDown();
						// Wait a brief moment for compute to throw and remove the entry
						getCanProceed.await(1, TimeUnit.SECONDS);
						// Now try to get the value
						Object result = localStore.get(namespace, key);
						if (result == null) {
							getReturnedNull.set(true);
						}
					}
					catch (RuntimeException e) {
						// If we see the exception, that's the bug we're testing for
						exceptionSeenByGet.set(true);
					}
					catch (InterruptedException e) {
						Thread.currentThread().interrupt();
					}
				});

				computeThread.start();
				getThread.start();

				computeThread.join(2000);
				getThread.join(2000);

				assertThat(exceptionSeenByGet).as(
					"get() should not see transient exception from failed computeIfAbsent").isFalse();
				assertThat(getReturnedNull).as(
					"get() should return null after computeIfAbsent fails and removes entry").isTrue();
			}
		}

		@Test
		void getConcurrentWithFailingComputeIfAbsentDoesNotSeeException() throws Exception {
			int iterations = 100;
			for (int i = 0; i < iterations; i++) {
				try (var localStore = new NamespacedHierarchicalStore<String>(null)) {
					var computeStarted = new CountDownLatch(1);
					var exceptionSeenByGet = new AtomicBoolean(false);

					Thread computeThread = new Thread(() -> {
						try {
							localStore.computeIfAbsent(namespace, key, __ -> {
								computeStarted.countDown();
								throw new RuntimeException("boom");
							});
						}
						catch (RuntimeException expected) {
							// Expected
						}
					});

					Thread getThread = new Thread(() -> {
						try {
							computeStarted.await(100, TimeUnit.MILLISECONDS);
							// Try to observe the transient state
							localStore.get(namespace, key);
						}
						catch (RuntimeException e) {
							exceptionSeenByGet.set(true);
						}
						catch (InterruptedException e) {
							Thread.currentThread().interrupt();
						}
					});

					computeThread.start();
					getThread.start();

					computeThread.join(500);
					getThread.join(500);

					assertThat(exceptionSeenByGet).as(
						"get() should not see transient exception from failed computeIfAbsent (iteration %d)",
						i).isFalse();
				}
			}
		}

		@Test
		void computeIfAbsentOverridesParentNullValue() {
			// computeIfAbsent must treat a null value from the parent store as logically absent,
			// so the child store can install and keep its own non-null value for the same key.
			try (var parent = new NamespacedHierarchicalStore<String>(null);
					var child = new NamespacedHierarchicalStore<String>(parent)) {

				parent.put(namespace, key, null);

				assertNull(parent.get(namespace, key));
				assertNull(child.get(namespace, key));

				Object childValue = child.computeIfAbsent(namespace, key, __ -> "value");

				assertEquals("value", childValue);
				assertEquals("value", child.get(namespace, key));
			}
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

	private static final class CollidingKey {

		private final String value;

		private CollidingKey(String value) {
			this.value = value;
		}

		@Override
		public int hashCode() {
			return 42;
		}

		@Override
		public boolean equals(Object obj) {
			if (this == obj) {
				return true;
			}
			if (!(obj instanceof CollidingKey other)) {
				return false;
			}
			return this.value.equals(other.value);
		}

		@Override
		public String toString() {
			return this.value;
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
}
