/*
 * Copyright 2015-2025 the original author or authors.
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v2.0 which
 * accompanies this distribution and is available at
 *
 * https://www.eclipse.org/legal/epl-v20.html
 */

package org.junit.platform.engine;

import java.util.concurrent.atomic.AtomicBoolean;

class DefaultCancellationToken implements CancellationToken {

	private final AtomicBoolean cancelled = new AtomicBoolean();

	@Override
	public boolean isCancellationRequested() {
		return cancelled.get();
	}

	@Override
	public void cancel() {
		cancelled.set(true);
	}
}
