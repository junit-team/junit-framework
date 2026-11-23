/*
 * Copyright 2015-2026 the original author or authors.
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v2.0 which
 * accompanies this distribution and is available at
 *
 * https://www.eclipse.org/legal/epl-v20.html
 */

package org.junit.platform.engine.support.hierarchical;

import static java.util.Objects.requireNonNull;
import static java.util.concurrent.CompletableFuture.completedFuture;
import static java.util.concurrent.CompletableFuture.failedFuture;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.platform.engine.TestExecutionResult.Status.FAILED;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;

import org.jspecify.annotations.NullMarked;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.junit.platform.engine.CancellationToken;
import org.junit.platform.engine.EngineExecutionListener;
import org.junit.platform.engine.ExecutionRequest;
import org.junit.platform.engine.TestDescriptor;
import org.junit.platform.engine.TestExecutionResult;
import org.junit.platform.engine.UniqueId;
import org.junit.platform.engine.support.descriptor.AbstractTestDescriptor;

@Timeout(15)
class AsyncNodeLifecycleTests {

	@Test
	void blockingAroundPreservesContextThreadAndLexicalScope() throws Exception {
		final var events = new CopyOnWriteArrayList<String>();
		final var scope = new ThreadLocal<String>();
		final var thread = Thread.currentThread();
		final var wrappedContext = new TestContext("wrapped");
		final var root = new TestNode("root", TestDescriptor.Type.CONTAINER) {
			@Override
			public void around(TestContext context, Invocation<TestContext> invocation) throws Exception {
				events.add("around-before");
				scope.set("active");
				try {
					invocation.invoke(wrappedContext);
				}
				finally {
					assertThat(scope.get()).isEqualTo("active");
					events.add("around-after");
					scope.remove();
				}
			}

			@Override
			public TestContext before(TestContext context) {
				assertScope(context, "before");
				return context;
			}

			@Override
			public TestContext execute(TestContext context, DynamicTestExecutor executor) {
				assertScope(context, "execute");
				return context;
			}

			@Override
			public void after(TestContext context) {
				assertScope(context, "after");
			}

			private void assertScope(TestContext context, String phase) {
				assertThat(context).isSameAs(wrappedContext);
				assertThat(Thread.currentThread()).isSameAs(thread);
				assertThat(scope.get()).isEqualTo("active");
				events.add(phase);
			}
		};
		final var listener = new RecordingListener();
		try (var service = new SameThreadHierarchicalTestExecutorService()) {
			run(root, service, listener).get(5, SECONDS);
		}
		assertThat(events).containsExactly("around-before", "before", "execute", "after", "around-after");
		assertThat(scope.get()).isNull();
		assertThat(listener.result(root).getThrowable()).isEmpty();
	}

	@Test
	void reactiveDefaultUsesNativeWrapperInsteadOfLegacyThreadScope() throws Exception {
		final var root = new TestNode("root", TestDescriptor.Type.CONTAINER) {
			@Override
			public void around(TestContext context, Invocation<TestContext> invocation) {
				throw new AssertionError("The opt-in reactive lane must not enter a synchronous thread scope");
			}
		};
		final var listener = new RecordingListener();
		try (var service = new ReactiveHierarchicalTestExecutorService(1)) {
			run(root, service, listener).get(5, SECONDS);
		}
		assertThat(listener.result(root).getThrowable()).isEmpty();
	}

