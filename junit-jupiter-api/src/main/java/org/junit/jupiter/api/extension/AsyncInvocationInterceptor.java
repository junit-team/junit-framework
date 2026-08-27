/*
 * Copyright 2015-2026 the original author or authors.
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v2.0 which
 * accompanies this distribution and is available at
 *
 * https://www.eclipse.org/legal/epl-v20.html
 */

package org.junit.jupiter.api.extension;

import static org.apiguardian.api.API.Status.EXPERIMENTAL;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.concurrent.CompletionStage;

import org.apiguardian.api.API;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestFactory;
import org.junit.jupiter.api.TestTemplate;

/**
 * {@code AsyncInvocationInterceptor} defines the API for {@link Extension
 * Extensions} that wish to intercept calls to test code without blocking a
 * thread while an asynchronously-completable test or lifecycle method runs.
 *
 * <p>This interface is the asynchronous counterpart of {@link InvocationInterceptor}.
 * Each interception method receives an {@link AsyncInvocation} and returns a
 * {@link CompletionStage} that completes when the interceptor has no more work
 * to do. This allows the engine to suspend and resume the execution lane
 * without parking a platform thread while awaiting a test or lifecycle method
 * that returns e.g. a {@link CompletionStage}.
 *
 * <h2>Invocation Contract</h2>
 *
 * <p>Each method in this class must return a {@link CompletionStage} that is
 * completed when the {@link AsyncInvocation#proceedAsync() proceedAsync()} on
 * the supplied invocation has been invoked exactly once. Otherwise, the
 * enclosing test or container will be reported as failed.
 *
 * <p>The default implementation returns {@link AsyncInvocation#proceedAsync()
 * proceedAsync()} on the supplied {@linkplain AsyncInvocation invocation}.
 *
 * <h2>Constructor Requirements</h2>
 *
 * <p>Consult the documentation in {@link Extension} for details on
 * constructor requirements.
 *
 * @since 6.2
 * @see InvocationInterceptor
 * @see AsyncInvocation
 * @see ReflectiveInvocationContext
 * @see ExtensionContext
 */
@API(status = EXPERIMENTAL, since = "6.2")
public interface AsyncInvocationInterceptor extends TestInstantiationAwareExtension {

	/**
	 * Intercept the invocation of a test class constructor.
	 *
	 * <p>Note that the test class may <em>not</em> have been initialized
	 * (static initialization) when this method is invoked.
	 *
	 * @param invocation the invocation that is being intercepted; never
	 * {@code null}
	 * @param invocationContext the context of the invocation that is being
	 * intercepted; never {@code null}
	 * @param extensionContext the current extension context; never {@code null}
	 * @param <T> the result type
	 * @return a completion stage signaling that the interceptor has finished;
	 * never {@code null}
	 */
	default <T> CompletionStage<T> interceptTestClassConstructorAsync(AsyncInvocation<T> invocation,
			ReflectiveInvocationContext<Constructor<T>> invocationContext, ExtensionContext extensionContext) {
		return invocation.proceedAsync();
	}

	/**
	 * Intercept the invocation of a {@link BeforeAll @BeforeAll} method.
	 *
	 * @param invocation the invocation that is being intercepted; never
	 * {@code null}
	 * @param invocationContext the context of the invocation that is being
	 * intercepted; never {@code null}
	 * @param extensionContext the current extension context; never {@code null}
	 * @return a completion stage signaling that the interceptor has finished;
	 * never {@code null}
	 */
	default CompletionStage<Void> interceptBeforeAllMethodAsync(AsyncInvocation<Void> invocation,
			ReflectiveInvocationContext<Method> invocationContext, ExtensionContext extensionContext) {
		return invocation.proceedAsync();
	}

	/**
	 * Intercept the invocation of a {@link BeforeEach @BeforeEach} method.
	 *
	 * @param invocation the invocation that is being intercepted; never
	 * {@code null}
	 * @param invocationContext the context of the invocation that is being
	 * intercepted; never {@code null}
	 * @param extensionContext the current extension context; never {@code null}
	 * @return a completion stage signaling that the interceptor has finished;
	 * never {@code null}
	 */
	default CompletionStage<Void> interceptBeforeEachMethodAsync(AsyncInvocation<Void> invocation,
			ReflectiveInvocationContext<Method> invocationContext, ExtensionContext extensionContext) {
		return invocation.proceedAsync();
	}

