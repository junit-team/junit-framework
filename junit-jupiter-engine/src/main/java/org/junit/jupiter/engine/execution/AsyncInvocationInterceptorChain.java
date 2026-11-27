/*
 * Copyright 2015-2026 the original author or authors.
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v2.0 which
 * accompanies this distribution and is available at
 *
 * https://www.eclipse.org/legal/epl-v20.html
 */

package org.junit.jupiter.engine.execution;

import static java.util.stream.Collectors.joining;
import static org.apiguardian.api.API.Status.INTERNAL;

import java.util.List;
import java.util.ListIterator;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Stream;

import org.apiguardian.api.API;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.extension.AsyncInvocationInterceptor;
import org.junit.jupiter.api.extension.AsyncInvocationInterceptor.AsyncInvocation;
import org.junit.jupiter.api.extension.Extension;
import org.junit.jupiter.api.extension.InvocationInterceptor;
import org.junit.jupiter.engine.extension.ExtensionRegistry;
import org.junit.platform.commons.JUnitException;
import org.junit.platform.commons.logging.Logger;
import org.junit.platform.commons.logging.LoggerFactory;
import org.junit.platform.commons.util.ExceptionUtils;

/**
 * {@code AsyncInvocationInterceptorChain} is the asynchronous counterpart of
 * {@link InvocationInterceptorChain}: it keeps track of all
 * {@linkplain AsyncInvocationInterceptor async invocation interceptors} which
 * need to be applied to an invocation and folds them into a
 * {@link CompletionStage}.
 *
 * <p>The chain does not block: each link returns a {@link CompletionStage}, so
 * the execution lane can be suspended and resumed without parking a thread
 * while awaiting a test or lifecycle method that returns an asynchronously
 * completable signal.
 *
 * @since 6.2
 */
@API(status = INTERNAL, since = "6.2")
public class AsyncInvocationInterceptorChain {

	private static final Logger logger = LoggerFactory.getLogger(AsyncInvocationInterceptorChain.class);

	private static Stream<AsyncInvocationInterceptor> streamAsyncInvocationInterceptors(
			ExtensionRegistry extensionRegistry) {
		return extensionRegistry.stream(Extension.class) //
				.map(extension -> {
					if (extension instanceof AsyncInvocationInterceptor asyncInterceptor) {
						return asyncInterceptor;
					}
					if (extension instanceof InvocationInterceptor syncInterceptor) {
						return new SynchronousInvocationInterceptorAdapter(syncInterceptor);
					}
					return null;
				}) //
				.filter(it -> it != null);
	}

	public <T extends @Nullable Object> CompletionStage<T> invoke(AsyncInvocation<T> invocation,
			ExtensionRegistry extensionRegistry, InterceptorCall<T> call) {
		List<AsyncInvocationInterceptor> interceptors = streamAsyncInvocationInterceptors(extensionRegistry).toList();
		if (interceptors.isEmpty()) {
			return proceed(invocation);
		}
		return chainAndInvoke(invocation, call, interceptors);
	}

	private <T extends @Nullable Object> CompletionStage<T> chainAndInvoke(AsyncInvocation<T> invocation,
			InterceptorCall<T> call, List<AsyncInvocationInterceptor> interceptors) {

		ValidatingAsyncInvocation<T> validatingInvocation = new ValidatingAsyncInvocation<>(invocation, interceptors);
		AsyncInvocation<T> chainedInvocation = chainInterceptors(validatingInvocation, call, interceptors);
		return proceed(chainedInvocation).whenComplete((___, throwable) -> {
			if (throwable == null) {
				validatingInvocation.verifyInvokedAtLeastOnce();
			}
		});
	}

	private <T extends @Nullable Object> AsyncInvocation<T> chainInterceptors(AsyncInvocation<T> invocation,
			InterceptorCall<T> call, List<AsyncInvocationInterceptor> interceptors) {
		AsyncInvocation<T> result = invocation;
		ListIterator<AsyncInvocationInterceptor> iterator = interceptors.listIterator(interceptors.size());
		while (iterator.hasPrevious()) {
			AsyncInvocationInterceptor interceptor = iterator.previous();
			result = new InterceptedAsyncInvocation<>(result, call, interceptor);
		}
		return result;
	}

	private static <T extends @Nullable Object> CompletionStage<T> proceed(AsyncInvocation<T> invocation) {
		return invocation.proceedAsync();
	}

	/**
	 * An asynchronous invocation that confirms that {@link #proceedAsync()} or
	 * {@link #skip()} has been invoked (at least once).
	 */
	private static class ValidatingAsyncInvocation<T extends @Nullable Object> implements AsyncInvocation<T> {

		private final List<AsyncInvocationInterceptor> interceptors;
		private final AtomicBoolean invoked = new AtomicBoolean();
		private final AsyncInvocation<T> delegate;

		ValidatingAsyncInvocation(AsyncInvocation<T> delegate, List<AsyncInvocationInterceptor> interceptors) {
			this.delegate = delegate;
			this.interceptors = interceptors;
		}

		@Override
		public CompletionStage<T> proceedAsync() {
			invoked.set(true);
			return delegate.proceedAsync();
		}

		@Override
		public void skip() {
			invoked.set(true);
			delegate.skip();
		}

		void verifyInvokedAtLeastOnce() {
			if (!invoked.get()) {
				String interceptorClasses = interceptors.stream() //
						.map(Object::getClass) //
						.map(Class::getName) //
						.collect(joining(", "));
				throw new JUnitException(
					"Invocation of interceptor chain not invoked at least once: " + interceptorClasses);
			}
		}
	}

	/**
	 * An invocation with one additional interceptor applied.
	 */
	private record InterceptedAsyncInvocation<T extends @Nullable Object>(AsyncInvocation<T> invocation,
			InterceptorCall<T> call, AsyncInvocationInterceptor interceptor) implements AsyncInvocation<T> {

		@Override
		public CompletionStage<T> proceedAsync() {
			try {
				return call.apply(interceptor, invocation);
			}
			catch (Throwable t) {
				logger.error(t, () -> "Internal error: " + t.getMessage());
				ExceptionUtils.throwAsUncheckedException(t);
				return CompletableFuture.failedFuture(t);
			}
		}

		@Override
		public void skip() {
			invocation.skip();
		}
	}

	/**
	 * A call to an interceptor.
	 */
	@FunctionalInterface
	public interface InterceptorCall<T extends @Nullable Object> {

		CompletionStage<T> apply(AsyncInvocationInterceptor interceptor, AsyncInvocation<T> invocation)
				throws Throwable;
	}

}
