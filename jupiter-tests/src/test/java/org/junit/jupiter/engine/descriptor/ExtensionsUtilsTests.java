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

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.reflect.Field;
import java.util.function.Function;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.extension.Extension;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.junit.jupiter.engine.extension.ExtensionRegistrar;

/**
 * Tests for {@link ExtensionUtils}.
 *
 * @since 5.11.3
 */
class ExtensionsUtilsTests {

	@Test
	void registerExtensionsViaStaticFields() throws Exception {
		Field field = TestCase.class.getDeclaredField("staticField");
		ExtensionRegistrar registrar = mock();
		ExtensionUtils.registerExtensionsFromStaticFields(registrar, TestCase.class);
		verify(registrar).registerExtension(Extension1.class);
		verify(registrar).registerExtension(Extension2.class);
		verify(registrar).registerExtension(TestCase.staticField, field);
	}

	@Test
	@SuppressWarnings("unchecked")
	void registerExtensionsViaInstanceFields() throws Exception {
		Class<TestCase> testClass = TestCase.class;
		Field field = testClass.getDeclaredField("instanceField");
		ExtensionRegistrar registrar = mock();
		ExtensionUtils.registerExtensionsFromInstanceFields(registrar, testClass);
		verify(registrar).registerExtension(Extension1.class);
		verify(registrar).registerExtension(Extension2.class);
		verify(registrar).registerUninitializedExtension(eq(testClass), eq(field), any(Function.class));
	}

	static class Extension1 implements Extension {
	}

	static class Extension2 implements Extension {
	}

	static class Extension3 implements Extension {
	}

	static class Extension4 implements Extension {
	}

	@Retention(RetentionPolicy.RUNTIME)
	@ExtendWith(Extension1.class)
	@ExtendWith(Extension2.class)
	@interface UseCustomExtensions {
	}

	static class TestCase {

		@UseCustomExtensions
		@RegisterExtension
		static Extension3 staticField = new Extension3();

		@UseCustomExtensions
		@RegisterExtension
		Extension4 instanceField = new Extension4();

	}

}
