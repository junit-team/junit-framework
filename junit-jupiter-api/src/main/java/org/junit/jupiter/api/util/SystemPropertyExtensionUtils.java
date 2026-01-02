/*
 * Copyright 2015-2025 the original author or authors.
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v2.0 which
 * accompanies this distribution and is available at
 *
 * https://www.eclipse.org/legal/epl-v20.html
 */

package org.junit.jupiter.api.util;

import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collector;

/**
 *  @since 6.1
 */
final class SystemPropertyExtensionUtils {

	private SystemPropertyExtensionUtils() {
		/* no-op */
	}

	/**
	 * A {@link java.util.stream.Collectors#toSet() toSet} collector that throws an {@link IllegalStateException}
	 * on duplicate elements (according to {@link Object#equals(Object) equals}).
	 */
	static <T> Collector<T, Set<T>, Set<T>> distinctToSet() {
		return Collector.of(HashSet::new, SystemPropertyExtensionUtils::addButThrowIfDuplicate, (left, right) -> {
			right.forEach(element -> addButThrowIfDuplicate(left, element));
			return left;
		});
	}

	private static <T> void addButThrowIfDuplicate(Set<T> set, T element) {
		boolean newElement = set.add(element);
		if (!newElement) {
			throw new IllegalStateException("Duplicate element '" + element + "'.");
		}
	}

}
