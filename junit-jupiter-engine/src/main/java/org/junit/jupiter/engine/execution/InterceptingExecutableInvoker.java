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
import java.util.Optional;

import org.apiguardian.api.API;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.InvocationInterceptor;
import org.junit.jupiter.api.extension.InvocationInterceptor.Invocation;
import org.junit.jupiter.api.extension.ParameterResolver;
import org.junit.jupiter.api.extension.ReflectiveInvocationContext;
import org.junit.jupiter.engine.execution.InterceptingExecutableInvoker.ReflectiveInterceptorCall.VoidMethodInterceptorCall;
import org.junit.jupiter.engine.extension.ExtensionRegistry;

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
	public <T> T invoke(Constructor<T> constructor, Optional<Object> outerInstance,
			ExtensionContextSupplier extensionContext, ExtensionRegistry extensionRegistry,
			ReflectiveInterceptorCall<Constructor<T>, T> interceptorCall) {

		@Nullable
		Object[] arguments = resolveParameters(constructor, Optional.empty(), outerInstance, extensionContext,
			extensionRegistry);
		ConstructorInvocation<T> invocation = new ConstructorInvocation<>(constructor, arguments);
		return invoke(invocation, invocation, extensionContext, extensionRegistry, interceptorCall);
	}

	public void invokeVoid(Method method, @Nullable Object target, ExtensionContext extensionContext,
			ExtensionRegistry extensionRegistry, VoidMethodInterceptorCall interceptorCall) {
		this.<@Nullable Void> invoke(method, target, extensionContext, extensionRegistry,
			ReflectiveInterceptorCall.ofVoidMethod(interceptorCall));
	}

	/**
	 * Invoke the supplied method with dynamic parameter resolution.
	 *
	 * @param method the method to invoke and resolve parameters for
	 * @param target the target on which the executable will be invoked,
	 * potentially wrapped in an {@link Optional}; can be {@code null} or an
	 * empty {@code Optional} for a {@code static} method
	 * @param extensionContext the current {@code ExtensionContext}
	 * @param extensionRegistry the {@code ExtensionRegistry} to retrieve
	 * {@code ParameterResolvers} from
	 * @param interceptorCall the call for intercepting this method invocation
	 * via all registered {@linkplain InvocationInterceptor interceptors}
	 */
	public <T extends @Nullable Object> T invoke(Method method, @Nullable Object target,
			ExtensionContext extensionContext, ExtensionRegistry extensionRegistry,
			ReflectiveInterceptorCall<Method, T> interceptorCall) {

		@SuppressWarnings({ "unchecked", "rawtypes" })
		Optional<Object> optionalTarget = (target instanceof Optional optional ? optional
				: Optional.ofNullable(target));
		@Nullable
		Object[] arguments = resolveParameters(method, optionalTarget, extensionContext, extensionRegistry);
		MethodInvocation<T> invocation = new MethodInvocation<>(method, optionalTarget, arguments);
		return invoke(invocation, invocation, extensionContext, extensionRegistry, interceptorCall);
	}

	private <E extends Executable, T> T invoke(Invocation<T> originalInvocation,
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

		static ReflectiveInterceptorCall<Method, @Nullable Void> ofVoidMethod(VoidMethodInterceptorCall call) {
			return (interceptorChain, invocation, invocationContext, extensionContext) -> {
				call.apply(interceptorChain, invocation, invocationContext, extensionContext);
				return null;
			};
		}

		interface VoidMethodInterceptorCall {
			void apply(InvocationInterceptor interceptor, Invocation<Void> invocation,
					ReflectiveInvocationContext<Method> invocationContext, ExtensionContext extensionContext)
					throws Throwable;
		}

	}

}
