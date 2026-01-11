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

import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Method;
import java.util.Optional;
import java.util.function.Function;

import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.api.Timeout.ThreadMode;
import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.BeforeEachCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.InvocationInterceptor;
import org.junit.jupiter.api.extension.ReflectiveInvocationContext;
import org.junit.jupiter.engine.extension.TimeoutInvocationFactory.TimeoutInvocationParameters;
import org.junit.platform.commons.support.AnnotationSupport;
import org.junit.platform.commons.util.ClassUtils;
import org.junit.platform.commons.util.ReflectionUtils;

/**
 * @since 5.5
 */
class TimeoutExtension implements BeforeAllCallback, BeforeEachCallback, InvocationInterceptor {

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

		ThreadMode threadMode = resolveTimeoutThreadMode(extensionContext, timeoutConfiguration);
		return new TimeoutInvocationFactory(extensionContext.getRoot().getStore(NAMESPACE)).create(threadMode,
			new TimeoutInvocationParameters<>(invocation, timeout, () -> describe(invocationContext, extensionContext),
				PreInterruptCallbackInvocationFactory.create((ExtensionContextInternal) extensionContext)));
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