	@Test
	void reactivePhasesAndWrapperComposeWithoutOccupyingTheOnlyWorker() throws Exception {
		final var events = new CopyOnWriteArrayList<String>();
		final var before = new CompletableFuture<TestContext>();
		final var body = new CompletableFuture<TestContext>();
		final var after = new CompletableFuture<Void>();
		final var wrapper = new CompletableFuture<Void>();
		final var beforeEntered = new CompletableFuture<Void>();
		final var bodyEntered = new CompletableFuture<Void>();
		final var afterEntered = new CompletableFuture<Void>();
		final var wrapperEntered = new CompletableFuture<Void>();
		final var root = new TestNode("root", TestDescriptor.Type.CONTAINER) {
			@Override
			public CompletionStage<?> aroundAsync(TestContext context,
					Function<TestContext, CompletionStage<?>> invocation) {
				events.add("around-before");
				return invocation.apply(new TestContext("wrapped")).thenCompose(__ -> {
					events.add("around-after");
					wrapperEntered.complete(null);
					return wrapper;
				});
			}

			@Override
			public CompletionStage<TestContext> beforeAsync(TestContext context) {
				assertThat(context.name()).isEqualTo("wrapped");
				events.add("before");
				beforeEntered.complete(null);
				return before;
			}

			@Override
			public CompletionStage<TestContext> executeAsync(TestContext context, DynamicTestExecutor executor) {
				assertThat(context.name()).isEqualTo("prepared");
				events.add("execute");
				bodyEntered.complete(null);
				return body;
			}

			@Override
			public CompletionStage<?> afterAsync(TestContext context) {
				assertThat(context.name()).isEqualTo("executed");
				events.add("after");
				afterEntered.complete(null);
				return after;
			}
		};
		final var worker = Executors.newSingleThreadExecutor();
		final var listener = new RecordingListener();
		try (var service = new ReactiveHierarchicalTestExecutorService(1, worker)) {
			final var result = run(root, service, listener);
			beforeEntered.get(5, SECONDS);
			assertThat(worker.submit(() -> "free").get(5, SECONDS)).isEqualTo("free");
			assertThat(events).containsExactly("around-before", "before");
			before.complete(new TestContext("prepared"));
			bodyEntered.get(5, SECONDS);
			assertThat(result.isDone()).isFalse();
			body.complete(new TestContext("executed"));
			afterEntered.get(5, SECONDS);
			assertThat(result.isDone()).isFalse();
			after.complete(null);
			wrapperEntered.get(5, SECONDS);
			assertThat(result.isDone()).isFalse();
			wrapper.complete(null);
			result.get(5, SECONDS);
		}
		finally {
			before.complete(new TestContext("prepared"));
			body.complete(new TestContext("executed"));
			after.complete(null);
			wrapper.complete(null);
		}
		assertThat(events).containsExactly("around-before", "before", "execute", "after", "around-after");
		assertThat(listener.result(root).getThrowable()).isEmpty();
	}

	@ParameterizedTest
	@ValueSource(booleans = { false, true })
	void reactiveExecuteFailureStillDrainsDynamicChildrenBeforeAfter(boolean throwDirectly) throws Exception {
		final var events = new CopyOnWriteArrayList<String>();
		final var dynamicEntered = new CompletableFuture<Void>();
		final var dynamicDone = new CompletableFuture<TestContext>();
		final var executeFailure = new IllegalStateException("execute");
		final var afterFailure = new IllegalArgumentException("after");
		final var dynamic = new TestNode("dynamic", TestDescriptor.Type.TEST) {
			@Override
			public CompletionStage<TestContext> executeAsync(TestContext context, DynamicTestExecutor executor) {
				dynamicEntered.complete(null);
				return dynamicDone;
			}

			@Override
			public void cleanUp(TestContext context) {
				events.add("dynamic-cleanup");
			}
		};
		final var parent = new TestNode("parent", TestDescriptor.Type.CONTAINER) {
			@Override
			public CompletionStage<TestContext> executeAsync(TestContext context, DynamicTestExecutor executor) {
				executor.execute(dynamic);
				if (throwDirectly) {
					throw executeFailure;
				}
				return failedFuture(executeFailure);
			}

			@Override
			public CompletionStage<?> afterAsync(TestContext context) {
				events.add("parent-after");
				return failedFuture(afterFailure);
			}

			@Override
			public void cleanUp(TestContext context) {
				events.add("parent-cleanup");
			}
		};
		final var root = new TestNode("root", TestDescriptor.Type.CONTAINER);
		root.addChild(parent);
		final var listener = new RecordingListener();
		try (var service = new ReactiveHierarchicalTestExecutorService(1)) {
			final var result = run(root, service, listener);
			dynamicEntered.get(5, SECONDS);
			assertThat(events).isEmpty();
			assertThat(result.isDone()).isFalse();
			dynamicDone.complete(new TestContext("dynamic"));
			result.get(5, SECONDS);
		}
		finally {
			dynamicDone.complete(new TestContext("dynamic"));
		}
		assertThat(events).containsExactly("dynamic-cleanup", "parent-after", "parent-cleanup");
		assertThat(listener.result(parent).getStatus()).isEqualTo(FAILED);
		assertThat(listener.result(parent).getThrowable().orElseThrow()).isSameAs(
			executeFailure).hasSuppressedException(afterFailure);
	}

	@ParameterizedTest
	@ValueSource(booleans = { false, true })
	void reactiveFatalFailureSettlesTheReturnedFuture(boolean throwDirectly) throws Exception {
		final var fatal = new OutOfMemoryError("test fatal propagation");
		final var root = new TestNode("root", TestDescriptor.Type.CONTAINER) {
			@Override
			public CompletionStage<TestContext> executeAsync(TestContext context, DynamicTestExecutor executor) {
				if (throwDirectly) {
					throw fatal;
				}
				return failedFuture(fatal);
			}
		};
		try (var service = new ReactiveHierarchicalTestExecutorService(1)) {
			final var result = run(root, service, new RecordingListener());
			final var failure = assertThrows(java.util.concurrent.ExecutionException.class,
				() -> result.get(5, SECONDS));
			assertThat(failure).hasRootCause(fatal);
		}
	}

