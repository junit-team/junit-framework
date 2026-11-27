/*
 * Copyright 2015-2026 the original author or authors.
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v2.0 which
 * accompanies this distribution and is available at
 *
 * https://www.eclipse.org/legal/epl-v20.html
 */

package org.junit.jupiter.engine.extension;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.platform.engine.discovery.DiscoverySelectors.selectClass;
import static org.junit.platform.testkit.engine.EventConditions.finishedSuccessfully;

import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.atomic.AtomicInteger;

import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.AsyncReturnValueHandler;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.platform.testkit.engine.EngineTestKit;

/**
 * Verifies that {@link AsyncReturnValueHandler#supports(Type, AnnotatedElement)}
 * is queried only a bounded number of times per method thanks to caching.
 *
 * @since 6.2
 */
class AsyncReturnValueHandlerCacheTests {

	private final CountingAsyncReturnValueHandler handler = new CountingAsyncReturnValueHandler();
	private final AtomicInteger supportCallsSinceReset = new AtomicInteger();

	@Test
	void supportsIsNotInvokedRepeatedlyForTheSameMethod() {
		handler.resetSupportCount();

		EngineTestKit.engine(new org.junit.jupiter.engine.JupiterTestEngine()) //
				.selectors(selectClass(TestCase.class)) //
				.execute() //
				.testEvents() //
				.assertStatistics(stats -> stats.started(1).succeeded(1).failed(0)) //
				.assertThatEvents().haveExactly(1, finishedSuccessfully());

		int supportsCallCount = handler.getSupportCount();
		// The result for this method is cached after the first evaluation, so
		// `supports` must not be invoked once per descriptor pass.
		assertTrue(supportsCallCount <= 2,
			"supports() should be called only once during this execution, but was called " + supportsCallCount
					+ " times");
	}

	// -------------------------------------------------------------------------

	record MyPromise<T>(CompletionStage<T> delegate) {

		static <T> MyPromise<T> completed(T value) {
			return new MyPromise<>(CompletableFuture.completedFuture(value));
		}
	}

	static class CountingAsyncReturnValueHandler implements AsyncReturnValueHandler {

		private final AtomicInteger supportCalls = new AtomicInteger();

		@Override
		public boolean supports(Type genericReturnType, @Nullable AnnotatedElement annotatedElement) {
			supportCalls.incrementAndGet();
			if (genericReturnType instanceof ParameterizedType parameterizedType) {
				return MyPromise.class.isAssignableFrom((Class<?>) parameterizedType.getRawType());
			}
			return MyPromise.class == genericReturnType;
		}

		@Override
		public CompletionStage<?> toCompletionStage(Object returnedValue) {
			return ((MyPromise<?>) returnedValue).delegate();
		}

		int getSupportCount() {
			return supportCalls.get();
		}

		void resetSupportCount() {
			supportCalls.set(0);
		}
	}

	@ExtendWith(CountingAsyncReturnValueHandler.class)
	static class TestCase {

		@Test
		MyPromise<String> test() {
			return MyPromise.completed("done");
		}
	}

}
