/*
 * Copyright 2015-2026 the original author or authors.
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v2.0 which
 * accompanies this distribution and is available at
 *
 * https://www.eclipse.org/legal/epl-v20.html
 */

package org.junit.platform.commons.util;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.entry;

import org.junit.jupiter.api.Test;

/**
 * @since 1.6
 */
class LruCacheTests {

	@Test
	void evictsEldestEntryWhenMaxSizeIsReached() {
		var cache = new LruCache<Integer, Integer>(1);

		cache.put(0, 0);
		cache.put(1, 1);

		assertThat(cache) //
				.doesNotContain(entry(0, 0)) //
				.hasSize(1);
	}

}
