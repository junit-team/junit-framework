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

import static org.apiguardian.api.API.Status.EXPERIMENTAL;
import static org.apiguardian.api.API.Status.MAINTAINED;

import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.Callable;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

import org.apiguardian.api.API;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.junit.platform.commons.JUnitException;
import org.junit.platform.commons.annotation.Contract;

/**
 * A container object which may either contain a nullable value in case of
 * <em>success</em> or an exception in case of <em>failure</em>.
 *
 * <p>Instances of this class should be returned by methods instead of
 * {@link Optional} when callers might want to report the exception via logging
 * or by wrapping it in another exception at a later point in time, e.g. via
 * {@link #getOrThrow(Function)}.
 *
 * <p>Moreover, it makes it particularly convenient to attach follow-up actions
 * should the {@code Try} have been successful (cf. {@link #andThen} and
 * {@link #andThenTry}) or fallback actions should it not have been (cf.
 * {@link #orElse} and {@link #orElseTry}).
 *
 * @since 1.4
 */
@API(status = MAINTAINED, since = "1.4")
public abstract class Try<V extends @Nullable Object> {

	/**
	 * Call the supplied {@link Callable} and return a successful {@code Try}
	 * that contains the returned value or, in case an exception was thrown, a
	 * failed {@code Try} that contains the exception.
	 *
	 * @param action the action to try; must not be {@code null}
	 * @return a succeeded or failed {@code Try} depending on the outcome of the
	 * supplied action; never {@code null}
	 * @see #success(Object)
	 * @see #failure(Exception)
	 */
	public static <V extends @Nullable Object> Try<V> call(Callable<V> action) {
		checkNotNull(action, "action");
		return Try.of(() -> success(action.call()));
	}

	/**
	 * Convert the supplied value into a succeeded {@code Try}.
	 *
	 * @param value the value to wrap; potentially {@code null}
	 * @return a succeeded {@code Try} that contains the supplied value; never
	 * {@code null}
	 */
	public static <V extends @Nullable Object> Try<V> success(V value) {
		return new Success<>(value);
	}

	/**
	 * Convert the supplied exception into a failed {@code Try}.
	 *
	 * @param cause the exception to wrap; must not be {@code null}
	 * @return a failed {@code Try} that contains the supplied value; never
	 * {@code null}
	 */
	public static <V> Try<V> failure(Exception cause) {
		return new Failure<>(checkNotNull(cause, "cause"));
	}

	// Cannot use Preconditions due to package cycle
	@Contract("null, _ -> fail; !null, _ -> param1")
	private static <T> T checkNotNull(@Nullable T input, String title) {
		if (input == null) {
			// Cannot use PreconditionViolationException due to package cycle
			throw new JUnitException(title + " must not be null");
		}
		return input;
	}

	private static <V> Try<V> of(Callable<Try<V>> action) {
		try {
			return action.call();
		}
		catch (Exception e) {
			return failure(e);
		}
	}

	private Try() {
		/* no-op */
	}

	/**
	 * If this {@code Try} is a success, apply the supplied transformer to its
	 * value and return a new successful or failed {@code Try} depending on the
	 * transformer's outcome; if this {@code Try} is a failure, do nothing.
	 *
	 * @param transformer the transformer to try; must not be {@code null}
	 * @return a succeeded or failed {@code Try}; never {@code null}
	 */
	public abstract <U> Try<U> andThenTry(Transformer<V, U> transformer);

	/**
	 * If this {@code Try} is a success, apply the supplied function to its
	 * value and return the resulting {@code Try}; if this {@code Try} is a
	 * failure, do nothing.
	 *
	 * @param function the function to apply; must not be {@code null}
	 * @return a succeeded or failed {@code Try}; never {@code null}
	 */
	public abstract <U> Try<U> andThen(Function<V, Try<U>> function);

	/**
	 * If this {@code Try} is a failure, call the supplied action and return a
	 * new successful or failed {@code Try} depending on the action's outcome;
	 * if this {@code Try} is a success, do nothing.
	 *
	 * @param action the action to try; must not be {@code null}
	 * @return a succeeded or failed {@code Try}; never {@code null}
	 */
	public abstract Try<V> orElseTry(Callable<V> action);

