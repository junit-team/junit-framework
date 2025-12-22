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

import static java.util.Comparator.comparing;
import static java.util.Objects.requireNonNull;
import static org.apiguardian.api.API.Status.DEPRECATED;
import static org.apiguardian.api.API.Status.EXPERIMENTAL;
import static org.apiguardian.api.API.Status.MAINTAINED;
import static org.junit.platform.commons.util.ExceptionUtils.throwAsUncheckedException;
import static org.junit.platform.commons.util.ReflectionUtils.getWrapperType;
import static org.junit.platform.commons.util.ReflectionUtils.isAssignableTo;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.FutureTask;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Supplier;

import org.apiguardian.api.API;
import org.jspecify.annotations.Nullable;
import org.junit.platform.commons.util.Preconditions;
import org.junit.platform.commons.util.UnrecoverableExceptions;

/**
 * {@code NamespacedHierarchicalStore} is a hierarchical, namespaced key-value store.
 *
 * <p>Its {@linkplain #close() closing} behavior can be customized by passing a
 * {@link CloseAction} to the
 * {@link #NamespacedHierarchicalStore(NamespacedHierarchicalStore, CloseAction)}
 * constructor.
 *
 * <p>This class is thread-safe. Please note, however, that thread safety is
 * not guaranteed while the {@link #close()} method is being invoked.
 *
 * @param <N> Namespace type
 * @since 1.10
 */
@API(status = MAINTAINED, since = "1.13.3")
public final class NamespacedHierarchicalStore<N> implements AutoCloseable {

	private final AtomicInteger insertOrderSequence = new AtomicInteger();

	private final ConcurrentMap<CompositeKey<N>, StoredValue> storedValues = new ConcurrentHashMap<>(4);

	private final @Nullable NamespacedHierarchicalStore<N> parentStore;

	private final @Nullable CloseAction<N> closeAction;

	private volatile boolean closed = false;

	/**
	 * Create a new store with the supplied parent.
	 *
	 * @param parentStore the parent store to use for lookups; may be {@code null}
	 */
	public NamespacedHierarchicalStore(@Nullable NamespacedHierarchicalStore<N> parentStore) {
		this(parentStore, null);
	}

	/**
	 * Create a new store with the supplied parent and close action.
	 *
	 * @param parentStore the parent store to use for lookups; may be {@code null}
	 * @param closeAction the action to be called for each stored value when this
	 * store is closed; may be {@code null}
	 */
	public NamespacedHierarchicalStore(@Nullable NamespacedHierarchicalStore<N> parentStore,
			@Nullable CloseAction<N> closeAction) {
		this.parentStore = parentStore;
		this.closeAction = closeAction;
	}

	/**
	 * Create a child store with this store as its parent and this store's close
	 * action.
	 */
	public NamespacedHierarchicalStore<N> newChild() {
		return new NamespacedHierarchicalStore<>(this, this.closeAction);
	}

	/**
	 * Returns the parent store of this {@code NamespacedHierarchicalStore}.
	 *
	 * <p>If this store does not have a parent, an empty {@code Optional} is returned.
	 *
	 * @return an {@code Optional} containing the parent store, or an empty {@code Optional} if there is no parent
	 * @since 1.13
	 */
	@API(status = EXPERIMENTAL, since = "6.0")
	public Optional<NamespacedHierarchicalStore<N>> getParent() {
		return Optional.ofNullable(this.parentStore);
	}

	/**
	 * Determine if this store has been {@linkplain #close() closed}.
	 *
	 * @return {@code true} if this store has been closed
	 * @since 1.11
	 * @see #close()
	 */
	@API(status = MAINTAINED, since = "1.13.3")
	public boolean isClosed() {
		return this.closed;
	}

