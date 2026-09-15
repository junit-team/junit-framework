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

import java.lang.reflect.Constructor;
import java.lang.reflect.Executable;
import java.lang.reflect.Method;
import java.util.List;
import java.util.concurrent.CompletionException;

import org.apiguardian.api.API;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.extension.AsyncReturnValueHandler;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.InvocationInterceptor;
import org.junit.jupiter.api.extension.InvocationInterceptor.Invocation;
import org.junit.jupiter.api.extension.ParameterResolver;
import org.junit.jupiter.api.extension.ReflectiveInvocationContext;
import org.junit.jupiter.engine.execution.InterceptingExecutableInvoker.ReflectiveInterceptorCall.VoidMethodInterceptorCall;
import org.junit.jupiter.engine.extension.ExtensionRegistry;
import org.junit.jupiter.engine.support.AsyncReturnTypeSupport;
import org.junit.platform.commons.util.ExceptionUtils;
import org.junit.platform.commons.util.UnrecoverableExceptions;

/**
 * {@code InterceptingExecutableInvoker} encapsulates the invocation of a
 * {@link java.lang.reflect.Executable} (i.e., method or constructor),
 * including support for dynamic resolution of method parameters via
 * {@link ParameterResolver ParameterResolvers}.
 *
 * @since 5.0
 */
@API(status = INTERNAL, since = "5.0")
public class InterceptingExecutableInvoker {

	private static final InvocationInterceptorChain interceptorChain = new InvocationInterceptorChain();

	/**
	 * Invoke the supplied constructor with the supplied outer instance and
	 * dynamic parameter resolution.
	 *
	 * @param constructor the constructor to invoke and resolve parameters for
	 * @param outerInstance the outer instance to supply as the first argument
	 * to the constructor; empty, for top-level classes
	 * @param extensionContext the current {@code ExtensionContext}
	 * @param extensionRegistry the {@code ExtensionRegistry} to retrieve
	 * {@code ParameterResolvers} from
	 * @param interceptorCall the call for intercepting this constructor
	 * invocation via all registered {@linkplain InvocationInterceptor
	 * interceptors}
	 */
	public <T> T invoke(Constructor<T> constructor, @Nullable Object outerInstance,
			ExtensionContextSupplier extensionContext, ExtensionRegistry extensionRegistry,
			ReflectiveInterceptorCall<Constructor<T>, T> interceptorCall) {

		@Nullable
		Object[] arguments = resolveParameters(constructor, null, outerInstance, extensionContext, extensionRegistry);
		ConstructorInvocation<T> invocation = new ConstructorInvocation<>(constructor, arguments);
		return invoke(invocation, invocation, extensionContext, extensionRegistry, interceptorCall);
	}

	public void invokeVoid(Method method, @Nullable Object target, ExtensionContext extensionContext,
			ExtensionRegistry extensionRegistry, VoidMethodInterceptorCall interceptorCall) {
		this.<@Nullable Void> invoke(method, target, extensionContext, extensionRegistry,
			ReflectiveInterceptorCall.ofVoidMethod(interceptorCall));
	}

	/**
	 * Invoke the supplied method and, if it returns an asynchronous completion
	 * signal (e.g. a {@link java.util.concurrent.CompletionStage}), await its
	 * completion.
	 *
	 * <p>The completion is awaited <em>within</em> the invocation that the
	 * {@linkplain InvocationInterceptor interceptor} chain sees, so interceptors
	 * such as {@code @Timeout} guard the entire asynchronous work rather than
	 * only the synchronous method call that returns the stage.
	 *
	 * @param method the method to invoke and resolve parameters for
	 * @param target the target on which the executable will be invoked
	 * @param extensionContext the current {@code ExtensionContext}
	 * @param extensionRegistry the {@code ExtensionRegistry} to retrieve
	 * {@code ParameterResolvers} from
	 * @param interceptorCall the call for intercepting this method invocation
	 * via all registered {@linkplain InvocationInterceptor interceptors}
	 */
	@SuppressWarnings("NullAway")
	public void invokeAndAwait(Method method, @Nullable Object target, ExtensionContext extensionContext,
			ExtensionRegistry extensionRegistry, VoidMethodInterceptorCall interceptorCall) {
		@Nullable
		Object[] arguments = resolveParameters(method, target, extensionContext, extensionRegistry);
		MethodInvocation<Object> capturing = new MethodInvocation<>(method, target, arguments) {
			@Override
			public Object proceed() {
				Object value = super.proceed();
				awaitIfSupported(method, value, extensionRegistry);
				return value;
			}
		};
		ReflectiveInterceptorCall<Method, Object> call = (interceptor, invocation, invocationContext, context) -> {
			interceptorCall.apply(interceptor, toVoidInvocation(invocation), invocationContext, context);
			return null;
		};
		invoke(capturing, capturing, extensionContext, extensionRegistry, call);
	}

