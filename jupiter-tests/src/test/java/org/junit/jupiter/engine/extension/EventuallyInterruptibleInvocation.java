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

import static org.assertj.core.api.Assertions.assertThat;

import java.util.stream.IntStream;

import org.junit.jupiter.api.extension.InvocationInterceptor.Invocation;

/**
 * @since 5.5
 */
class EventuallyInterruptibleInvocation implements Invocation<Void> {

	@Override
	public Void proceed() {
		while (!Thread.currentThread().isInterrupted()) {
			assertThat(IntStream.range(1, 1_000_000).sum()).isGreaterThan(0);
		}
		return null;
	}

}