	/**
	 * If a {@link CloseAction} is configured, it will be called with all successfully
	 * stored values in reverse insertion order.
	 *
	 * <p>Closing a store does not close its parent or any of its children.
	 *
	 * <p>Invocations of this method after the store has already been closed will
	 * be ignored.
	 *
	 * @see #isClosed()
	 */
	@Override
	public void close() {
		if (!this.closed) {
			try {
				if (this.closeAction != null) {
					List<Throwable> failures = new ArrayList<>();
					this.storedValues.entrySet().stream() //
							.map(e -> EvaluatedValue.createSafely(e.getKey(), e.getValue())) //
							.filter(Objects::nonNull) //
							.sorted(EvaluatedValue.REVERSE_INSERT_ORDER) //
							.forEach(it -> {
								try {
									it.close(this.closeAction);
								}
								catch (Throwable t) {
									UnrecoverableExceptions.rethrowIfUnrecoverable(t);
									failures.add(t);
								}
							});
					if (!failures.isEmpty()) {
						var iterator = failures.iterator();
						var throwable = iterator.next();
						iterator.forEachRemaining(throwable::addSuppressed);
						throw throwAsUncheckedException(throwable);
					}
				}
			}
			finally {
				this.closed = true;
			}
		}
	}

	/**
	 * Get the value stored for the supplied namespace and key in this store or
	 * the parent store, if present.
	 *
	 * @param namespace the namespace; never {@code null}
	 * @param key the key; never {@code null}
	 * @return the stored value; may be {@code null}
	 * @throws NamespacedHierarchicalStoreException if this store has already been
	 * closed
	 */
	public @Nullable Object get(N namespace, Object key) {
		StoredValue storedValue = getStoredValue(new CompositeKey<>(namespace, key));
		return StoredValue.evaluateIfNotNull(storedValue);
	}

	/**
	 * Get the value stored for the supplied namespace and key in this store or
	 * the parent store, if present, and cast it to the supplied required type.
	 *
	 * @param namespace the namespace; never {@code null}
	 * @param key the key; never {@code null}
	 * @param requiredType the required type of the value; never {@code null}
	 * @return the stored value; may be {@code null}
	 * @throws NamespacedHierarchicalStoreException if the stored value cannot
	 * be cast to the required type, or if this store has already been closed
	 */
	public <T> @Nullable T get(N namespace, Object key, Class<T> requiredType)
			throws NamespacedHierarchicalStoreException {
		Object value = get(namespace, key);
		return castToRequiredType(key, value, requiredType);
	}

	/**
	 * Get the value stored for the supplied namespace and key in this store or
	 * the parent store, if present, or call the supplied function to compute it.
	 *
	 * @param namespace the namespace; never {@code null}
	 * @param key the key; never {@code null}
	 * @param defaultCreator the function called with the supplied {@code key}
	 * to create a new value; never {@code null} but may return {@code null}
	 * @return the stored value; may be {@code null}
	 * @throws NamespacedHierarchicalStoreException if this store has already been
	 * closed
	 * @deprecated Please use {@link #computeIfAbsent(Object, Object, Function)} instead.
	 */
	@Deprecated(since = "6.0")
	@API(status = DEPRECATED, since = "6.0")
	public <K, V extends @Nullable Object> @Nullable Object getOrComputeIfAbsent(N namespace, K key,
			Function<? super K, ? extends V> defaultCreator) {
		Preconditions.notNull(defaultCreator, "defaultCreator must not be null");
		var compositeKey = new CompositeKey<>(namespace, key);
		var currentStoredValue = getStoredValue(compositeKey);
		if (currentStoredValue != null) {
			return currentStoredValue.evaluate();
		}
		var candidateStoredValue = newStoredSuppliedNullableValue(() -> {
			rejectIfClosed();
			return defaultCreator.apply(key);
		});
		var storedValue = storedValues.compute(compositeKey, //
			(__, oldStoredValue) -> {
				// guard against race conditions, repeated from getStoredValue
				// this filters out failures inserted by computeIfAbsent
				if (StoredValue.isNonNullAndPresent(oldStoredValue)) {
					return oldStoredValue;
				}
				rejectIfClosed();
				return candidateStoredValue;
			});

		// Only the caller that created the candidateStoredValue may run it
		if (candidateStoredValue.equals(storedValue)) {
			return candidateStoredValue.execute();
		}
		return storedValue.evaluate();
	}

