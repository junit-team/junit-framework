/*
 * Copyright 2015-2026 the original author or authors.
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v2.0 which
 * accompanies this distribution and is available at
 *
 * https://www.eclipse.org/legal/epl-v20.html
 */

package org.junit.jupiter.engine;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.InvocationInterceptor;
import org.junit.jupiter.api.extension.TestMethodReturnValueHandler;

public class CompletableFutureReturnValueHandler implements TestMethodReturnValueHandler {

	@Override
	public boolean supportsReturnType(Class<?> returnType) {
		return CompletableFuture.class.isAssignableFrom(returnType);
	}

	@Override
	public void execute(InvocationInterceptor.Invocation<Object> invocation, ExtensionContext context) throws Throwable {
		Object result = invocation.proceed();
		if (result == null) {
			return;
		}
		try {
			((CompletableFuture<?>) result).get(30, TimeUnit.SECONDS);
		}
		catch (ExecutionException ex) {
			throw ex.getCause();
		}
		catch (TimeoutException ex) {
			throw new AssertionError("CompletableFuture did not complete within 30 seconds", ex);
		}
	}

}
