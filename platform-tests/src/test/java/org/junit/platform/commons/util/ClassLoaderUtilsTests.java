/*
 * Copyright 2015-2025 the original author or authors.
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v2.0 which
 * accompanies this distribution and is available at
 *
 * https://www.eclipse.org/legal/epl-v20.html
 */

package org.junit.platform.commons.util;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.platform.commons.test.PreconditionAssertions.assertPreconditionViolationNotNullFor;
import static org.mockito.Mockito.mock;

import org.junit.jupiter.api.Test;
import org.junit.platform.commons.test.TestClassLoader;

/**
 * Unit tests for {@link ClassLoaderUtils}.
 *
 * @since 1.0
 */
class ClassLoaderUtilsTests {

	@SuppressWarnings("DataFlowIssue")
	@Test
	void getClassLoaderPreconditions() {
		assertPreconditionViolationNotNullFor("Class", () -> ClassLoaderUtils.getClassLoader(null));
	}

	@Test
	void getClassLoaderForPrimitive() {
		assertThat(int.class.getClassLoader()).isNull();
		ClassLoader classLoader = ClassLoaderUtils.getClassLoader(int.class);
		assertThat(classLoader).isSameAs(getClass().getClassLoader());
	}

	@Test
	void getClassLoaderForWrapperType() {
		assertThat(Byte.class.getClassLoader()).isNull();
		ClassLoader classLoader = ClassLoaderUtils.getClassLoader(Byte.class);
		assertThat(classLoader).isSameAs(getClass().getClassLoader());
	}

	@Test
	void getClassLoaderForVoidType() {
		assertThat(void.class.getClassLoader()).isNull();
		ClassLoader classLoader = ClassLoaderUtils.getClassLoader(void.class);
		assertThat(classLoader).isSameAs(getClass().getClassLoader());
	}

	@Test
	void getClassLoaderForTestClass() {
		assertThat(getClass().getClassLoader()).isNotNull();
		ClassLoader classLoader = ClassLoaderUtils.getClassLoader(getClass());
		assertThat(classLoader).isSameAs(getClass().getClassLoader());
	}

	@Test
	void getClassLoaderForClassInDifferentClassLoader() throws Exception {
		try (var testClassLoader = TestClassLoader.forClasses(getClass())) {
			var testClass = testClassLoader.loadClass(getClass().getName());
			assertThat(testClass.getClassLoader()).isSameAs(testClassLoader);

			var classLoader = ClassLoaderUtils.getClassLoader(testClass);
			assertThat(classLoader).isSameAs(testClassLoader);
		}
	}

	@Test
	void getDefaultClassLoaderWithExplicitContextClassLoader() {
		var original = Thread.currentThread().getContextClassLoader();
		var mock = mock(ClassLoader.class);
		Thread.currentThread().setContextClassLoader(mock);
		try {
			assertSame(mock, ClassLoaderUtils.getDefaultClassLoader());
		}
		finally {
			Thread.currentThread().setContextClassLoader(original);
		}
	}

	@Test
	void getDefaultClassLoaderWithNullContextClassLoader() {
		var original = Thread.currentThread().getContextClassLoader();
		Thread.currentThread().setContextClassLoader(null);
		try {
			assertSame(ClassLoader.getSystemClassLoader(), ClassLoaderUtils.getDefaultClassLoader());
		}
		finally {
			Thread.currentThread().setContextClassLoader(original);
		}
	}

	@SuppressWarnings("DataFlowIssue")
	@Test
	void getLocationFromNullFails() {
		assertPreconditionViolationNotNullFor("object", () -> ClassLoaderUtils.getLocation(null));
	}

	@Test
	void getLocationFromVariousObjectsArePresent() {
		assertTrue(ClassLoaderUtils.getLocation(getClass()).isPresent());
		assertTrue(ClassLoaderUtils.getLocation(this).isPresent());
		assertTrue(ClassLoaderUtils.getLocation("").isPresent());
		assertTrue(ClassLoaderUtils.getLocation(0).isPresent());
		assertTrue(ClassLoaderUtils.getLocation(Thread.State.RUNNABLE).isPresent());
	}

}
