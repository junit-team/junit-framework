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

import static java.util.stream.Collectors.joining;
import static org.apiguardian.api.API.Status.INTERNAL;

import java.util.List;
import java.util.ListIterator;
import java.util.concurrent.atomic.AtomicBoolean;

import org.apiguardian.api.API;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.extension.InvocationInterceptor;
import org.junit.jupiter.api.extension.InvocationInterceptor.Invocation;
import org.junit.jupiter.engine.extension.ExtensionRegistry;
import org.junit.platform.commons.JUnitException;
import org.junit.platform.commons.logging.Logger;
import org.junit.platform.commons.logging.LoggerFactory;
import org.junit.platform.commons.util.ExceptionUtils;

@API(status = INTERNAL, since = "5.5")
public class InvocationInterceptorChain {

	public <T extends @Nullable Object> T invoke(Invocation<T> invocation, ExtensionRegistry extensionRegistry,
			InterceptorCall<T> call) {
		List<InvocationInterceptor> interceptors = extensionRegistry.getExtensions(InvocationInterceptor.class);
		if (interceptors.isEmpty()) {
			return proceed(invocation);
		}
		return chainAndInvoke(invocation, call, interceptors);
	}

	private <T> T chainAndInvoke(Invocation<T> invocation, InterceptorCall<T> call,
			List<InvocationInterceptor> interceptors) {

		ValidatingInvocation<T> validatingInvocation = new ValidatingInvocation<>(invocation, interceptors);
		Invocation<T> chainedInvocation = chainInterceptors(validatingInvocation, call, interceptors);
		T result = proceed(chainedInvocation);
		validatingInvocation.verifyInvokedAtLeastOnce();
		return result;
	}

	private <T> Invocation<T> chainInterceptors(Invocation<T> invocation, InterceptorCall<T> call,
			List<InvocationInterceptor> interceptors) {

		Invocation<T> result = invocation;
		ListIterator<InvocationInterceptor> iterator = interceptors.listIterator(interceptors.size());
		while (iterator.hasPrevious()) {
			InvocationInterceptor interceptor = iterator.previous();
			result = new InterceptedInvocation<>(result, call, interceptor);
		}
		return result;
	}

	private <T> T proceed(Invocation<T> invocation) {
		try {
			return invocation.proceed();
		}
		catch (Throwable t) {
			throw ExceptionUtils.throwAsUncheckedException(t);
		}
	}

	@FunctionalInterface
	public interface InterceptorCall<T extends @Nullable Object> {

		T apply(InvocationInterceptor interceptor, Invocation<T> invocation) throws Throwable;

		static InterceptorCall<@Nullable Void> ofVoid(VoidInterceptorCall call) {
			return (InvocationInterceptor interceptorChain, Invocation<@Nullable Void> invocation) -> {
				call.apply(interceptorChain, invocation);
				return null;
			};
		}

	}

	@FunctionalInterface
	public interface VoidInterceptorCall {

		void apply(InvocationInterceptor interceptor, Invocation<@Nullable Void> invocation) throws Throwable;

	}

	private record InterceptedInvocation<T>(Invocation<T> invocation, InterceptorCall<T> call,
			InvocationInterceptor interceptor) implements Invocation<T> {

		@Override
		public T proceed() throws Throwable {
			return call.apply(interceptor, invocation);
		}

		@Override
		public void skip() {
			invocation.skip();
		}
	}

	private static class ValidatingInvocation<T extends @Nullable Object> implements Invocation<T> {

		private static final Logger logger = LoggerFactory.getLogger(ValidatingInvocation.class);

		private final AtomicBoolean invokedOrSkipped = new AtomicBoolean();
		private final Invocation<T> delegate;
		private final List<InvocationInterceptor> interceptors;

		ValidatingInvocation(Invocation<T> delegate, List<InvocationInterceptor> interceptors) {
			this.delegate = delegate;
			this.interceptors = interceptors;
		}

		@Override
		public T proceed() throws Throwable {
			markInvokedOrSkipped();
			return delegate.proceed();
		}

		@Override
		public void skip() {
			logger.debug(() -> "The invocation is skipped");
			markInvokedOrSkipped();
			delegate.skip();
		}

		private void markInvokedOrSkipped() {
			if (!invokedOrSkipped.compareAndSet(false, true)) {
				fail("Chain of InvocationInterceptors called invocation multiple times instead of just once");
			}
		}

		void verifyInvokedAtLeastOnce() {
			if (!invokedOrSkipped.get()) {
				fail("Chain of InvocationInterceptors never called invocation");
			}
		}

		private void fail(String prefix) {
			String commaSeparatedInterceptorClasses = interceptors.stream().map(Object::getClass).map(
				Class::getName).collect(joining(", "));
			throw new JUnitException(prefix + ": " + commaSeparatedInterceptorClasses);
		}
	}

}
