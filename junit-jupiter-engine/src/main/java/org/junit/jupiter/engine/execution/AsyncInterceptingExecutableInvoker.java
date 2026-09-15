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

import static org.apiguardian.api.API.Status.INTERNAL;
import static org.junit.jupiter.engine.execution.ParameterResolutionUtils.resolveParameters;

import java.lang.reflect.Method;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

import org.apiguardian.api.API;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.extension.AsyncInvocationInterceptor;
import org.junit.jupiter.api.extension.AsyncInvocationInterceptor.AsyncInvocation;
import org.junit.jupiter.api.extension.AsyncReturnValueHandler;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.ReflectiveInvocationContext;
import org.junit.jupiter.engine.extension.ExtensionRegistry;
import org.junit.jupiter.engine.support.AsyncReturnTypeSupport;
import org.junit.jupiter.engine.support.MethodReflectionUtils;

/**
 * {@code AsyncInterceptingExecutableInvoker} is the asynchronous counterpart of
 * {@link InterceptingExecutableInvoker}. It invokes a
 * {@link java.lang.reflect.Executable} while allowing
 * {@linkplain AsyncInvocationInterceptor async invocation interceptors} to
 * intercept the invocation without blocking a thread while the intercepted
 * executable performs asynchronous work.
 *
 * <p>Unlike the synchronous invoker, this invoker never parks the calling
 * thread on a {@link CompletionStage} returned by the intercepted method:
 * instead it composes the returned stage into the stage it produces.
 *
 * @since 6.2
 */
@API(status = INTERNAL, since = "6.2")
public final class AsyncInterceptingExecutableInvoker {

	private static final AsyncInvocationInterceptorChain interceptorChain = new AsyncInvocationInterceptorChain();

	/**
	 * Invoke the supplied {@code method}, returning a {@link CompletionStage}
	 * that completes once the invocation and (where applicable) the
	 * asynchronous work signaled by the method's return value have finished.
	 *
	 * @param method the method to invoke and resolve parameters for
	 * @param target the target on which the executable will be invoked; may be
	 * {@code null} for {@code static} methods
	 * @param extensionContext the current {@code ExtensionContext}
	 * @param extensionRegistry the {@code ExtensionRegistry} to retrieve
	 * {@code ParameterResolvers} from
	 * @param interceptorCall the {@link AsyncVoidMethodInterceptorCall} to
	 * dispatch the interceptors to
	 * @return a completion stage signaling termination of the invocation; never
	 * {@code null}
	 */
	public CompletionStage<Void> interceptMethodAsync(Method method, @Nullable Object target,
			ExtensionContext extensionContext, ExtensionRegistry extensionRegistry,
			AsyncVoidMethodInterceptorCall interceptorCall) {
		@Nullable
		Object[] arguments = resolveParameters(method, target, extensionContext, extensionRegistry);
		MethodInvocation<@Nullable Void> methodInvocation = new MethodInvocation<>(method, target, arguments);
		AsyncInvocation<Void> asyncInvocation = new AwaitingAsyncMethodInvocation(method, target, arguments,
			extensionRegistry.getExtensions(AsyncReturnValueHandler.class));
		return interceptorChain.invoke(asyncInvocation, extensionRegistry, (interceptor, wrapped) -> {
			// The AwaitingAsyncMethodInvocation, when reached, must ultimately
			// resolve parameters; the MethodInvocation already holds the resolved
			// arguments and acts as the ReflectiveInvocationContext for the call.
			return interceptorCall.apply(interceptor, wrapped, methodInvocation, extensionContext);
		});
	}

	/**
	 * Wraps a {@link MethodInvocation} so that interceptor methods operate on an
	 * {@link AsyncInvocation} and the produced stage completes once the
	 * asynchronous work signaled by the method's return value has finished.
	 */
	private static class AwaitingAsyncMethodInvocation implements AsyncInvocation<Void> {

		private final Method method;
		private final @Nullable Object target;
		private final @Nullable Object[] arguments;
		private final List<AsyncReturnValueHandler> asyncReturnValueHandlers;
		private @Nullable Object result;

		AwaitingAsyncMethodInvocation(Method method, @Nullable Object target, @Nullable Object[] arguments,
				List<AsyncReturnValueHandler> asyncReturnValueHandlers) {
			this.method = method;
			this.target = target;
			this.arguments = arguments;
			this.asyncReturnValueHandlers = asyncReturnValueHandlers;
		}

		@Override
		public CompletionStage<Void> proceedAsync() {
			@Nullable
			Object value = getOrInvoke();
			if (value == null) {
				return completedVoid();
			}
			AsyncReturnValueHandler handler = AsyncReturnTypeSupport.findHandler(value, method,
				asyncReturnValueHandlers);
			if (handler != null) {
				return await(handler.toCompletionStage(value));
			}
			if (value instanceof CompletionStage<?> stage) {
				return await(stage);
			}
			if (value instanceof java.util.concurrent.Future<?> future) {
				return awaitFuture(future);
			}
			return completedVoid();
		}

		private @Nullable Object getOrInvoke() {
			Object current = this.result;
			if (current == null) {
				current = MethodReflectionUtils.invoke(this.method, this.target, this.arguments);
				this.result = current;
			}
			return current;
		}

		private static CompletionStage<Void> await(CompletionStage<?> stage) {
			@SuppressWarnings("unchecked")
			CompletionStage<Object> cast = (CompletionStage<Object>) (Object) stage;
			return cast.thenCompose(__ -> completedVoid());
		}

		private static CompletionStage<Void> awaitFuture(java.util.concurrent.Future<?> future) {
			return CompletableFuture.completedFuture(null).thenCompose(__ -> {
				try {
					future.get();
				}
				catch (InterruptedException e) {
					Thread.currentThread().interrupt();
					return CompletableFuture.failedFuture(e);
				}
				catch (java.util.concurrent.ExecutionException e) {
					return CompletableFuture.failedFuture(e.getCause() != null ? e.getCause() : e);
				}
				return completedVoid();
			});
		}

		private static CompletionStage<Void> completedVoid() {
			return CompletedStageSupport.completedVoid();
		}
	}

	/**
	 * A functional interface for the call to be made to an
	 * {@link AsyncInvocationInterceptor}.
	 */
	@FunctionalInterface
	public interface AsyncVoidMethodInterceptorCall {

		CompletionStage<Void> apply(AsyncInvocationInterceptor interceptor, AsyncInvocation<Void> invocation,
				ReflectiveInvocationContext<Method> invocationContext, ExtensionContext extensionContext)
				throws Throwable;
	}

	/**
	 * Small internal helper for producing a completed {@link Nullable Void} stage
	 * without tripping NullAway.
	 */
	private static final class CompletedStageSupport {

		@SuppressWarnings("NullAway")
		private static CompletionStage<Void> completedVoid() {
			return CompletableFuture.completedFuture(null);
		}
	}

}
