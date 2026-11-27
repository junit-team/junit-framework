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

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.AsyncReturnValueHandler;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.platform.engine.DiscoverySelector;
import org.junit.platform.testkit.engine.EngineExecutionResults;
import org.junit.platform.testkit.engine.EngineTestKit;

/**
 * Integration tests for custom asynchronous return types handled via
 * {@link AsyncReturnValueHandler}.
 *
 * @since 6.2
 */
class AsyncReturnValueHandlerTests {

	@Test
	void testMethodReturningCustomPromiseIsAwaitedViaExtendWith() {
		executeForClass(ExtendWithTestCase.class).testEvents() //
				.assertStatistics(stats -> stats.started(1).succeeded(1).failed(0)) //
				.assertThatEvents().haveExactly(1, finishedSuccessfully());
	}

	@Test
	void testMethodReturningCustomPromiseIsAwaitedViaUmbrellaAnnotation() {
		executeForClass(UmbrellaAnnotationTestCase.class).testEvents() //
				.assertStatistics(stats -> stats.started(1).succeeded(1).failed(0)) //
				.assertThatEvents().haveExactly(1, finishedSuccessfully());
	}

	@Test
	void classLevelExtendWithIsRecognizedWhenReturningCustomPromise() {
		executeForClass(ClassLevelExtendWithTestCase.class).testEvents() //
				.assertStatistics(stats -> stats.started(1).succeeded(1).failed(0)) //
				.assertThatEvents().haveExactly(1, finishedSuccessfully());
	}

	@Test
	void classLevelUmbrellaAnnotationIsRecognizedWhenReturningCustomPromise() {
		executeForClass(ClassLevelUmbrellaTestCase.class).testEvents() //
				.assertStatistics(stats -> stats.started(1).succeeded(1).failed(0)) //
				.assertThatEvents().haveExactly(1, finishedSuccessfully());
	}

	@Test
	void inheritedClassLevelExtendWithIsRecognizedWhenReturningCustomPromise() {
		executeForClass(InheritedClassLevelTestCase.class).testEvents() //
				.assertStatistics(stats -> stats.started(1).succeeded(1).failed(0)) //
				.assertThatEvents().haveExactly(1, finishedSuccessfully());
	}

	@Test
	void enclosingClassLevelExtendWithIsRecognizedForNestedTestWhenReturningCustomPromise() {
		executeForClass(NestedTestCase.class).testEvents() //
				.assertStatistics(stats -> stats.started(1).succeeded(1).failed(0)) //
				.assertThatEvents().haveExactly(1, finishedSuccessfully());
	}

	@Test
	void lifecycleMethodsReturningCustomPromiseAreAwaited() {
		executeForClass(LifecycleMethodsTestCase.class).testEvents() //
				.assertStatistics(stats -> stats.started(1).succeeded(1).failed(0)) //
				.assertThatEvents().haveExactly(1, finishedSuccessfully());
	}

	@Test
	void nestedClassLifecycleMethodsReturningCustomPromiseAreAwaited() {
		executeForClass(NestedLifecycleTestCase.class).testEvents() //
				.assertStatistics(stats -> stats.started(1).succeeded(1).failed(0)) //
				.assertThatEvents().haveExactly(1, finishedSuccessfully());
	}

	private EngineExecutionResults executeForClass(Class<?> testClass) {
		return execute(selectClass(testClass));
	}

	private EngineExecutionResults execute(DiscoverySelector selector) {
		return EngineTestKit //
				.engine(new org.junit.jupiter.engine.JupiterTestEngine()) //
				.selectors(selector) //
				.execute();
	}

	// -------------------------------------------------------------------------

	record MyPromise<T>(CompletionStage<T> delegate) {

		static <T> MyPromise<T> completed(T value) {
			return new MyPromise<>(CompletableFuture.completedFuture(value));
		}

		static MyPromise<Void> ofAsync(Runnable asyncBody) {
			CompletableFuture<Void> stage = new CompletableFuture<>();
			asyncBody.run();
			stage.complete(null);
			return new MyPromise<>(stage);
		}
	}

