/*
 * Copyright 2026 the original author or authors.
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v2.0 which
 * accompanies this distribution and is available at
 *
 * https://www.eclipse.org/legal/epl-v20.html
 */

package org.junit.jupiter.engine.extension;

import java.util.function.Supplier;

import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.extension.InvocationInterceptor;
import org.junit.platform.commons.util.Preconditions;

record TimeoutInvocationParameters<T extends @Nullable Object>(//
		InvocationInterceptor.Invocation<T> invocation, TimeoutDuration timeout, Supplier<String> descriptionSupplier,
		PreInterruptCallbackInvocation preInterruptCallback, Boolean threadDumpEnabled) {

	TimeoutInvocationParameters {
		Preconditions.notNull(invocation, "invocation must not be null");
		Preconditions.notNull(timeout, "timeout must not be null");
		Preconditions.notNull(descriptionSupplier, "description supplier must not be null");
		Preconditions.notNull(preInterruptCallback, "preInterruptCallback must not be null");
	}
}
