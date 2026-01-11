/*
 * Copyright 2015-2026 the original author or authors.
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v2.0 which
 * accompanies this distribution and is available at
 *
 * https://www.eclipse.org/legal/epl-v20.html
 */

package org.junit.platform.engine;

/**
 * @since 6.0
 */
final class DisabledCancellationToken implements CancellationToken {

	static final DisabledCancellationToken INSTANCE = new DisabledCancellationToken();

	private DisabledCancellationToken() {
	}

	@Override
	public boolean isCancellationRequested() {
		return false;
	}

	@Override
	public void cancel() {
	}
}
