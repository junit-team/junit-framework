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

import static org.junit.jupiter.api.Timeout.ThreadMode.SAME_THREAD;
import static org.junit.jupiter.api.extension.PreInterruptCallback.THREAD_DUMP_ENABLED_PROPERTY_NAME;

import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Method;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.Function;

import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.api.Timeout.ThreadMode;
import org.junit.jupiter.api.extension.AsyncInvocationInterceptor;
import org.junit.jupiter.api.extension.AsyncInvocationInterceptor.AsyncInvocation;
import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.BeforeEachCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.InvocationInterceptor;
import org.junit.jupiter.api.extension.ReflectiveInvocationContext;
import org.junit.platform.commons.support.AnnotationSupport;
import org.junit.platform.commons.util.ClassUtils;
import org.junit.platform.commons.util.ReflectionUtils;

/**
 * @since 5.5
 */
class TimeoutExtension
		implements BeforeAllCallback, BeforeEachCallback, InvocationInterceptor, AsyncInvocationInterceptor {

	private static final ExtensionContext.Namespace NAMESPACE = ExtensionContext.Namespace.create(Timeout.class);
	private static final String TESTABLE_METHOD_TIMEOUT_KEY = "testable_method_timeout_from_annotation";
	private static final String TESTABLE_METHOD_TIMEOUT_THREAD_MODE_KEY = "testable_method_timeout_thread_mode_from_annotation";
	private static final String GLOBAL_TIMEOUT_CONFIG_KEY = "global_timeout_config";

	@Override
	public ExtensionContextScope getTestInstantiationExtensionContextScope(ExtensionContext rootContext) {
		return ExtensionContextScope.TEST_METHOD;
	}

	@Override
	public void beforeAll(ExtensionContext context) {
		readAndStoreTimeoutSoChildrenInheritIt(context);
	}

	@Override
	public void beforeEach(ExtensionContext context) {
		readAndStoreTimeoutSoChildrenInheritIt(context);
	}

	private void readAndStoreTimeoutSoChildrenInheritIt(ExtensionContext context) {
		readTimeoutFromAnnotation(context.getElement()).ifPresent(
			timeout -> context.getStore(NAMESPACE).put(TESTABLE_METHOD_TIMEOUT_KEY, timeout));
		readTimeoutThreadModeFromAnnotation(context.getElement()).ifPresent(
			timeoutThreadMode -> context.getStore(NAMESPACE).put(TESTABLE_METHOD_TIMEOUT_THREAD_MODE_KEY,
				timeoutThreadMode));
	}

	@Override
	public void interceptBeforeAllMethod(Invocation<@Nullable Void> invocation,
			ReflectiveInvocationContext<Method> invocationContext, ExtensionContext extensionContext) throws Throwable {

		interceptLifecycleMethod(invocation, invocationContext, extensionContext,
			TimeoutConfiguration::getDefaultBeforeAllMethodTimeout);
	}

	@Override
	public void interceptBeforeEachMethod(Invocation<@Nullable Void> invocation,
			ReflectiveInvocationContext<Method> invocationContext, ExtensionContext extensionContext) throws Throwable {

		interceptLifecycleMethod(invocation, invocationContext, extensionContext,
			TimeoutConfiguration::getDefaultBeforeEachMethodTimeout);
	}

	@Override
	public void interceptTestMethod(Invocation<@Nullable Void> invocation,
			ReflectiveInvocationContext<Method> invocationContext, ExtensionContext extensionContext) throws Throwable {

		this.<@Nullable Void> interceptTestableMethod(invocation, invocationContext, extensionContext,
			TimeoutConfiguration::getDefaultTestMethodTimeout);
	}

	@Override
	public void interceptTestTemplateMethod(Invocation<@Nullable Void> invocation,
			ReflectiveInvocationContext<Method> invocationContext, ExtensionContext extensionContext) throws Throwable {

		this.<@Nullable Void> interceptTestableMethod(invocation, invocationContext, extensionContext,
			TimeoutConfiguration::getDefaultTestTemplateMethodTimeout);
	}

	@Override
	public <T extends @Nullable Object> T interceptTestFactoryMethod(Invocation<T> invocation,
			ReflectiveInvocationContext<Method> invocationContext, ExtensionContext extensionContext) throws Throwable {

		return this.<T> interceptTestableMethod(invocation, invocationContext, extensionContext,
			TimeoutConfiguration::getDefaultTestFactoryMethodTimeout);
	}

	@Override
	public void interceptAfterEachMethod(Invocation<@Nullable Void> invocation,
			ReflectiveInvocationContext<Method> invocationContext, ExtensionContext extensionContext) throws Throwable {

		interceptLifecycleMethod(invocation, invocationContext, extensionContext,
			TimeoutConfiguration::getDefaultAfterEachMethodTimeout);
	}

	@Override
	public void interceptAfterAllMethod(Invocation<@Nullable Void> invocation,
			ReflectiveInvocationContext<Method> invocationContext, ExtensionContext extensionContext) throws Throwable {

		interceptLifecycleMethod(invocation, invocationContext, extensionContext,
			TimeoutConfiguration::getDefaultAfterAllMethodTimeout);
	}

	// --- Asynchronous interceptor methods ---------------------------------

	@Override
	public CompletionStage<Void> interceptBeforeAllMethodAsync(AsyncInvocation<Void> invocation,
			ReflectiveInvocationContext<Method> invocationContext, ExtensionContext extensionContext) {
		return interceptAsync(invocation, invocationContext, extensionContext,
			TimeoutConfiguration::getDefaultBeforeAllMethodTimeout,
			readTimeoutFromAnnotation(Optional.of(invocationContext.getExecutable())));
	}

	@Override
	public CompletionStage<Void> interceptBeforeEachMethodAsync(AsyncInvocation<Void> invocation,
			ReflectiveInvocationContext<Method> invocationContext, ExtensionContext extensionContext) {
		return interceptAsync(invocation, invocationContext, extensionContext,
			TimeoutConfiguration::getDefaultBeforeEachMethodTimeout,
			readTimeoutFromAnnotation(Optional.of(invocationContext.getExecutable())));
	}

	@Override
	public CompletionStage<Void> interceptTestMethodAsync(AsyncInvocation<Void> invocation,
			ReflectiveInvocationContext<Method> invocationContext, ExtensionContext extensionContext) {
		TimeoutDuration explicitTimeout = extensionContext.getStore(NAMESPACE).get(TESTABLE_METHOD_TIMEOUT_KEY,
			TimeoutDuration.class);
		return interceptAsync(invocation, invocationContext, extensionContext,
			TimeoutConfiguration::getDefaultTestMethodTimeout, Optional.ofNullable(explicitTimeout));
	}

	@Override
	public CompletionStage<Void> interceptTestTemplateMethodAsync(AsyncInvocation<Void> invocation,
			ReflectiveInvocationContext<Method> invocationContext, ExtensionContext extensionContext) {
		TimeoutDuration explicitTimeout = extensionContext.getStore(NAMESPACE).get(TESTABLE_METHOD_TIMEOUT_KEY,
			TimeoutDuration.class);
		return interceptAsync(invocation, invocationContext, extensionContext,
			TimeoutConfiguration::getDefaultTestTemplateMethodTimeout, Optional.ofNullable(explicitTimeout));
	}

	@Override
	public <T extends @Nullable Object> CompletionStage<T> interceptTestFactoryMethodAsync(
			AsyncInvocation<T> invocation, ReflectiveInvocationContext<Method> invocationContext,
			ExtensionContext extensionContext) {
		TimeoutDuration explicitTimeout = extensionContext.getStore(NAMESPACE).get(TESTABLE_METHOD_TIMEOUT_KEY,
			TimeoutDuration.class);
		return interceptAsyncResult(invocation, invocationContext, extensionContext,
			TimeoutConfiguration::getDefaultTestFactoryMethodTimeout, Optional.ofNullable(explicitTimeout));
	}

	@Override
	public CompletionStage<Void> interceptAfterEachMethodAsync(AsyncInvocation<Void> invocation,
			ReflectiveInvocationContext<Method> invocationContext, ExtensionContext extensionContext) {
		return interceptAsync(invocation, invocationContext, extensionContext,
			TimeoutConfiguration::getDefaultAfterEachMethodTimeout,
			readTimeoutFromAnnotation(Optional.of(invocationContext.getExecutable())));
	}

	@Override
	public CompletionStage<Void> interceptAfterAllMethodAsync(AsyncInvocation<Void> invocation,
			ReflectiveInvocationContext<Method> invocationContext, ExtensionContext extensionContext) {
		return interceptAsync(invocation, invocationContext, extensionContext,
			TimeoutConfiguration::getDefaultAfterAllMethodTimeout,
			readTimeoutFromAnnotation(Optional.of(invocationContext.getExecutable())));
	}

	@SuppressWarnings("UnusedVariable")
	private CompletionStage<Void> interceptAsync(AsyncInvocation<Void> invocation,
			ReflectiveInvocationContext<Method> invocationContext, ExtensionContext extensionContext,
			TimeoutProvider defaultTimeoutProvider, Optional<TimeoutDuration> explicitTimeout) {
		return interceptAsync(invocation.proceedAsync(), extensionContext, defaultTimeoutProvider, explicitTimeout);
	}

	private <T extends @Nullable Object> CompletionStage<T> interceptAsync(CompletionStage<T> base,
			ExtensionContext extensionContext, TimeoutProvider defaultTimeoutProvider,
			Optional<TimeoutDuration> explicitTimeout) {
		TimeoutConfiguration timeoutConfiguration = getGlobalTimeoutConfiguration(extensionContext);
		if (timeoutConfiguration.isTimeoutDisabled()) {
			return base;
		}
		TimeoutDuration timeout = explicitTimeout.orElseGet(
			() -> getDefaultTimeout(defaultTimeoutProvider, timeoutConfiguration));
		if (timeout == null) {
			return base;
		}
		var threadMode = resolveTimeoutThreadMode(extensionContext, timeoutConfiguration);
		return applyTimeout(base, timeout, threadMode);
	}

	@SuppressWarnings("UnusedVariable")
	private <T extends @Nullable Object> CompletionStage<T> interceptAsyncResult(AsyncInvocation<T> invocation,
			ReflectiveInvocationContext<Method> invocationContext, ExtensionContext extensionContext,
			TimeoutProvider defaultTimeoutProvider, Optional<TimeoutDuration> explicitTimeout) {
		return interceptAsync(invocation.proceedAsync(), extensionContext, defaultTimeoutProvider, explicitTimeout);
	}

	/**
	 * Apply {@code orTimeout} to the supplied stage. For {@link ThreadMode#SEPARATE_THREAD} the
	 * thread completing the original stage is interrupted when the timeout fires; if the async work
	 * is not interruptible (e.g. its stage is already complete), the timeout still fires and the
	 * timed-out invocation is reported as failed, never as successful.
	 */
	@SuppressWarnings({ "FutureReturnValueIgnored", "UnusedVariable" })
	private <T extends @Nullable Object> CompletionStage<T> applyTimeout(CompletionStage<T> base,
			TimeoutDuration timeout, ThreadMode threadMode) {
		long millis = timeout.toDuration().toMillis();
		CompletableFuture<T> future = base.toCompletableFuture();
		if (threadMode == ThreadMode.SEPARATE_THREAD) {
			var completingThread = new java.util.concurrent.atomic.AtomicReference<Thread>();
			future.whenComplete((___, throwable) -> completingThread.set(Thread.currentThread()));
			CompletableFuture<T> raced = future.orTimeout(millis, TimeUnit.MILLISECONDS);
			raced.whenComplete((value, throwable) -> {
				if (containsTimeout(throwable)) {
					// Best-effort interruption of the thread finishing the async
					// work. If the body is not thread-bound (interruption lost),
					// the timed-out invocation is still reported as failed; the
					// late-arriving success never flips it to passed.
					Thread thread = completingThread.get();
					if (thread != null && !thread.equals(Thread.currentThread())) {
						thread.interrupt();
					}
				}
			});
			return raced;
		}
		return future.orTimeout(millis, TimeUnit.MILLISECONDS);
	}

	private static boolean containsTimeout(Throwable throwable) {
		for (Throwable current = throwable; current != null; current = current.getCause()) {
			if (current instanceof TimeoutException) {
				return true;
			}
		}
		return false;
	}

	private void interceptLifecycleMethod(Invocation<@Nullable Void> invocation,
			ReflectiveInvocationContext<Method> invocationContext, ExtensionContext extensionContext,
			TimeoutProvider defaultTimeoutProvider) throws Throwable {

		TimeoutDuration timeout = readTimeoutFromAnnotation(Optional.of(invocationContext.getExecutable())).orElse(
			null);
		this.<@Nullable Void> intercept(invocation, invocationContext, extensionContext, timeout,
			defaultTimeoutProvider);
	}

	@SuppressWarnings("OptionalUsedAsFieldOrParameterType")
	private Optional<TimeoutDuration> readTimeoutFromAnnotation(Optional<AnnotatedElement> element) {
		return AnnotationSupport.findAnnotation(element, Timeout.class).map(TimeoutDuration::from);
	}

	@SuppressWarnings("OptionalUsedAsFieldOrParameterType")
	private Optional<ThreadMode> readTimeoutThreadModeFromAnnotation(Optional<AnnotatedElement> element) {
		return AnnotationSupport.findAnnotation(element, Timeout.class).map(Timeout::threadMode);
	}

	private <T extends @Nullable Object> T interceptTestableMethod(Invocation<T> invocation,
			ReflectiveInvocationContext<Method> invocationContext, ExtensionContext extensionContext,
			TimeoutProvider defaultTimeoutProvider) throws Throwable {

		TimeoutDuration timeout = extensionContext.getStore(NAMESPACE).get(TESTABLE_METHOD_TIMEOUT_KEY,
			TimeoutDuration.class);
		return intercept(invocation, invocationContext, extensionContext, timeout, defaultTimeoutProvider);
	}

	private <T extends @Nullable Object> T intercept(Invocation<T> invocation,
			ReflectiveInvocationContext<Method> invocationContext, ExtensionContext extensionContext,
			@Nullable TimeoutDuration explicitTimeout, TimeoutProvider defaultTimeoutProvider) throws Throwable {

		TimeoutConfiguration timeoutConfiguration = getGlobalTimeoutConfiguration(extensionContext);
		if (timeoutConfiguration.isTimeoutDisabled()) {
			return invocation.proceed();
		}

		TimeoutDuration timeout = explicitTimeout == null
				? getDefaultTimeout(defaultTimeoutProvider, timeoutConfiguration)
				: explicitTimeout;
		return decorate(invocation, invocationContext, extensionContext, timeout, timeoutConfiguration).proceed();
	}

	private @Nullable TimeoutDuration getDefaultTimeout(TimeoutProvider defaultTimeoutProvider,
			TimeoutConfiguration timeoutConfiguration) {

		return defaultTimeoutProvider.apply(timeoutConfiguration).orElse(null);
	}

	private TimeoutConfiguration getGlobalTimeoutConfiguration(ExtensionContext extensionContext) {
		ExtensionContext root = extensionContext.getRoot();
		return root.getStore(NAMESPACE).computeIfAbsent(GLOBAL_TIMEOUT_CONFIG_KEY, __ -> new TimeoutConfiguration(root),
			TimeoutConfiguration.class);
	}

	private <T extends @Nullable Object> Invocation<T> decorate(Invocation<T> invocation,
			ReflectiveInvocationContext<Method> invocationContext, ExtensionContext extensionContext,
			@Nullable TimeoutDuration timeout, TimeoutConfiguration timeoutConfiguration) {

		if (timeout == null) {
			return invocation;
		}

		var threadMode = resolveTimeoutThreadMode(extensionContext, timeoutConfiguration);
		return new TimeoutInvocationFactory(extensionContext.getRoot().getStore(NAMESPACE)) //
				.create(threadMode, createParameters(invocation, invocationContext, extensionContext, timeout));
	}

	private <T extends @Nullable Object> TimeoutInvocationParameters<T> createParameters(Invocation<T> invocation,
			ReflectiveInvocationContext<Method> invocationContext, ExtensionContext extensionContext,
			TimeoutDuration timeout) {
		var threadDumpEnabled = extensionContext.getConfigurationParameter(THREAD_DUMP_ENABLED_PROPERTY_NAME) //
				.map(Boolean::parseBoolean) //
				.orElse(false);
		return new TimeoutInvocationParameters<>(invocation, timeout,
			() -> describe(invocationContext, extensionContext),
			PreInterruptCallbackInvocationFactory.create((ExtensionContextInternal) extensionContext),
			threadDumpEnabled);
	}

	private ThreadMode resolveTimeoutThreadMode(ExtensionContext extensionContext,
			TimeoutConfiguration timeoutConfiguration) {
		ThreadMode annotationThreadMode = getAnnotationThreadMode(extensionContext);
		if (annotationThreadMode == null || annotationThreadMode == ThreadMode.INFERRED) {
			return timeoutConfiguration.getDefaultTimeoutThreadMode().orElse(SAME_THREAD);
		}
		return annotationThreadMode;
	}

	private @Nullable ThreadMode getAnnotationThreadMode(ExtensionContext extensionContext) {
		return extensionContext.getStore(NAMESPACE).get(TESTABLE_METHOD_TIMEOUT_THREAD_MODE_KEY, ThreadMode.class);
	}

	private String describe(ReflectiveInvocationContext<Method> invocationContext, ExtensionContext extensionContext) {
		Method method = invocationContext.getExecutable();
		Optional<Class<?>> testClass = extensionContext.getTestClass();
		if (testClass.isPresent() && invocationContext.getTargetClass().equals(testClass.get())) {
			return "%s(%s)".formatted(method.getName(), ClassUtils.nullSafeToString(method.getParameterTypes()));
		}
		return ReflectionUtils.getFullyQualifiedMethodName(invocationContext.getTargetClass(), method);
	}

	@FunctionalInterface
	private interface TimeoutProvider extends Function<TimeoutConfiguration, Optional<TimeoutDuration>> {
	}
}