	@ParameterizedTest
	@ValueSource(strings = { "success", "skip", "failure", "fatal", "prepare-fatal" })
	void reactiveTaskFinalizesExactlyOnceBeforeCompletion(String outcome) throws Exception {
		final var finalizations = new AtomicInteger();
		final var root = new TestNode("root", TestDescriptor.Type.CONTAINER) {
			@Override
			public TestContext prepare(TestContext context) {
				if (outcome.equals("prepare-fatal")) {
					throw new OutOfMemoryError("prepare");
				}
				return context;
			}

			@Override
			public SkipResult shouldBeSkipped(TestContext context) {
				return outcome.equals("skip") ? SkipResult.skip("test") : SkipResult.doNotSkip();
			}

			@Override
			public CompletionStage<TestContext> executeAsync(TestContext context, DynamicTestExecutor executor) {
				return switch (outcome) {
					case "failure" -> failedFuture(new IllegalStateException("execute"));
					case "fatal" -> failedFuture(new OutOfMemoryError("execute"));
					default -> completedFuture(context);
				};
			}
		};
		try (var service = new ReactiveHierarchicalTestExecutorService(1)) {
			final var advisor = new NodeTreeWalker().walk(root);
			final var taskContext = new NodeTestTaskContext(new RecordingListener(), service,
				OpenTest4JAwareThrowableCollector::new, advisor, CancellationToken.create());
			final var task = new NodeTestTask<TestContext>(taskContext, root, finalizations::incrementAndGet);
			task.setParentContext(new TestContext("root"));
			final var result = service.submit(task);
			if (outcome.contains("fatal")) {
				assertThrows(java.util.concurrent.ExecutionException.class, () -> result.get(5, SECONDS));
			}
			else {
				result.get(5, SECONDS);
			}
			assertThat(finalizations.get()).isOne();
		}
	}

	@Test
	void completedDynamicFatalIsNotLostWhileParentBodyRemainsPending() throws Exception {
		final var fatal = new OutOfMemoryError("completed dynamic child");
		final var body = new CompletableFuture<TestContext>();
		final var submitted = new CompletableFuture<Future<?>>();
		final var afterCalls = new AtomicInteger();
		final var dynamic = new TestNode("dynamic", TestDescriptor.Type.TEST) {
			@Override
			public CompletionStage<TestContext> executeAsync(TestContext context, DynamicTestExecutor executor) {
				return failedFuture(fatal);
			}
		};
		final var root = new TestNode("root", TestDescriptor.Type.CONTAINER) {
			@Override
			public CompletionStage<TestContext> executeAsync(TestContext context, DynamicTestExecutor executor) {
				submitted.complete(executor.execute(dynamic, EngineExecutionListener.NOOP));
				return body;
			}

			@Override
			public void after(TestContext context) {
				afterCalls.incrementAndGet();
			}
		};
		try (var service = new ReactiveHierarchicalTestExecutorService(1)) {
			final var result = run(root, service, new RecordingListener());
			final var childResult = submitted.get(5, SECONDS);
			assertThrows(java.util.concurrent.ExecutionException.class, () -> childResult.get(5, SECONDS));
			// Its finalizer has run, while the parent has not reached dynamic draining yet.
			assertThat(result.isDone()).isFalse();
			body.complete(new TestContext("root"));
			final var failure = assertThrows(java.util.concurrent.ExecutionException.class,
				() -> result.get(5, SECONDS));
			assertThat(failure).hasRootCause(fatal);
			assertThat(afterCalls.get()).isZero();
		}
		finally {
			body.complete(new TestContext("root"));
		}
	}

	private Future<?> run(TestNode root, HierarchicalTestExecutorService service, RecordingListener listener) {
		final ExecutionRequest request = mock();
		when(request.getRootTestDescriptor()).thenReturn(root);
		when(request.getEngineExecutionListener()).thenReturn(listener);
		when(request.getCancellationToken()).thenReturn(CancellationToken.create());
		return new HierarchicalTestExecutor<>(request, new TestContext("root"), service,
			OpenTest4JAwareThrowableCollector::new).execute();
	}

	private record TestContext(String name) implements EngineExecutionContext {
	}

	@NullMarked
	private static class TestNode extends AbstractTestDescriptor implements Node<TestContext> {

		private final Type type;

		private TestNode(String name, Type type) {
			super(UniqueId.root("test", name), name);
			this.type = type;
		}

		@Override
		public Type getType() {
			return type;
		}
	}

	@NullMarked
	private static class RecordingListener implements EngineExecutionListener {

		private final Map<TestDescriptor, TestExecutionResult> results = new ConcurrentHashMap<>();

		private TestExecutionResult result(TestDescriptor descriptor) {
			return requireNonNull(results.get(descriptor));
		}

		@Override
		public void executionFinished(TestDescriptor descriptor, TestExecutionResult result) {
			results.put(descriptor, result);
		}
	}
}