	/**
	 * Intercept the invocation of a {@link Test @Test} method.
	 *
	 * @param invocation the invocation that is being intercepted; never
	 * {@code null}
	 * @param invocationContext the context of the invocation that is being
	 * intercepted; never {@code null}
	 * @param extensionContext the current extension context; never {@code null}
	 * @return a completion stage signaling that the interceptor has finished;
	 * never {@code null}
	 */
	default CompletionStage<Void> interceptTestMethodAsync(AsyncInvocation<Void> invocation,
			ReflectiveInvocationContext<Method> invocationContext, ExtensionContext extensionContext) {
		return invocation.proceedAsync();
	}

	/**
	 * Intercept the invocation of a {@link TestFactory @TestFactory} method,
	 * such as a {@link org.junit.jupiter.api.RepeatedTest @RepeatedTest} or
	 * {@code @ParameterizedTest} method.
	 *
	 * @param invocation the invocation that is being intercepted; never
	 * {@code null}
	 * @param invocationContext the context of the invocation that is being
	 * intercepted; never {@code null}
	 * @param extensionContext the current extension context; never {@code null}
	 * @param <T> the result type
	 * @return a completion stage providing the result of the invocation;
	 * never {@code null}
	 */
	default <T extends @Nullable Object> CompletionStage<T> interceptTestFactoryMethodAsync(
			AsyncInvocation<T> invocation, ReflectiveInvocationContext<Method> invocationContext,
			ExtensionContext extensionContext) {
		return invocation.proceedAsync();
	}

	/**
	 * Intercept the invocation of a {@link TestTemplate @TestTemplate} method.
	 *
	 * @param invocation the invocation that is being intercepted; never
	 * {@code null}
	 * @param invocationContext the context of the invocation that is being
	 * intercepted; never {@code null}
	 * @param extensionContext the current extension context; never {@code null}
	 * @return a completion stage signaling that the interceptor has finished;
	 * never {@code null}
	 */
	default CompletionStage<Void> interceptTestTemplateMethodAsync(AsyncInvocation<Void> invocation,
			ReflectiveInvocationContext<Method> invocationContext, ExtensionContext extensionContext) {
		return invocation.proceedAsync();
	}

	/**
	 * Intercept the invocation of a {@link DynamicTest}.
	 *
	 * @param invocation the invocation that is being intercepted; never
	 * {@code null}
	 * @param invocationContext the context of the invocation that is being
	 * intercepted; never {@code null}
	 * @param extensionContext the current extension context; never {@code null}
	 * @return a completion stage signaling that the interceptor has finished;
	 * never {@code null}
	 */
	default CompletionStage<Void> interceptDynamicTestAsync(AsyncInvocation<Void> invocation,
			DynamicTestInvocationContext invocationContext, ExtensionContext extensionContext) {
		return invocation.proceedAsync();
	}

	/**
	 * Intercept the invocation of an {@link AfterEach @AfterEach} method.
	 *
	 * @param invocation the invocation that is being intercepted; never
	 * {@code null}
	 * @param invocationContext the context of the invocation that is being
	 * intercepted; never {@code null}
	 * @param extensionContext the current extension context; never {@code null}
	 * @return a completion stage signaling that the interceptor has finished;
	 * never {@code null}
	 */
	default CompletionStage<Void> interceptAfterEachMethodAsync(AsyncInvocation<Void> invocation,
			ReflectiveInvocationContext<Method> invocationContext, ExtensionContext extensionContext) {
		return invocation.proceedAsync();
	}

	/**
	 * Intercept the invocation of an {@link AfterAll @AfterAll} method.
	 *
	 * @param invocation the invocation that is being intercepted; never
	 * {@code null}
	 * @param invocationContext the context of the invocation that is being
	 * intercepted; never {@code null}
	 * @param extensionContext the current extension context; never {@code null}
	 * @return a completion stage signaling that the interceptor has finished;
	 * never {@code null}
	 */
	default CompletionStage<Void> interceptAfterAllMethodAsync(AsyncInvocation<Void> invocation,
			ReflectiveInvocationContext<Method> invocationContext, ExtensionContext extensionContext) {
		return invocation.proceedAsync();
	}

	/**
	 * An invocation that returns a result, possibly after asynchronous work,
	 * and may throw a {@link Throwable}.
	 *
	 * <p>This interface is not intended to be implemented by clients.
	 *
	 * @param <T> the result type
	 * @since 6.2
	 */
	@API(status = EXPERIMENTAL, since = "6.2")
	interface AsyncInvocation<T extends @Nullable Object> {

		/**
		 * Proceed with this invocation asynchronously.
		 *
		 * @return a completion stage providing the result of this invocation;
		 * potentially {@code null}
		 */
		CompletionStage<T> proceedAsync();

		/**
		 * Explicitly skip this invocation.
		 *
		 * <p>This allows to bypass the check that {@link #proceedAsync()} must
		 * be called at least once. The default implementation does nothing.
		 */
		default void skip() {
			// do nothing
		}
	}

}
