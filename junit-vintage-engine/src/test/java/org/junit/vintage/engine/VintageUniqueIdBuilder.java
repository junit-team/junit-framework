/*
 * Copyright 2015-2026 the original author or authors.
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v2.0 which
 * accompanies this distribution and is available at
 *
 * https://www.eclipse.org/legal/epl-v20.html
 */

package org.junit.vintage.engine;

import org.junit.platform.engine.UniqueId;
import org.junit.vintage.engine.descriptor.VintageTestDescriptor;

/**
 * Test data builder for building unique IDs for the {@link VintageTestEngine}.
 *
 * Used to decouple tests from concrete unique ID strings.
 *
 * @since 4.12
 */
public class VintageUniqueIdBuilder {

	public static UniqueId uniqueIdForErrorInClass(Class<?> clazz, Class<?> failingClass) {
		return uniqueIdForClasses(clazz).append(VintageTestDescriptor.SEGMENT_TYPE_TEST,
			"initializationError(" + failingClass.getName() + ")");
	}

	public static UniqueId uniqueIdForClass(Class<?> clazz) {
		return uniqueIdForClasses(clazz);
	}

	public static UniqueId uniqueIdForClasses(Class<?> clazz, Class<?>... clazzes) {
		var uniqueId = uniqueIdForClass(clazz.getName());
		for (var each : clazzes) {
			uniqueId = uniqueId.append(VintageTestDescriptor.SEGMENT_TYPE_TEST, each.getName());
		}
		return uniqueId;
	}

	public static UniqueId uniqueIdForClass(String fullyQualifiedClassName) {
		var containerId = engineId();
		return containerId.append(VintageTestDescriptor.SEGMENT_TYPE_RUNNER, fullyQualifiedClassName);
	}

	public static UniqueId uniqueIdForMethod(Class<?> testClass, String methodName) {
		return uniqueIdForClass(testClass).append(VintageTestDescriptor.SEGMENT_TYPE_TEST,
			methodValue(testClass, methodName));
	}

	private static String methodValue(Class<?> testClass, String methodName) {
		return methodName + "(" + testClass.getName() + ")";
	}

	public static UniqueId uniqueIdForMethod(Class<?> testClass, String methodName, String index) {
		return uniqueIdForClass(testClass).append(VintageTestDescriptor.SEGMENT_TYPE_TEST,
			methodValue(testClass, methodName) + "[" + index + "]");
	}

	public static UniqueId uniqueIdForMethod(UniqueId containerId, Class<?> testClass, String methodName) {
		return containerId.append(VintageTestDescriptor.SEGMENT_TYPE_TEST, methodValue(testClass, methodName));
	}

	public static UniqueId engineId() {
		return UniqueId.forEngine(VintageTestDescriptor.ENGINE_ID);
	}

}
