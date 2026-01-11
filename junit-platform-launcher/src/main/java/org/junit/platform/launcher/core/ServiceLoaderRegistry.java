/*
 * Copyright 2015-2026 the original author or authors.
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v2.0 which
 * accompanies this distribution and is available at
 *
 * https://www.eclipse.org/legal/epl-v20.html
 */

package org.junit.platform.launcher.core;

import java.util.ArrayList;
import java.util.List;
import java.util.ServiceLoader;
import java.util.function.Function;
import java.util.function.Predicate;

import org.jspecify.annotations.Nullable;
import org.junit.platform.commons.logging.Logger;
import org.junit.platform.commons.logging.LoggerFactory;
import org.junit.platform.commons.util.ClassLoaderUtils;
import org.junit.platform.commons.util.ServiceLoaderUtils;

/**
 * @since 1.8
 */
class ServiceLoaderRegistry {

	static <T> Iterable<T> load(Class<T> type) {
		return load(type, __ -> true, instances -> logLoadedInstances(type, instances, null));
	}

	static <T> Iterable<T> load(@SuppressWarnings("SameParameterValue") Class<T> type,
			Predicate<String> classNameFilter) {
		List<String> exclusions = new ArrayList<>();
		Predicate<String> collectingClassNameFilter = className -> {
			boolean included = classNameFilter.test(className);
			if (!included) {
				exclusions.add(className);
			}
			return included;
		};
		return load(type, collectingClassNameFilter, instances -> logLoadedInstances(type, instances, exclusions));
	}

	private static <T> String logLoadedInstances(Class<T> type, List<T> instances, @Nullable List<String> exclusions) {
		String typeName = type.getSimpleName();
		if (exclusions == null) {
			return "Loaded %s instances: %s".formatted(typeName, instances);
		}
		return "Loaded %s instances: %s (excluded classes: %s)".formatted(typeName, instances, exclusions);
	}

	private static <T> List<T> load(Class<T> type, Predicate<String> classNameFilter,
			Function<List<T>, String> logMessageSupplier) {
		ServiceLoader<T> serviceLoader = ServiceLoader.load(type, ClassLoaderUtils.getDefaultClassLoader());
		Predicate<Class<? extends T>> providerPredicate = clazz -> classNameFilter.test(clazz.getName());
		List<T> instances = ServiceLoaderUtils.filter(serviceLoader, providerPredicate).toList();
		getLogger().config(() -> logMessageSupplier.apply(instances));
		return instances;
	}

	private static Logger getLogger() {
		// Not a constant to avoid problems with building GraalVM native images
		return LoggerFactory.getLogger(ServiceLoaderRegistry.class);
	}

	private ServiceLoaderRegistry() {
	}

}
