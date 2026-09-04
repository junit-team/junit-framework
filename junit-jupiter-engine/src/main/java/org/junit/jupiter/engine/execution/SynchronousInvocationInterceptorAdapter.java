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

import static org.apiguardian.api.API.Status.EXPERIMENTAL;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

import org.apiguardian.api.API;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.extension.AsyncInvocationInterceptor;
import org.junit.jupiter.api.extension.AsyncInvocationInterceptor.AsyncInvocation;
import org.junit.jupiter.api.extension.DynamicTestInvocationContext;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.InvocationInterceptor;
import org.junit.jupiter.api.extension.InvocationInterceptor.Invocation;
import org.junit.jupiter.api.extension.ReflectiveInvocationContext;

/**
 * {@code SynchronousInvocationInterceptorAdapter} adapts a legacy
 * {@link InvocationInterceptor} to the {@link AsyncInvocationInterceptor}
 * contract by running the synchronous interceptor method and completing a stage
 * immediately. The underlying invocation is executed synchronously (blocking,
 * since a legacy interceptor cannot observe asynchronous completion) whenever
 * the legacy interceptor calls {@link Invocation#proceed()}.
 *
 * <p>This enables existing (deprecated) synchronous interceptors to operate
 * unchanged within the asynchronous invocation pipeline.
 *
 * @since 6.2
 */
@API(status = EXPERIMENTAL, since = "6.2")
public class SynchronousInvocationInterceptorAdapter implements AsyncInvocationInterceptor {

	private final InvocationInterceptor delegate;

	public SynchronousInvocationInterceptorAdapter(InvocationInterceptor delegate) {
		this.delegate = delegate;
	}

	@Override
	public <T> CompletionStage<T> interceptTestClassConstructorAsync(AsyncInvocation<T> invocation,
			ReflectiveInvocationContext<Constructor<T>> invocationContext, ExtensionContext extensionContext) {
		return defer(
			() -> delegate.<T> interceptTestClassConstructor(toSync(invocation), invocationContext, extensionContext));
	}

	@Override
	public CompletionStage<Void> interceptBeforeAllMethodAsync(AsyncInvocation<Void> invocation,
			ReflectiveInvocationContext<Method> invocationContext, ExtensionContext extensionContext) {
		return deferVoid(() -> {
			delegate.interceptBeforeAllMethod(toSyncNullable(invocation), invocationContext, extensionContext);
		});
	}

	@Override
	public CompletionStage<Void> interceptBeforeEachMethodAsync(AsyncInvocation<Void> invocation,
			ReflectiveInvocationContext<Method> invocationContext, ExtensionContext extensionContext) {
		return deferVoid(() -> {
			delegate.interceptBeforeEachMethod(toSyncNullable(invocation), invocationContext, extensionContext);
		});
	}

	@Override
	public CompletionStage<Void> interceptTestMethodAsync(AsyncInvocation<Void> invocation,
			ReflectiveInvocationContext<Method> invocationContext, ExtensionContext extensionContext) {
		return deferVoid(() -> {
			delegate.interceptTestMethod(toSyncNullable(invocation), invocationContext, extensionContext);
		});
	}

	@Override
	public <T extends @Nullable Object> CompletionStage<T> interceptTestFactoryMethodAsync(
			AsyncInvocation<T> invocation, ReflectiveInvocationContext<Method> invocationContext,
			ExtensionContext extensionContext) {
		return defer(
			() -> delegate.<T> interceptTestFactoryMethod(toSync(invocation), invocationContext, extensionContext));
	}

	@Override
	public CompletionStage<Void> interceptTestTemplateMethodAsync(AsyncInvocation<Void> invocation,
			ReflectiveInvocationContext<Method> invocationContext, ExtensionContext extensionContext) {
		return deferVoid(() -> {
			delegate.interceptTestTemplateMethod(toSyncNullable(invocation), invocationContext, extensionContext);
		});
	}

	@Override
	public CompletionStage<Void> interceptDynamicTestAsync(AsyncInvocation<Void> invocation,
			DynamicTestInvocationContext invocationContext, ExtensionContext extensionContext) {
		return deferVoid(() -> {
			delegate.interceptDynamicTest(toSyncNullable(invocation), invocationContext, extensionContext);
		});
	}

	@Override
	public CompletionStage<Void> interceptAfterEachMethodAsync(AsyncInvocation<Void> invocation,
			ReflectiveInvocationContext<Method> invocationContext, ExtensionContext extensionContext) {
		return deferVoid(() -> {
			delegate.interceptAfterEachMethod(toSyncNullable(invocation), invocationContext, extensionContext);
		});
	}

	@Override
	public CompletionStage<Void> interceptAfterAllMethodAsync(AsyncInvocation<Void> invocation,
			ReflectiveInvocationContext<Method> invocationContext, ExtensionContext extensionContext) {
		return deferVoid(() -> {
			delegate.interceptAfterAllMethod(toSyncNullable(invocation), invocationContext, extensionContext);
		});
	}

	private static <T> Invocation<T> toSync(AsyncInvocation<T> invocation) {
		return new Invocation<T>() {
			@Override
			@SuppressWarnings("NullAway")
			public T proceed() throws Throwable {
				return invocation.proceedAsync().toCompletableFuture().join();
			}
		};
	}

	private static <T extends @Nullable Object> Invocation<@Nullable T> toSyncNullable(AsyncInvocation<T> invocation) {
		return new Invocation<@Nullable T>() {
			@Override
			public @Nullable T proceed() throws Throwable {
				return invocation.proceedAsync().toCompletableFuture().join();
			}
		};
	}

	private static CompletionStage<Void> deferVoid(ThrowableRunnable runnable) {
		try {
			runnable.run();
			return completedStageOfVoid();
		}
		catch (Throwable t) {
			return CompletableFuture.failedFuture(t);
		}
	}

	@SuppressWarnings("NullAway")
	private static CompletionStage<Void> completedStageOfVoid() {
		return CompletableFuture.completedFuture(null);
	}

	private static <T extends @Nullable Object> CompletionStage<T> defer(ThrowableSupplier<@Nullable T> supplier) {
		try {
			return completedStageOf(supplier.get());
		}
		catch (Throwable t) {
			return CompletableFuture.failedFuture(t);
		}
	}

	@SuppressWarnings("NullAway")
	private static <T extends @Nullable Object> CompletionStage<T> completedStageOf(@Nullable T value) {
		return CompletableFuture.completedFuture(value);
	}

	@FunctionalInterface
	private interface ThrowableSupplier<T extends @Nullable Object> {

		@Nullable
		T get() throws Throwable;
	}

	@FunctionalInterface
	private interface ThrowableRunnable {

		void run() throws Throwable;
	}

}