	/**
	 * Return the value stored for the supplied namespace and key in this store
	 * or the parent store, if present and not {@code null}, or call the
	 * supplied function to compute it.
	 *
	 * @param namespace the namespace; never {@code null}
	 * @param key the key; never {@code null}
	 * @param defaultCreator the function called with the supplied {@code key}
	 * to create a new value; never {@code null} and must not return
	 * {@code null}
	 * @return the stored value; never {@code null}
	 * @throws NamespacedHierarchicalStoreException if this store has already been
	 * closed
	 * @since 6.0
	 */
	@API(status = MAINTAINED, since = "6.0")
	public <K, V> Object computeIfAbsent(N namespace, K key, Function<? super K, ? extends V> defaultCreator) {
		Preconditions.notNull(defaultCreator, "defaultCreator must not be null");
		var compositeKey = new CompositeKey<>(namespace, key);
		var currentStoredValue = getStoredValue(compositeKey);
		var result = StoredValue.evaluateIfNotNull(currentStoredValue);
		if (result != null) {
			return result;
		}
		var candidateStoredValue = newStoredSuppliedValue(() -> {
			rejectIfClosed();
			return Preconditions.notNull(defaultCreator.apply(key), "defaultCreator must not return null");
		});
		var storedValue = storedValues.compute(compositeKey, (__, oldStoredValue) -> {
			// guard against race conditions
			// computeIfAbsent replaces both null and absent values
			if (StoredValue.evaluateIfNotNull(oldStoredValue) != null) {
				return oldStoredValue;
			}
			rejectIfClosed();
			return candidateStoredValue;
		});

		// In a race condition either put, getOrComputeIfAbsent, or another
		// computeIfAbsent call put a non-null value in the store
		if (!candidateStoredValue.equals(storedValue)) {
			return requireNonNull(storedValue.evaluate());
		}
		// Only the caller that created the candidateStoredValue may run it
		// and see the exception.
		Object newResult = candidateStoredValue.execute();
		// DeferredOptionalValue is quite heavy, replace with lighter container
		if (candidateStoredValue.isPresent()) {
			storedValues.computeIfPresent(compositeKey, compareAndPut(storedValue, newStoredValue(newResult)));
		}
		return newResult;
	}

	private static <N> BiFunction<CompositeKey<N>, StoredValue, StoredValue> compareAndPut(StoredValue expectedValue,
			StoredValue newValue) {
		return (__, storedValue) -> {
			if (expectedValue.equals(storedValue)) {
				return newValue;
			}
			return storedValue;
		};
	}

	/**
	 * Get the value stored for the supplied namespace and key in this store or
	 * the parent store, if present, or call the supplied function to compute it
	 * and, finally, cast it to the supplied required type.
	 *
	 * @param namespace the namespace; never {@code null}
	 * @param key the key; never {@code null}
	 * @param defaultCreator the function called with the supplied {@code key}
	 * to create a new value; never {@code null} but may return {@code null}
	 * @param requiredType the required type of the value; never {@code null}
	 * @return the stored value; may be {@code null}
	 * @throws NamespacedHierarchicalStoreException if the stored value cannot
	 * be cast to the required type, or if this store has already been closed
	 * @deprecated Please use {@link #computeIfAbsent(Object, Object, Function, Class)} instead.
	 */
	@Deprecated(since = "6.0")
	@API(status = DEPRECATED, since = "6.0")
	public <K, V extends @Nullable Object> @Nullable V getOrComputeIfAbsent(N namespace, K key,
			Function<? super K, ? extends V> defaultCreator, Class<V> requiredType)
			throws NamespacedHierarchicalStoreException {

		Object value = getOrComputeIfAbsent(namespace, key, defaultCreator);
		return castToRequiredType(key, value, requiredType);
	}

