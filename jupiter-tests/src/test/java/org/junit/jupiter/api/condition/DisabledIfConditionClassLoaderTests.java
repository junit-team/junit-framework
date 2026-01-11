/*
 * Copyright 2015-2026 the original author or authors.
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v2.0 which
 * accompanies this distribution and is available at
 *
 * https://www.eclipse.org/legal/epl-v20.html
 */

package org.junit.jupiter.api.condition;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.lang.reflect.Method;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ConditionEvaluationResult;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.platform.commons.support.ReflectionSupport;
import org.junit.platform.commons.test.TestClassLoader;

/**
 * Tests for {@link DisabledIfCondition} using custom {@link ClassLoader} arrangements.
 *
 * @since 5.10
 */
public class DisabledIfConditionClassLoaderTests {

	@Test
	// No need to introduce a "disabled" version of this test, since it would simply be the
	// logical inverse of this method and would therefore not provide any further benefit.
	void enabledWithStaticMethodInTypeFromDifferentClassLoader() throws Exception {
		try (var testClassLoader = TestClassLoader.forClasses(getClass(), StaticConditionMethods.class)) {
			var testClass = testClassLoader.loadClass(getClass().getName());
			assertThat(testClass.getClassLoader()).isSameAs(testClassLoader);

			ExtensionContext context = mock();
			Method annotatedMethod = ReflectionSupport.findMethod(getClass(), "enabledMethod").get();
			when(context.getElement()).thenReturn(Optional.of(annotatedMethod));
			doReturn(testClass).when(context).getRequiredTestClass();

			DisabledIfCondition condition = new DisabledIfCondition();
			ConditionEvaluationResult result = condition.evaluateExecutionCondition(context);
			assertThat(result).isNotNull();
			assertThat(result.isDisabled()).isFalse();

			Method conditionMethod = condition.getConditionMethod(
				"org.junit.jupiter.api.condition.StaticConditionMethods#returnsFalse", context);
			assertThat(conditionMethod).isNotNull();
			Class<?> declaringClass = conditionMethod.getDeclaringClass();
			assertThat(declaringClass.getClassLoader()).isSameAs(testClassLoader);
			assertThat(declaringClass.getName()).isEqualTo(StaticConditionMethods.class.getName());
			assertThat(declaringClass).isNotEqualTo(StaticConditionMethods.class);
		}
	}

	@DisabledIf("org.junit.jupiter.api.condition.StaticConditionMethods#returnsFalse")
	private void enabledMethod() {
	}

}
