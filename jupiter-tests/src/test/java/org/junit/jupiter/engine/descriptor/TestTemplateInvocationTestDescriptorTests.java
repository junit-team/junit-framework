/*
 * Copyright 2015-2026 the original author or authors.
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v2.0 which
 * accompanies this distribution and is available at
 *
 * https://www.eclipse.org/legal/epl-v20.html
 */

package org.junit.jupiter.engine.descriptor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.lang.reflect.Method;
import java.net.URI;
import java.util.List;

import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestTemplate;
import org.junit.jupiter.api.extension.TestTemplateInvocationContext;
import org.junit.jupiter.api.parallel.ResourceLock;
import org.junit.jupiter.engine.config.JupiterConfiguration;
import org.junit.platform.engine.UniqueId;
import org.junit.platform.engine.support.descriptor.ClasspathResourceSource;
import org.junit.platform.engine.support.descriptor.FilePosition;
import org.junit.platform.engine.support.descriptor.MethodSource;

class TestTemplateInvocationTestDescriptorTests {

	@Test
	void invocationsDoNotDeclareExclusiveResources() throws Exception {
		Class<MyTestCase> testClass = MyTestCase.class;
		Method testTemplateMethod = testClass.getDeclaredMethod("testTemplate");
		JupiterConfiguration configuration = mock();
		when(configuration.getDefaultDisplayNameGenerator()).thenReturn(new DisplayNameGenerator.Standard());
		TestTemplateTestDescriptor parent = new TestTemplateTestDescriptor(UniqueId.root("segment", "template"),
			testClass, testTemplateMethod, List::of, configuration);
		TestTemplateInvocationContext invocationContext = mock();
		when(invocationContext.getDisplayName(anyInt())).thenReturn("invocation");

		TestTemplateInvocationTestDescriptor testDescriptor = new TestTemplateInvocationTestDescriptor(
			parent.getUniqueId().append(TestTemplateInvocationTestDescriptor.SEGMENT_TYPE, "1"), testClass,
			testTemplateMethod, invocationContext, 1, configuration);

		assertThat(parent.getExclusiveResources()).hasSize(1);
		assertThat(testDescriptor.getExclusiveResources()).isEmpty();
	}

	@Test
	void usesMethodSourceWhenTestSourceUriIsNull() throws Exception {
		Class<MyTestCase> testClass = MyTestCase.class;
		Method testTemplateMethod = testClass.getDeclaredMethod("testTemplate");
		JupiterConfiguration configuration = mock();
		TestTemplateInvocationContext invocationContext = new TestTemplateInvocationContext() {
		};

		TestTemplateInvocationTestDescriptor testDescriptor = new TestTemplateInvocationTestDescriptor(
			UniqueId.root("segment", "template").append(TestTemplateInvocationTestDescriptor.SEGMENT_TYPE, "1"),
			testClass, testTemplateMethod, invocationContext, 1, configuration);

		assertThat(testDescriptor.getSource()).contains(MethodSource.from(testClass, testTemplateMethod));
	}

	@Test
	void convertsCustomTestSourceUriAndPreservesItOnCopy() throws Exception {
		Class<MyTestCase> testClass = MyTestCase.class;
		Method testTemplateMethod = testClass.getDeclaredMethod("testTemplate");
		JupiterConfiguration configuration = mock();
		URI customUri = URI.create("classpath:/test.js?line=42&column=21");
		TestTemplateInvocationContext invocationContext = new TestTemplateInvocationContext() {
			@Override
			public URI testSourceUri() {
				return customUri;
			}
		};

		UniqueId originalUniqueId = UniqueId.root("segment", "template").append(
			TestTemplateInvocationTestDescriptor.SEGMENT_TYPE, "1");
		TestTemplateInvocationTestDescriptor testDescriptor = new TestTemplateInvocationTestDescriptor(originalUniqueId,
			testClass, testTemplateMethod, invocationContext, 1, configuration);

		ClasspathResourceSource expectedSource = ClasspathResourceSource.from("test.js", FilePosition.from(42, 21));
		assertThat(testDescriptor.getSource()).contains(expectedSource);

		UniqueId newUniqueId = UniqueId.root("segment", "other").append(
			TestTemplateInvocationTestDescriptor.SEGMENT_TYPE, "1");
		TestTemplateInvocationTestDescriptor copy = testDescriptor.withUniqueId(__ -> newUniqueId);
		assertThat(copy.getSource()).contains(expectedSource);
	}

	static class MyTestCase {
		@TestTemplate
		@ResourceLock("a")
		void testTemplate() {
		}
	}

}