	/**
	 * Return the value stored for the supplied namespace and key in this store
	 * or the parent store, if present and not {@code null}, or call the
	 * supplied function to compute it and, finally, cast it to the supplied
	 * required type.
	 *
	 * @param namespace the namespace; never {@code null}
	 * @param key the key; never {@code null}
	 * @param defaultCreator the function called with the supplied {@code key}
	 * to create a new value; never {@code null} and must not return
	 * {@code null}
	 * @param requiredType the required type of the value; never {@code null}
	 * @return the stored value; never {@code null}
	 * @throws NamespacedHierarchicalStoreException if the stored value cannot
	 * be cast to the required type, or if this store has already been closed
	 * @since 6.0
	 */
	@API(status = MAINTAINED, since = "6.0")
	public <K, V> V computeIfAbsent(N namespace, K key, Function<? super K, ? extends V> defaultCreator,
			Class<V> requiredType) throws NamespacedHierarchicalStoreException {

		Object value = computeIfAbsent(namespace, key, defaultCreator);
		return castNonNullToRequiredType(key, value, requiredType);
	}

	/**
	 * Put the supplied value for the supplied namespace and key into this
	 * store and return the previously associated value in this store.
	 *
	 * <p>The {@link CloseAction} will <em>not</em> be called for the previously
	 * stored value, if any.
	 *
	 * @param namespace the namespace; never {@code null}
	 * @param key the key; never {@code null}
	 * @param value the value to store; may be {@code null}
	 * @return the previously stored value; may be {@code null}
	 * @throws NamespacedHierarchicalStoreException if an error occurs while
	 * storing the value, or if this store has already been closed
	 */
	public @Nullable Object put(N namespace, Object key, @Nullable Object value)
			throws NamespacedHierarchicalStoreException {
		rejectIfClosed();
		StoredValue oldValue = this.storedValues.put(new CompositeKey<>(namespace, key), newStoredValue(value));
		return StoredValue.evaluateIfNotNull(oldValue);
	}

	/**
	 * Remove the value stored for the supplied namespace and key from this
	 * store.
	 *
	 * <p>The {@link CloseAction} will <em>not</em> be called for the removed
	 * value.
	 *
	 * @param namespace the namespace; never {@code null}
	 * @param key the key; never {@code null}
	 * @return the previously stored value; may be {@code null}
	 * @throws NamespacedHierarchicalStoreException if this store has already been
	 * closed
	 */
	public @Nullable Object remove(N namespace, Object key) {
		rejectIfClosed();
		StoredValue previous = this.storedValues.remove(new CompositeKey<>(namespace, key));
		return StoredValue.evaluateIfNotNull(previous);
	}

	/**
	 * Remove the value stored for the supplied namespace and key from this
	 * store and cast it to the supplied required type.
	 *
	 * <p>The {@link CloseAction} will <em>not</em> be called for the removed
	 * value.
	 *
	 * @param namespace the namespace; never {@code null}
	 * @param key the key; never {@code null}
	 * @param requiredType the required type of the value; never {@code null}
	 * @return the previously stored value; may be {@code null}
	 * @throws NamespacedHierarchicalStoreException if the stored value cannot
	 * be cast to the required type, or if this store has already been closed
	 */
	public <T> @Nullable T remove(N namespace, Object key, Class<T> requiredType)
			throws NamespacedHierarchicalStoreException {
		rejectIfClosed();
		Object value = remove(namespace, key);
		return castToRequiredType(key, value, requiredType);
	}

	private StoredValue.Value newStoredValue(@Nullable Object value) {
		var sequenceNumber = insertOrderSequence.getAndIncrement();
		return new StoredValue.Value(sequenceNumber, value);
	}

