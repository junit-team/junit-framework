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

import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.condition.OS.WINDOWS;

import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicReference;

import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout.ThreadMode;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.InvocationInterceptor.Invocation;
import org.junit.jupiter.engine.execution.NamespaceAwareStore;
import org.junit.jupiter.engine.extension.TimeoutInvocationFactory.TimeoutInvocationParameters;
import org.junit.platform.engine.support.store.Namespace;
import org.junit.platform.engine.support.store.NamespacedHierarchicalStore;

/**
 * @since 5.9
 */
@DisplayName("SeparateThreadTimeoutInvocation")
class SeparateThreadTimeoutInvocationTests {

	private static final long PREEMPTIVE_TIMEOUT_MILLIS = WINDOWS.isCurrentOs() ? 1000 : 100;

	@Test
	@DisplayName("throws timeout exception when timeout duration is exceeded")
	void throwsTimeoutException() {
		AtomicReference<String> threadName = new AtomicReference<>();
		var invocation = aSeparateThreadInvocation(() -> {
			threadName.set(Thread.currentThread().getName());
			Thread.sleep(PREEMPTIVE_TIMEOUT_MILLIS * 2);
			return "ignored";
		});

		assertThatThrownBy(invocation::proceed) //
				.hasMessage("method() timed out after " + PREEMPTIVE_TIMEOUT_MILLIS + " milliseconds") //
				.isInstanceOf(TimeoutException.class) //
				.hasRootCauseMessage("Execution timed out in thread " + threadName.get());
	}

	@Test
	@DisplayName("executes invocation in a separate thread")
	void runsInvocationUsingSeparateThread() throws Throwable {
		var invocationThreadName = aSeparateThreadInvocation(() -> Thread.currentThread().getName()).proceed();
		assertThat(invocationThreadName).isNotEqualTo(Thread.currentThread().getName());
	}

	@Test
	@DisplayName("throws invocation exception")
	void shouldThrowInvocationException() {
		var invocation = aSeparateThreadInvocation(() -> {
			throw new RuntimeException("hi!");
		});
		assertThatThrownBy(invocation::proceed) //
				.isInstanceOf(RuntimeException.class) //
				.hasMessage("hi!");
	}

	private static <T extends @Nullable Object> SeparateThreadTimeoutInvocation<T> aSeparateThreadInvocation(
			Invocation<T> invocation) {
		var namespace = ExtensionContext.Namespace.create(SeparateThreadTimeoutInvocationTests.class);
		var store = new NamespaceAwareStore(new NamespacedHierarchicalStore<>(null),
			Namespace.create(namespace.getParts()));
		var parameters = new TimeoutInvocationParameters<>(invocation,
			new TimeoutDuration(PREEMPTIVE_TIMEOUT_MILLIS, MILLISECONDS), () -> "method()",
			PreInterruptCallbackInvocation.NOOP);
		return (SeparateThreadTimeoutInvocation<T>) new TimeoutInvocationFactory(store) //
				.create(ThreadMode.SEPARATE_THREAD, parameters);
	}
}
