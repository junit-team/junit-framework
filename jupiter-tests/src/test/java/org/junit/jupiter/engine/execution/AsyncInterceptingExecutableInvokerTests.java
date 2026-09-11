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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;

import java.lang.reflect.Method;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.atomic.AtomicBoolean;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.AsyncInvocationInterceptor;
import org.junit.jupiter.api.extension.AsyncInvocationInterceptor.AsyncInvocation;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.InvocationInterceptor;
import org.junit.jupiter.api.extension.ReflectiveInvocationContext;
import org.junit.jupiter.engine.extension.MutableExtensionRegistry;

/**
 * Tests for {@link AsyncInterceptingExecutableInvoker} and the asynchronous
 * invocation pipeline.
 *
 * @since 6.2
 */
class AsyncInterceptingExecutableInvokerTests {

	private final ExtensionContext extensionContext = mock();

	private final MutableExtensionRegistry extensionRegistry = MutableExtensionRegistry.createEmptyRegistry();

	@Test
	void interceptsVoidMethodAndCompletes() throws Exception {
		var method = getMethod(TestCase.class, "voidMethod");
		extensionRegistry.registerExtension(new IncrementingAsyncInterceptor(), this);

		CompletionStage<Void> stage = new AsyncInterceptingExecutableInvoker().interceptMethodAsync(method,
			new TestCase(), extensionContext, extensionRegistry, interceptTestMethodAsync());

		stage.toCompletableFuture().get();
		assertEquals(2, TestCase.count);
	}

	@Test
	void waitsForAsyncReturnValueWithoutBlocking() {
		var method = getMethod(TestCase.class, "completionStageMethod");
		var gate = new CompletableFuture<Void>();

		CompletableFuture<Void> invocation = new AsyncInterceptingExecutableInvoker().interceptMethodAsync(method,
			new TestCase(gate), extensionContext, extensionRegistry, interceptTestMethodAsync()).toCompletableFuture();

		// Non-blocking guarantee: the produced stage is returned immediately and
		// only completes once the intercepted method's own stage completes.
		assertFalse(invocation.isDone());

		gate.complete(null);
		assertTrue(invocation.isDone());
		var result = invocation.join();
	}

	@Test
	void propagatesAsyncFailure() {
		var method = getMethod(TestCase.class, "completionStageMethod");
		var gate = new CompletableFuture<Void>();
		gate.completeExceptionally(new IllegalStateException("async boom"));

		CompletableFuture<Void> invocation = new AsyncInterceptingExecutableInvoker().interceptMethodAsync(method,
			new TestCase(gate), extensionContext, extensionRegistry, interceptTestMethodAsync()).toCompletableFuture();

		var executionException = assertThrows(ExecutionException.class, invocation::get);
		assertTrue(executionException.getCause() instanceof IllegalStateException);
	}

	@Test
	void adaptsLegacyInvocationInterceptor() throws Exception {
		var method = getMethod(TestCase.class, "voidMethod");
		extensionRegistry.registerExtension(new LegacyLoggingInterceptor(), this);

		CompletionStage<Void> stage = new AsyncInterceptingExecutableInvoker().interceptMethodAsync(method,
			new TestCase(), extensionContext, extensionRegistry, interceptTestMethodAsync());

		stage.toCompletableFuture().get();
		assertTrue(TestCase.legacySeen.get());
	}

	private static Method getMethod(Class<?> clazz, String name) {
		try {
			return clazz.getDeclaredMethod(name);
		}
		catch (NoSuchMethodException e) {
			throw new IllegalArgumentException(e);
		}
	}

	private static AsyncInterceptingExecutableInvoker.AsyncVoidMethodInterceptorCall interceptTestMethodAsync() {
		return AsyncInvocationInterceptor::interceptTestMethodAsync;
	}

	static class TestCase {

		static volatile int count;
		static final AtomicBoolean legacySeen = new AtomicBoolean();

		private final CompletableFuture<Void> gate;

		TestCase() {
			this(CompletableFuture.completedFuture(null));
		}

		TestCase(CompletableFuture<Void> gate) {
			this.gate = gate;
		}

		void voidMethod() {
			count++;
		}

		CompletionStage<?> completionStageMethod() {
			return gate;
		}
	}

	static class IncrementingAsyncInterceptor implements AsyncInvocationInterceptor {

		@Override
		public CompletionStage<Void> interceptTestMethodAsync(AsyncInvocation<Void> invocation,
				ReflectiveInvocationContext<Method> invocationContext, ExtensionContext extensionContext) {
			TestCase.count++;
			return invocation.proceedAsync();
		}
	}

	static class LegacyLoggingInterceptor implements InvocationInterceptor {

		@Override
		public void interceptTestMethod(Invocation<@org.jspecify.annotations.Nullable Void> invocation,
				ReflectiveInvocationContext<Method> invocationContext, ExtensionContext extensionContext)
				throws Throwable {
			TestCase.legacySeen.set(true);
			invocation.proceed();
		}
	}

}