	private StoredValue.DeferredValue newStoredSuppliedNullableValue(Supplier<@Nullable Object> supplier) {
		var sequenceNumber = insertOrderSequence.getAndIncrement();
		return new StoredValue.DeferredValue(sequenceNumber, supplier);
	}

	private StoredValue.DeferredOptionalValue newStoredSuppliedValue(Supplier<Object> supplier) {
		var sequenceNumber = insertOrderSequence.getAndIncrement();
		return new StoredValue.DeferredOptionalValue(sequenceNumber, supplier);
	}

	private @Nullable StoredValue getStoredValue(CompositeKey<N> compositeKey) {
		StoredValue storedValue = this.storedValues.get(compositeKey);
		if (StoredValue.isNonNullAndPresent(storedValue)) {
			return storedValue;
		}
		if (this.parentStore != null) {
			return this.parentStore.getStoredValue(compositeKey);
		}
		return null;
	}

	private <T> @Nullable T castToRequiredType(Object key, @Nullable Object value, Class<T> requiredType) {
		Preconditions.notNull(requiredType, "requiredType must not be null");
		if (value == null) {
			return null;
		}
		return castNonNullToRequiredType(key, value, requiredType);
	}

	@SuppressWarnings("unchecked")
	private <T, V> T castNonNullToRequiredType(Object key, V value, Class<T> requiredType) {
		if (isAssignableTo(value, requiredType)) {
			if (requiredType.isPrimitive()) {
				return (T) requireNonNull(getWrapperType(requiredType)).cast(value);
			}
			return requiredType.cast(value);
		}
		// else
		throw new NamespacedHierarchicalStoreException(
			"Object stored under key [%s] is not of required type [%s], but was [%s]: %s".formatted(key,
				requiredType.getName(), value.getClass().getName(), value));
	}

	private void rejectIfClosed() {
		if (this.closed) {
			throw new NamespacedHierarchicalStoreException(
				"A NamespacedHierarchicalStore cannot be modified or queried after it has been closed");
		}
	}

	private record CompositeKey<N>(N namespace, Object key) {

		CompositeKey {
			Preconditions.notNull(namespace, "namespace must not be null");
			Preconditions.notNull(key, "key must not be null");
		}

	}

	private interface StoredValue {

		int order();

		@Nullable
		Object evaluate();

		boolean isPresent();

		static @Nullable Object evaluateIfNotNull(@Nullable StoredValue value) {
			return value != null ? value.evaluate() : null;
		}

		static boolean isNonNullAndPresent(@Nullable StoredValue value) {
			return value != null && value.isPresent();
		}

		/**
		 * May contain {@code null} or a value, never an exception.
		 */
		final class Value implements StoredValue {
			private final int order;
			private final @Nullable Object value;

			Value(int order, @Nullable Object value) {
				this.order = order;
				this.value = value;
			}

			@Override
			public @Nullable Object evaluate() {
				return value;
			}

			@Override
			public boolean isPresent() {
				return true;
			}

			@Override
			public int order() {
				return order;
			}
		}

		/**
		 * May eventually contain {@code null} or a value or an exception.
		 */
		final class DeferredValue implements StoredValue {
			private final int order;
			private final DeferredSupplier delegate;

			DeferredValue(int order, Supplier<@Nullable Object> delegate) {
				this.order = order;
				this.delegate = new DeferredSupplier(delegate);
			}

			@Override
			public @Nullable Object evaluate() {
				return delegate.getOrThrow();
			}

			@Override
			public boolean isPresent() {
				return true;
			}

			@Nullable
			Object execute() {
				delegate.run();
				return delegate.getOrThrow();
			}

			@Override
			public int order() {
				return order;
			}
		}

		/**
		 * May eventually contain a value or an exception, never {@code null}.
		 */
		final class DeferredOptionalValue implements StoredValue {
			private final int order;
			private final DeferredSupplier delegate;

