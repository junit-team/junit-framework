/*
 * Copyright 2015-2026 the original author or authors.
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v2.0 which
 * accompanies this distribution and is available at
 *
 * https://www.eclipse.org/legal/epl-v20.html
 */

package org.junit.platform.engine.support.hierarchical;

import java.util.List;
import java.util.concurrent.locks.Lock;

class ResourceLockSupport {

	static List<Lock> getLocks(ResourceLock resourceLock) {
		if (resourceLock instanceof NopLock) {
			return List.of();
		}
		if (resourceLock instanceof SingleLock lock) {
			return List.of(lock.getLock());
		}
		return ((CompositeLock) resourceLock).getLocks();
	}
}
