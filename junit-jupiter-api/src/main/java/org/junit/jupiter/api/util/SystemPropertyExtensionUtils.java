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

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collector;

import org.junit.jupiter.api.extension.ExtensionContext;

/**
 * Utility methods for the SystemPropertiesExtension.
 */
final class SystemPropertyExtensionUtils {

	private SystemPropertyExtensionUtils() {
		// private constructor to prevent instantiation of utility class
	}

	/**
	 * A {@link java.util.stream.Collectors#toSet() toSet} collector that throws an {@link IllegalStateException}
	 * on duplicate elements (according to {@link Object#equals(Object) equals}).
	 */
	public static <T> Collector<T, Set<T>, Set<T>> distinctToSet() {
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

	/**
	 * Find all (parent) {@code ExtensionContext}s via {@link ExtensionContext#getParent()}.
	 *
	 * @param context the context for which to find all (parent) contexts; never {@code null}
	 * @return a list of all contexts, "outwards" in the {@link ExtensionContext#getParent() getParent}-order,
	 *         beginning with the given context; never {@code null} or empty
	 */
	public static List<ExtensionContext> findAllContexts(ExtensionContext context) {
		List<ExtensionContext> allContexts = new ArrayList<>();
		for (var c = context; c != null; c = c.getParent().orElse(null)) {
			allContexts.add(c);
		}
		return allContexts;
	}
}
