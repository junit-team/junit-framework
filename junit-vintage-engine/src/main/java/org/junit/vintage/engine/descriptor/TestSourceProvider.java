/*
 * Copyright 2015-2026 the original author or authors.
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v2.0 which
 * accompanies this distribution and is available at
 *
 * https://www.eclipse.org/legal/epl-v20.html
 */

package org.junit.vintage.engine.descriptor;

import static java.util.Collections.synchronizedMap;
import static java.util.function.Predicate.isEqual;
import static org.apiguardian.api.API.Status.INTERNAL;
import static org.junit.platform.commons.support.HierarchyTraversalMode.TOP_DOWN;
import static org.junit.platform.commons.support.ReflectionSupport.findMethods;
import static org.junit.platform.commons.util.FunctionUtils.where;

import java.lang.reflect.Method;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.apiguardian.api.API;
import org.jspecify.annotations.Nullable;
import org.junit.platform.commons.support.ModifierSupport;
import org.junit.platform.commons.util.LruCache;
import org.junit.platform.engine.TestSource;
import org.junit.platform.engine.support.descriptor.ClassSource;
import org.junit.platform.engine.support.descriptor.MethodSource;
import org.junit.runner.Description;

/**
 * @since 5.6
 */
@API(status = INTERNAL, since = "5.6")
public class TestSourceProvider {

	@SuppressWarnings("serial")
	private static final TestSource NULL_SOURCE = new TestSource() {
	};

	private final Map<Description, TestSource> testSourceCache = new ConcurrentHashMap<>();
	private final Map<Class<?>, List<Method>> methodsCache = synchronizedMap(new LruCache<>(31));

	public @Nullable TestSource findTestSource(Description description) {
		TestSource testSource = testSourceCache.computeIfAbsent(description, this::computeTestSource);
		return testSource == NULL_SOURCE ? null : testSource;
	}

	private TestSource computeTestSource(Description description) {
		Class<?> testClass = description.getTestClass();
		if (testClass != null) {
			String methodName = DescriptionUtils.getMethodName(description);
			if (methodName != null) {
				Method method = findMethod(testClass, sanitizeMethodName(methodName));
				if (method != null) {
					return MethodSource.from(testClass, method);
				}
			}
			return ClassSource.from(testClass);
		}
		return NULL_SOURCE;
	}

	private String sanitizeMethodName(String methodName) {
		if (methodName.contains("[") && methodName.endsWith("]")) {
			// special case for parameterized tests
			return methodName.substring(0, methodName.indexOf("["));
		}
		return methodName;
	}

	private @Nullable Method findMethod(Class<?> testClass, String methodName) {
		List<Method> methods = methodsCache.computeIfAbsent(testClass,
			clazz -> findMethods(clazz, m -> true, TOP_DOWN)).stream() //
				.filter(where(Method::getName, isEqual(methodName))) //
				.toList();
		if (methods.isEmpty()) {
			return null;
		}
		if (methods.size() == 1) {
			return methods.get(0);
		}
		methods = methods.stream().filter(ModifierSupport::isPublic).toList();
		if (methods.size() == 1) {
			return methods.get(0);
		}
		return null;
	}

}