			DeferredOptionalValue(int order, Supplier<Object> delegate) {
				this.order = order;
				this.delegate = new DeferredSupplier(delegate);
			}

			@Override
			public @Nullable Object evaluate() {
				return delegate.get();
			}

			@Override
			public boolean isPresent() {
				return evaluate() != null;
			}

			Object execute() {
				delegate.run();
				// Delegate does not produce null
				return requireNonNull(delegate.getOrThrow());
			}

			@Override
			public int order() {
				return order;
			}
		}
	}

	private record EvaluatedValue<N>(CompositeKey<N> compositeKey, int order, Object value) {

		private static <N> @Nullable EvaluatedValue<N> createSafely(CompositeKey<N> compositeKey, StoredValue value) {
			try {
				var evaluatedValue = value.evaluate();
				if (evaluatedValue == null) {
					return null;
				}
				return new EvaluatedValue<>(compositeKey, value.order(), evaluatedValue);
			}
			catch (Throwable t) {
				UnrecoverableExceptions.rethrowIfUnrecoverable(t);
				return null;
			}
		}

		private static final Comparator<EvaluatedValue<?>> REVERSE_INSERT_ORDER = comparing(
			(EvaluatedValue<?> it) -> it.order).reversed();

		private void close(CloseAction<N> closeAction) throws Throwable {
			closeAction.close(this.compositeKey.namespace, this.compositeKey.key, this.value);
		}

	}

	/**
	 * Deferred computation that can be added to the store.
	 * <p>
	 * This allows values to be computed outside the
	 * {@link ConcurrentHashMap#compute(Object, BiFunction)} calls and
	 * prevents recursive updates.
	 */
	static final class DeferredSupplier {

		private final FutureTask<@Nullable Object> task;

		DeferredSupplier(Supplier<?> delegate) {
			this.task = new FutureTask<>(delegate::get);
		}

		void run() {
			task.run();
		}

		@Nullable
		Object get() {
			try {
				return task.get();
			}
			catch (InterruptedException e) {
				Thread.currentThread().interrupt();
				throw throwAsUncheckedException(e);
			}
			catch (ExecutionException e) {
				// non-null guaranteed by FutureTask
				var cause = requireNonNull(e.getCause());
				UnrecoverableExceptions.rethrowIfUnrecoverable(cause);
				return null;
			}
		}

		@Nullable
		Object getOrThrow() {
			try {
				return task.get();
			}
			catch (InterruptedException e) {
				Thread.currentThread().interrupt();
				throw throwAsUncheckedException(e);
			}
			catch (ExecutionException e) {
				// non-null guaranteed by FutureTask
				var cause = requireNonNull(e.getCause());
				UnrecoverableExceptions.rethrowIfUnrecoverable(cause);
				throw throwAsUncheckedException(cause);
			}
		}
	}

	/**
	 * Called for each successfully stored non-null value in the store when a
	 * {@link NamespacedHierarchicalStore} is
	 * {@linkplain NamespacedHierarchicalStore#close() closed}.
	 */
	@FunctionalInterface
	public interface CloseAction<N> {

		/**
		 * Static factory method for creating a {@link CloseAction} which
		 * {@linkplain #close(Object, Object, Object) closes} any value that
		 * implements {@link AutoCloseable}.
		 *
		 * @since 6.0
		 */
		@API(status = EXPERIMENTAL, since = "6.0")
		static <N> CloseAction<N> closeAutoCloseables() {
			return (__, ___, value) -> {
				if (value instanceof AutoCloseable closeable) {
					closeable.close();
				}
			};
		}

		/**
		 * Close the supplied {@code value}.
		 *
		 * @param namespace the namespace; never {@code null}
		 * @param key the key; never {@code null}
		 * @param value the value; never {@code null}
		 */
		void close(N namespace, Object key, Object value) throws Throwable;

	}

}
