/*
 * Copyright 2015-2026 the original author or authors.
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v2.0 which
 * accompanies this distribution and is available at
 *
 * https://www.eclipse.org/legal/epl-v20.html
 */

package org.junit.jupiter.params.support;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.jspecify.annotations.NullMarked;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.BeforeClassTemplateInvocationCallback;
import org.junit.jupiter.api.extension.BeforeEachCallback;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.engine.AbstractJupiterTestEngineTests;
import org.junit.jupiter.params.Parameter;
import org.junit.jupiter.params.ParameterizedClass;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * @since 5.13
 */
class DeprecatedParameterInfoIntegrationTests extends AbstractJupiterTestEngineTests {

	@Test
	void storesParameterInfoInExtensionContextStoreOnDifferentLevels() {
		var results = executeTestsForClass(TestCase.class);

		results.allEvents().debug().assertStatistics(stats -> stats.started(7).succeeded(7));
	}

	@ParameterizedClass
	@ValueSource(ints = 1)
	@ExtendWith(ParameterInfoConsumingExtension.class)
	record TestCase(int i) {

		@Nested
		@ParameterizedClass
		@ValueSource(ints = 2)
		class Inner {

			@Parameter
			int j;

			@ParameterizedTest
			@ValueSource(ints = 3)
			void test(int k) {
				assertEquals(1, i);
				assertEquals(2, j);
				assertEquals(3, k);
			}
		}
	}

	@SuppressWarnings("removal")
	@NullMarked
	private static class ParameterInfoConsumingExtension
			implements BeforeClassTemplateInvocationCallback, BeforeEachCallback {

		@Override
		public void beforeClassTemplateInvocation(ExtensionContext parameterizedClassInvocationContext) {
			if (TestCase.Inner.class.equals(parameterizedClassInvocationContext.getRequiredTestClass())) {
				assertParameterInfo(parameterizedClassInvocationContext, "j", 2);

				var nestedParameterizedClassContext = parameterizedClassInvocationContext.getParent().orElseThrow();
				assertParameterInfo(nestedParameterizedClassContext, "i", 1);

				parameterizedClassInvocationContext = nestedParameterizedClassContext.getParent().orElseThrow();
			}

			assertParameterInfo(parameterizedClassInvocationContext, "i", 1);

			var outerParameterizedClassContext = parameterizedClassInvocationContext.getParent().orElseThrow();
			assertNull(ParameterInfo.get(outerParameterizedClassContext));
		}

		@Override
		public void beforeEach(ExtensionContext parameterizedTestInvocationContext) {
			assertParameterInfo(parameterizedTestInvocationContext, "k", 3);

			var parameterizedTestContext = parameterizedTestInvocationContext.getParent().orElseThrow();
			assertParameterInfo(parameterizedTestContext, "j", 2);

			var nestedParameterizedClassInvocationContext = parameterizedTestContext.getParent().orElseThrow();
			assertParameterInfo(nestedParameterizedClassInvocationContext, "j", 2);

			var nestedParameterizedClassContext = nestedParameterizedClassInvocationContext.getParent().orElseThrow();
			assertParameterInfo(nestedParameterizedClassContext, "i", 1);

			var outerParameterizedClassInvocationContext = nestedParameterizedClassContext.getParent().orElseThrow();
			assertParameterInfo(outerParameterizedClassInvocationContext, "i", 1);

			var outerParameterizedClassContext = outerParameterizedClassInvocationContext.getParent().orElseThrow();
			assertNull(ParameterInfo.get(outerParameterizedClassContext));
		}

		private static void assertParameterInfo(ExtensionContext context, String parameterName, int argumentValue) {
			var parameterInfo = ParameterInfo.get(context);
			assertNotNull(parameterInfo);
			var declaration = parameterInfo.getDeclarations().get(0).orElseThrow();
			assertEquals(parameterName, declaration.getParameterName().orElseThrow());
			assertEquals(int.class, declaration.getParameterType());
			assertEquals(argumentValue, parameterInfo.getArguments().getInteger(0));
		}
	}
}
