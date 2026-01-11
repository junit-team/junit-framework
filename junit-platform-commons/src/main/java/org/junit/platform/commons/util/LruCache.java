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

import static org.apiguardian.api.API.Status.INTERNAL;

import java.io.Serial;
import java.util.LinkedHashMap;
import java.util.Map;

import org.apiguardian.api.API;

/**
 * A simple LRU cache with a maximum size.
 *
 * <p>This class is not thread-safe.
 *
 * @param <K> the type of keys maintained by this cache
 * @param <V> the type of values maintained by this cache
 * @since 1.6
 */
@API(status = INTERNAL, since = "1.6")
public class LruCache<K, V> extends LinkedHashMap<K, V> {

	@Serial
	private static final long serialVersionUID = 1L;

	private final int maxSize;

	/**
	 * Create a new LRU cache that maintains at most the supplied number of
	 * entries.
	 *
	 * <p>For optimal use of the internal data structures, you should pick a
	 * number that's one below a power of two since this is based on a
	 * {@link java.util.HashMap} and the eldest entry will be evicted after
	 * adding the entry that increases the {@linkplain #size() size} to be above
	 * {@code maxSize}.
	 */
	public LruCache(int maxSize) {
		super(maxSize + 1, 1, true);
		this.maxSize = maxSize;
	}

	@Override
	protected boolean removeEldestEntry(Map.Entry<K, V> eldest) {
		return size() > maxSize;
	}

}