	/**
	 * If this {@code Try} is a failure, call the supplied supplier and return
	 * the resulting {@code Try}; if this {@code Try} is a success, do nothing.
	 *
	 * @param supplier the supplier to call; must not be {@code null}
	 * @return a succeeded or failed {@code Try}; never {@code null}
	 */
	public abstract Try<V> orElse(Supplier<Try<V>> supplier);

	/**
	 * If this {@code Try} is a success, get the contained value; if this
	 * {@code Try} is a failure, throw the contained exception.
	 *
	 * @return the contained value, if available; potentially {@code null}
	 * @throws Exception if this {@code Try} is a failure
	 */
	public abstract V get() throws Exception;

	/**
	 * If this {@code Try} is a success, get the contained value; if this
	 * {@code Try} is a failure, throw the contained exception.
	 *
	 * @return the contained value, if available
	 * @throws Exception if this {@code Try} is a failure or the contained value
	 * is {@code null}
	 *
	 * @since 6.0
	 */
	@API(status = EXPERIMENTAL, since = "6.0")
	public final @NonNull V getNonNull() throws Exception {
		var value = get();
		return checkNotNull(value, "value");
	}

	/**
	 * If this {@code Try} is a success, get the contained value; if this
	 * {@code Try} is a failure, call the supplied {@link Function} with the
	 * contained exception and throw the resulting {@link Exception}.
	 *
	 * @param exceptionTransformer the transformer to be called with the
	 * contained exception, if available; must not be {@code null}
	 * @return the contained value, if available
	 * @throws E if this {@code Try} is a failure
	 */
	public abstract <E extends Exception> V getOrThrow(Function<? super Exception, E> exceptionTransformer) throws E;

	/**
	 * If this {@code Try} is a success, get the contained value; if this
	 * {@code Try} is a failure, call the supplied {@link Function} with the
	 * contained exception and throw the resulting {@link Exception}.
	 *
	 * @param exceptionTransformer the transformer to be called with the
	 * contained exception, if available; must not be {@code null}
	 * @return the contained value, if available and not {@code null}
	 * @throws E if this {@code Try} is a failure or the contained value
	 * is {@code null}
	 */
	@API(status = EXPERIMENTAL, since = "6.0")
	public final <E extends Exception> @NonNull V getNonNullOrThrow(
			Function<@Nullable Exception, E> exceptionTransformer) throws E {
		var value = getOrThrow(exceptionTransformer);
		if (value == null) {
			throw exceptionTransformer.apply(null);
		}
		return value;
	}

	/**
	 * If this {@code Try} is a success, call the supplied {@link Consumer} with
	 * the contained value; otherwise, do nothing.
	 *
	 * @param valueConsumer the consumer to be called with the contained value,
	 * if available; must not be {@code null}
	 * @return the same {@code Try} for method chaining
	 */
	public abstract Try<V> ifSuccess(Consumer<V> valueConsumer);

	/**
	 * If this {@code Try} is a failure, call the supplied {@link Consumer} with
	 * the contained exception; otherwise, do nothing.
	 *
	 * @param causeConsumer the consumer to be called with the contained
	 * exception, if available; must not be {@code null}
	 * @return the same {@code Try} for method chaining
	 */
	public abstract Try<V> ifFailure(Consumer<Exception> causeConsumer);

	/**
	 * If this {@code Try} is a failure, return an empty {@link Optional}; if
	 * this {@code Try} is a success, wrap the contained value using
	 * {@link Optional#ofNullable(Object)}.
	 *
	 * @return an {@link Optional}; never {@code null} but potentially
	 * <em>empty</em>
	 */
	public abstract Optional<V> toOptional();

	/**
	 * A transformer for values of type {@code S} to type {@code T}.
	 *
	 * <p>The {@code Transformer} interface is similar to {@link Function},
	 * except that a {@code Transformer} may throw an exception.
	 */
	@FunctionalInterface
	public interface Transformer<S extends @Nullable Object, T extends @Nullable Object> {