	private void awaitIfSupported(Method method, @Nullable Object value, ExtensionRegistry extensionRegistry) {
		List<AsyncReturnValueHandler> asyncReturnValueHandlers = extensionRegistry.getExtensions(
			AsyncReturnValueHandler.class);
		if (value == null || !AsyncReturnTypeSupport.isSupported(method, asyncReturnValueHandlers)) {
			return;
		}
		try {
			AsyncReturnTypeSupport.toCompletableFuture(value, method, asyncReturnValueHandlers).get();
		}
		catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			throw new CompletionException(e);
		}
		catch (java.util.concurrent.ExecutionException e) {
			Throwable cause = e.getCause() != null ? e.getCause() : e;
			UnrecoverableExceptions.rethrowIfUnrecoverable(cause);
			// Propagate the root cause (checked or not) through the void-typed
			// interceptor chain unchanged.
			ExceptionUtils.throwAsUncheckedException(cause);
		}
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	private static Invocation<@Nullable Void> toVoidInvocation(Invocation<Object> invocation) {
		return (Invocation) invocation;
	}

	/**
	 * Invoke the supplied method with dynamic parameter resolution.
	 *
	 * @param method the method to invoke and resolve parameters for
	 * @param target the target on which the executable will be invoked,
	 * can be {@code null} for a {@code static} method.
	 * @param extensionContext the current {@code ExtensionContext}
	 * @param extensionRegistry the {@code ExtensionRegistry} to retrieve
	 * {@code ParameterResolvers} from
	 * @param interceptorCall the call for intercepting this method invocation
	 * via all registered {@linkplain InvocationInterceptor interceptors}
	 */
	public <T extends @Nullable Object> T invoke(Method method, @Nullable Object target,
			ExtensionContext extensionContext, ExtensionRegistry extensionRegistry,
			ReflectiveInterceptorCall<Method, T> interceptorCall) {

		@Nullable
		Object[] arguments = resolveParameters(method, target, extensionContext, extensionRegistry);
		MethodInvocation<T> invocation = new MethodInvocation<>(method, target, arguments);
		return invoke(invocation, invocation, extensionContext, extensionRegistry, interceptorCall);
	}

	private <E extends Executable, T extends @Nullable Object> T invoke(Invocation<T> originalInvocation,
			ReflectiveInvocationContext<E> invocationContext, ExtensionContext extensionContext,
			ExtensionRegistry extensionRegistry, ReflectiveInterceptorCall<E, T> call) {
		return interceptorChain.invoke(originalInvocation, extensionRegistry, (interceptor,
				wrappedInvocation) -> call.apply(interceptor, wrappedInvocation, invocationContext, extensionContext));
	}

	private <E extends Executable, T> T invoke(Invocation<T> originalInvocation,
			ReflectiveInvocationContext<E> invocationContext, ExtensionContextSupplier extensionContext,
			ExtensionRegistry extensionRegistry, ReflectiveInterceptorCall<E, T> call) {
		return interceptorChain.invoke(originalInvocation, extensionRegistry,
			(interceptor, wrappedInvocation) -> call.apply(interceptor, wrappedInvocation, invocationContext,
				extensionContext.get(interceptor)));
	}

	public interface ReflectiveInterceptorCall<E extends Executable, T extends @Nullable Object> {

		T apply(InvocationInterceptor interceptor, Invocation<T> invocation,
				ReflectiveInvocationContext<E> invocationContext, ExtensionContext extensionContext) throws Throwable;

		@SuppressWarnings("NullAway") // for JDK 26 and earlier
		static ReflectiveInterceptorCall<Method, @Nullable Void> ofVoidMethod(VoidMethodInterceptorCall call) {
			return (interceptorChain, invocation, invocationContext, extensionContext) -> {
				call.apply(interceptorChain, invocation, invocationContext, extensionContext);
				return null;
			};
		}

		interface VoidMethodInterceptorCall {
			void apply(InvocationInterceptor interceptor, Invocation<@Nullable Void> invocation,
					ReflectiveInvocationContext<Method> invocationContext, ExtensionContext extensionContext)
					throws Throwable;
		}

	}

}
