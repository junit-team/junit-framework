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

import java.util.List;

import org.junit.jupiter.api.extension.Extension;
import org.junit.jupiter.api.extension.TestTemplateInvocationContext;

/**
 * {@link TestTemplateInvocationContext} for a {@link org.junit.jupiter.api.RetryTest @RetryTest}.
 *
 * @since 6.1
 */
class RetryTestInvocationContext implements TestTemplateInvocationContext {

	private final DefaultRetryInfo retryInfo;
	private final RetryTestDisplayNameFormatter formatter;

	RetryTestInvocationContext(DefaultRetryInfo retryInfo, RetryTestDisplayNameFormatter formatter) {
		this.retryInfo = retryInfo;
		this.formatter = formatter;
	}

	@Override
	public String getDisplayName(int invocationIndex) {
		return this.formatter.format(this.retryInfo.currentAttempt(), this.retryInfo.maxAttempts());
	}

	@Override
	public List<Extension> getAdditionalExtensions() {
		return List.of(new RetryExtension(this.retryInfo));
	}

}
