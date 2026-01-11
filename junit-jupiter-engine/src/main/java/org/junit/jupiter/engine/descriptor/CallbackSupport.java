/*
 * Copyright 2015-2026 the original author or authors.
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v2.0 which
 * accompanies this distribution and is available at
 *
 * https://www.eclipse.org/legal/epl-v20.html
 */

package org.junit.jupiter.engine.descriptor;

import static org.junit.platform.commons.util.CollectionUtils.forEachInReverseOrder;

import org.junit.jupiter.api.extension.Extension;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.engine.execution.JupiterEngineExecutionContext;
import org.junit.jupiter.engine.extension.ExtensionRegistry;
import org.junit.platform.engine.support.hierarchical.ThrowableCollector;

/**
 * @since 5.13
 */
class CallbackSupport {

	static <T extends Extension> void invokeBeforeCallbacks(Class<T> type, JupiterEngineExecutionContext context,
			CallbackInvoker<T> callbackInvoker) {

		ExtensionRegistry registry = context.getExtensionRegistry();
		ExtensionContext extensionContext = context.getExtensionContext();
		ThrowableCollector throwableCollector = context.getThrowableCollector();

		for (T callback : registry.getExtensions(type)) {
			throwableCollector.execute(() -> callbackInvoker.invoke(callback, extensionContext));
			if (throwableCollector.isNotEmpty()) {
				break;
			}
		}
	}

	static <T extends Extension> void invokeAfterCallbacks(Class<T> type, JupiterEngineExecutionContext context,
			CallbackInvoker<T> callbackInvoker) {

		ExtensionRegistry registry = context.getExtensionRegistry();
		ExtensionContext extensionContext = context.getExtensionContext();
		ThrowableCollector throwableCollector = context.getThrowableCollector();

		forEachInReverseOrder(registry.getExtensions(type), //
			callback -> throwableCollector.execute(() -> callbackInvoker.invoke(callback, extensionContext)));
	}

	@FunctionalInterface
	protected interface CallbackInvoker<T extends Extension> {

		void invoke(T t, ExtensionContext context) throws Throwable;

	}

	private CallbackSupport() {
	}

}
