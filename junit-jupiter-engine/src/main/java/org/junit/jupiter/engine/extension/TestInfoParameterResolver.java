/*
 * Copyright 2015-2026 the original author or authors.
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v2.0 which
 * accompanies this distribution and is available at
 *
 * https://www.eclipse.org/legal/epl-v20.html
 */

package org.junit.jupiter.engine.extension;

import java.lang.reflect.Method;
import java.util.Optional;
import java.util.Set;

import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.TestInfo;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.ParameterContext;
import org.junit.jupiter.api.extension.ParameterResolver;
import org.junit.platform.commons.util.ToStringBuilder;

/**
 * {@link ParameterResolver} that resolves the {@link TestInfo} for
 * the currently executing test.
 *
 * @since 5.0
 */
class TestInfoParameterResolver implements ParameterResolver {

	@Override
	public ExtensionContextScope getTestInstantiationExtensionContextScope(ExtensionContext rootContext) {
		return ExtensionContextScope.TEST_METHOD;
	}

	@Override
	public boolean supportsParameter(ParameterContext parameterContext, ExtensionContext extensionContext) {
		return (parameterContext.getParameter().getType() == TestInfo.class);
	}

	@Override
	public TestInfo resolveParameter(ParameterContext parameterContext, ExtensionContext extensionContext) {
		return new DefaultTestInfo(extensionContext);
	}

	private static class DefaultTestInfo implements TestInfo {

		private final String displayName;
		private final Set<String> tags;
		private final Optional<Class<?>> testClass;
		private final Optional<Method> testMethod;

		DefaultTestInfo(ExtensionContext extensionContext) {
			this.displayName = extensionContext.getDisplayName();
			this.tags = extensionContext.getTags();
			this.testClass = extensionContext.getTestClass();
			this.testMethod = extensionContext.getTestMethod();
		}

		@Override
		public String getDisplayName() {
			return this.displayName;
		}

		@Override
		public Set<String> getTags() {
			return this.tags;
		}

		@Override
		public Optional<Class<?>> getTestClass() {
			return this.testClass;
		}

		@Override
		public Optional<Method> getTestMethod() {
			return this.testMethod;
		}

		@Override
		public String toString() {
			// @formatter:off
			return new ToStringBuilder(this)
					.append("displayName", this.displayName)
					.append("tags", this.tags)
					.append("testClass", nullSafeGet(this.testClass))
					.append("testMethod", nullSafeGet(this.testMethod))
					.toString();
			// @formatter:on
		}

		@SuppressWarnings({ "OptionalAssignedToNull", "NullableOptional" })
		private static @Nullable Object nullSafeGet(@Nullable Optional<?> optional) {
			return optional != null ? optional.orElse(null) : null;
		}

	}

}
