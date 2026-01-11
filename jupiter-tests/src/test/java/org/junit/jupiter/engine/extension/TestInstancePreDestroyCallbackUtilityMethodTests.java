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

import static org.junit.jupiter.api.TestInstance.Lifecycle.PER_CLASS;
import static org.junit.platform.testkit.engine.EventConditions.event;
import static org.junit.platform.testkit.engine.EventConditions.reportEntry;
import static org.junit.platform.testkit.engine.EventConditions.started;
import static org.junit.platform.testkit.engine.EventConditions.test;

import java.util.Map;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.TestInstancePostProcessor;
import org.junit.jupiter.api.extension.TestInstancePreDestroyCallback;
import org.junit.jupiter.engine.AbstractJupiterTestEngineTests;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

public class TestInstancePreDestroyCallbackUtilityMethodTests extends AbstractJupiterTestEngineTests {

	@ParameterizedTest(name = "{0}")
	@ValueSource(classes = { PerMethodLifecycleOnAllLevels.class, PerMethodWithinPerClassLifecycle.class,
			PerClassWithinPerMethodLifecycle.class, PerClassLifecycleOnAllLevels.class })
	void destroysWhatWasPostProcessed(Class<?> testClass) {
		executeTestsForClass(testClass).allEvents() //
				.assertStatistics(stats -> stats.reportingEntryPublished(4)) //
				.assertEventsMatchLooselyInOrder( //
					reportEntry(Map.of("post-process", testClass.getSimpleName())),
					reportEntry(Map.of("post-process", "Inner")), //
					event(test(), started()), //
					reportEntry(Map.of("pre-destroy", "Inner")), //
					reportEntry(Map.of("pre-destroy", testClass.getSimpleName())) //
				);
	}

	@ExtendWith(TestInstanceLifecycleExtension.class)
	static class PerMethodLifecycleOnAllLevels {
		@Nested
		class Inner {
			@Test
			void test() {
			}
		}
	}

	@ExtendWith(TestInstanceLifecycleExtension.class)
	@TestInstance(PER_CLASS)
	static class PerMethodWithinPerClassLifecycle {
		@Nested
		class Inner {
			@Test
			void test() {
			}
		}
	}

	@ExtendWith(TestInstanceLifecycleExtension.class)
	static class PerClassWithinPerMethodLifecycle {
		@Nested
		@TestInstance(PER_CLASS)
		class Inner {
			@Test
			void test() {
			}
		}
	}

	@ExtendWith(TestInstanceLifecycleExtension.class)
	@TestInstance(PER_CLASS)
	static class PerClassLifecycleOnAllLevels {
		@Nested
		@TestInstance(PER_CLASS)
		class Inner {
			@Test
			void test() {
			}
		}
	}

	private static class TestInstanceLifecycleExtension
			implements TestInstancePostProcessor, TestInstancePreDestroyCallback {

		@Override
		public void postProcessTestInstance(Object testInstance, ExtensionContext context) {
			context.publishReportEntry("post-process", testInstance.getClass().getSimpleName());
		}

		@Override
		public void preDestroyTestInstance(ExtensionContext context) {
			TestInstancePreDestroyCallback.preDestroyTestInstances(context,
				testInstance -> context.publishReportEntry("pre-destroy", testInstance.getClass().getSimpleName()));
		}
	}
}