		/**
		 * Apply this transformer to the supplied value.
		 *
		 * @throws Exception if the transformation fails
		 */
		T apply(S value) throws Exception;

	}

	private static final class Success<V extends @Nullable Object> extends Try<V> {

		private final V value;

		Success(V value) {
			this.value = value;
		}

		@Override
		public <U> Try<U> andThenTry(Transformer<V, U> transformer) {
			checkNotNull(transformer, "transformer");
			return Try.call(() -> transformer.apply(this.value));
		}

		@Override
		public <U> Try<U> andThen(Function<V, Try<U>> function) {
			checkNotNull(function, "function");
			return Try.of(() -> function.apply(this.value));
		}

		@Override
		public Try<V> orElseTry(Callable<V> action) {
			// don't call action because this Try is a success
			return this;
		}

		@Override
		public Try<V> orElse(Supplier<Try<V>> supplier) {
			// don't call supplier because this Try is a success
			return this;
		}

		@Override
		public V get() {
			return this.value;
		}

		@Override
		public <E extends Exception> V getOrThrow(Function<? super Exception, E> exceptionTransformer) {
			// don't call exceptionTransformer because this Try is a success
			return this.value;
		}

		@Override
		public Try<V> ifSuccess(Consumer<V> valueConsumer) {
			checkNotNull(valueConsumer, "valueConsumer");
			valueConsumer.accept(this.value);
			return this;
		}

		@Override
		public Try<V> ifFailure(Consumer<Exception> causeConsumer) {
			// don't call causeConsumer because this Try was a success
			return this;
		}

		@Override
		public Optional<V> toOptional() {
			return Optional.ofNullable(this.value);
		}

		@Override
		public boolean equals(Object that) {
			if (this == that) {
				return true;
			}
			if (that == null || this.getClass() != that.getClass()) {
				return false;
			}
			return Objects.equals(this.value, ((Success<?>) that).value);
		}

		@Override
		public int hashCode() {
			return Objects.hash(value);
		}
	}

	private static final class Failure<V extends @Nullable Object> extends Try<V> {

		private final Exception cause;

		Failure(Exception cause) {
			this.cause = cause;
		}

		@Override
		public <U> Try<U> andThenTry(Transformer<V, U> transformer) {
			// don't call transformer because this Try is a failure
			return uncheckedCast();
		}

		@Override
		public <U> Try<U> andThen(Function<V, Try<U>> function) {
			// don't call function because this Try is a failure
			return uncheckedCast();
		}

		@SuppressWarnings("unchecked")
		private <U> Try<U> uncheckedCast() {
			return (Try<U>) this;
		}

		@Override
		public Try<V> orElseTry(Callable<V> action) {
			checkNotNull(action, "action");
			return Try.call(action);
		}

		@Override
		public Try<V> orElse(Supplier<Try<V>> supplier) {
			checkNotNull(supplier, "supplier");
			return Try.of(supplier::get);
		}

		@Contract(" -> fail")
		@Override
		public V get() throws Exception {
			throw this.cause;
		}

		@Contract("_ -> fail")
		@Override
		public <E extends Exception> V getOrThrow(Function<? super Exception, E> exceptionTransformer) throws E {
			checkNotNull(exceptionTransformer, "exceptionTransformer");
			throw exceptionTransformer.apply(this.cause);
		}

		@Override
		public Try<V> ifSuccess(Consumer<V> valueConsumer) {
			// don't call valueConsumer because this Try is a failure
			return this;
		}

		@Override
		public Try<V> ifFailure(Consumer<Exception> causeConsumer) {
			checkNotNull(causeConsumer, "causeConsumer");
			causeConsumer.accept(this.cause);
			return this;
		}

		@Override
		public Optional<V> toOptional() {
			return Optional.empty();
		}

		@Override
		public boolean equals(Object that) {
			if (this == that) {
				return true;
			}
			if (that == null || this.getClass() != that.getClass()) {
				return false;
			}
			return Objects.equals(this.cause, ((Failure<?>) that).cause);
		}

		@Override
		public int hashCode() {
			return Objects.hash(cause);
		}

	}

}