	/**
	 * Converts a {@link MyPromise} returned from a test method into a
	 * {@link CompletionStage} to be awaited.
	 */
	static class MyAsyncReturnValueHandler implements AsyncReturnValueHandler {

		@Override
		public boolean supports(Type genericReturnType, @Nullable AnnotatedElement annotatedElement) {
			Class<?> rawType = (genericReturnType instanceof ParameterizedType parameterizedType)
					? (Class<?>) parameterizedType.getRawType()
					: (Class<?>) genericReturnType;
			return MyPromise.class.isAssignableFrom(rawType);
		}

		@Override
		public CompletionStage<?> toCompletionStage(Object returnedValue) {
			return ((MyPromise<?>) returnedValue).delegate();
		}
	}

	@Target({ ElementType.METHOD, ElementType.ANNOTATION_TYPE })
	@Retention(RetentionPolicy.RUNTIME)
	@ExtendWith(MyAsyncReturnValueHandler.class)
	@interface MarkPromise {
	}

	@Target(ElementType.METHOD)
	@Retention(RetentionPolicy.RUNTIME)
	@MarkPromise
	@interface MapMyPromise {
	}

	@Target(ElementType.TYPE)
	@Retention(RetentionPolicy.RUNTIME)
	@ExtendWith(MyAsyncReturnValueHandler.class)
	@interface ClassLevelMarkPromise {
	}

	static class ExtendWithTestCase {

		@Test
		@ExtendWith(MyAsyncReturnValueHandler.class)
		MyPromise<String> test() {
			return MyPromise.completed("done");
		}
	}

	static class UmbrellaAnnotationTestCase {

		@Test
		@MapMyPromise
		MyPromise<String> test() {
			return MyPromise.completed("done");
		}
	}

	@ClassLevelMarkPromise
	static class ClassLevelExtendWithTestCase {

		@Test
		MyPromise<String> test() {
			return MyPromise.completed("done");
		}
	}

	@ClassLevelMarkPromise
	static class ClassLevelUmbrellaTestCase {

		@Test
		MyPromise<String> test() {
			return MyPromise.completed("done");
		}
	}

	@ClassLevelMarkPromise
	static class AbstractClassLevelBase {

		@Test
		MyPromise<String> test() {
			return MyPromise.completed("done");
		}
	}

	static class InheritedClassLevelTestCase extends AbstractClassLevelBase {
	}

	@ClassLevelMarkPromise
	static class NestedTestCase {

		@Nested
		class NestedInner {

			@Test
			MyPromise<String> test() {
				return MyPromise.completed("done");
			}
		}
	}

	@ClassLevelMarkPromise
	static class LifecycleMethodsTestCase {

		static volatile boolean beforeAllCompleted;
		static volatile boolean beforeAllAfterEachSeen;

		@BeforeAll
		static MyPromise<Void> beforeAll() {
			return MyPromise.ofAsync(() -> beforeAllCompleted = true);
		}

		@BeforeEach
		MyPromise<Void> beforeEach() {
			// Must only run after @BeforeAll's async work completed.
			return MyPromise.ofAsync(() -> beforeAllAfterEachSeen = beforeAllCompleted);
		}

		@AfterEach
		MyPromise<Void> afterEach() {
			return MyPromise.ofAsync(() -> {
			});
		}

		@AfterAll
		static MyPromise<Void> afterAll() {
			return MyPromise.ofAsync(() -> {
			});
		}

		@Test
		void test() {
			// @BeforeAll completed asynchronously before this test ran.
			assertTrue(beforeAllAfterEachSeen);
		}
	}

	@ClassLevelMarkPromise
	static class NestedLifecycleTestCase {

		@Nested
		class NestedInner {

			@BeforeEach
			MyPromise<Void> beforeEach() {
				return MyPromise.ofAsync(() -> {
				});
			}

			@Test
			MyPromise<String> test() {
				return MyPromise.completed("done");
			}
		}
	}

}
