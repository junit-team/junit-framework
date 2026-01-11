/*
 * Copyright 2015-2026 the original author or authors.
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v2.0 which
 * accompanies this distribution and is available at
 *
 * https://www.eclipse.org/legal/epl-v20.html
 */

package org.junit.platform.engine.support.hierarchical;

import static org.junit.jupiter.api.parallel.ExecutionMode.CONCURRENT;
import static org.junit.jupiter.api.parallel.ExecutionMode.SAME_THREAD;
import static org.junit.jupiter.api.parallel.ResourceAccessMode.READ;
import static org.junit.jupiter.api.parallel.ResourceAccessMode.READ_WRITE;
import static org.junit.jupiter.api.parallel.Resources.SYSTEM_PROPERTIES;
import static org.junit.platform.engine.discovery.DiscoverySelectors.selectClasses;

import java.time.LocalTime;
import java.util.concurrent.CountDownLatch;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.api.extension.AfterAllCallback;
import org.junit.jupiter.api.extension.AfterTestExecutionCallback;
import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.BeforeTestExecutionCallback;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.Isolated;
import org.junit.jupiter.api.parallel.ResourceLock;
import org.junit.jupiter.engine.Constants;
import org.junit.jupiter.params.ParameterizedClass;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.platform.engine.support.hierarchical.ParallelHierarchicalTestExecutorServiceFactory.ParallelExecutorServiceType;
import org.junit.platform.testkit.engine.EngineTestKit;

// https://github.com/junit-team/junit-framework/issues/3945
@Timeout(10)
@ParameterizedClass
@EnumSource(ParallelExecutorServiceType.class)
record ForkJoinDeadLockTests(ParallelExecutorServiceType executorServiceType) {

	@Test
	void forkJoinExecutionDoesNotLeadToDeadLock() {
		run(NonIsolatedTestCase.class, IsolatedTestCase.class, Isolated2TestCase.class);
	}

	@Test
	void nestedResourceLocksShouldStillWork() {
		run(SharedResourceTestCase.class);
	}

	@Test
	void multiLevelLocks() {
		run(ClassLevelTestCase.class);
	}

	private void run(Class<?>... classes) {
		EngineTestKit.engine("junit-jupiter") //
				.selectors(selectClasses(classes)) //
				.configurationParameter(Constants.PARALLEL_EXECUTION_ENABLED_PROPERTY_NAME, "true") //
				.configurationParameter(Constants.PARALLEL_CONFIG_EXECUTOR_SERVICE_PROPERTY_NAME,
					executorServiceType.name()) //
				.configurationParameter(Constants.DEFAULT_PARALLEL_EXECUTION_MODE, "concurrent") //
				.configurationParameter(Constants.DEFAULT_CLASSES_EXECUTION_MODE_PROPERTY_NAME, "concurrent") //
				.configurationParameter(Constants.PARALLEL_CONFIG_STRATEGY_PROPERTY_NAME, "fixed") //
				.configurationParameter(Constants.PARALLEL_CONFIG_FIXED_MAX_POOL_SIZE_PROPERTY_NAME, "3") //
				.configurationParameter(Constants.PARALLEL_CONFIG_FIXED_PARALLELISM_PROPERTY_NAME, "3") //
				.configurationParameter(Constants.PARALLEL_CONFIG_FIXED_SATURATE_PROPERTY_NAME, "false") //
				.execute();
	}

	@ExtendWith(StartFinishLogger.class)
	static class BaseTestCase {
	}

	@Execution(CONCURRENT)
	public static class NonIsolatedTestCase extends BaseTestCase {

		public static CountDownLatch otherThreadRunning = new CountDownLatch(1);
		public static CountDownLatch sameThreadFinishing = new CountDownLatch(1);

		@Test
		@Execution(CONCURRENT)
		void otherThread() throws Exception {
			otherThreadRunning.countDown();
			sameThreadFinishing.await();
			Thread.sleep(100);
		}

		@Test
		@Execution(SAME_THREAD)
		void sameThread() throws Exception {
			otherThreadRunning.await();
			sameThreadFinishing.countDown();
		}
	}

	@Isolated
	public static class IsolatedTestCase extends BaseTestCase {

		@Test
		void test() throws Exception {
			Thread.sleep(100);
		}
	}

	static class Isolated2TestCase extends IsolatedTestCase {
	}

	public static class SharedResourceTestCase {

		@Test
		@ResourceLock(value = SYSTEM_PROPERTIES, mode = READ)
		void customPropertyIsNotSetByDefault() {
		}

		@Test
		@ResourceLock(value = SYSTEM_PROPERTIES, mode = READ_WRITE)
		void canSetCustomPropertyToApple() {
		}

		@Test
		@ResourceLock(value = SYSTEM_PROPERTIES, mode = READ_WRITE)
		void canSetCustomPropertyToBanana() {
		}
	}

	@ResourceLock(value = "foo", mode = READ_WRITE)
	public static class ClassLevelTestCase {

		@Test
		@ResourceLock(value = SYSTEM_PROPERTIES, mode = READ)
		void customPropertyIsNotSetByDefault() {
		}

		@Test
		@ResourceLock(value = SYSTEM_PROPERTIES, mode = READ_WRITE)
		void canSetCustomPropertyToApple() {
		}

		@Test
		@ResourceLock(value = SYSTEM_PROPERTIES, mode = READ_WRITE)
		void canSetCustomPropertyToBanana() {
		}
	}

	static class StartFinishLogger
			implements BeforeTestExecutionCallback, AfterTestExecutionCallback, BeforeAllCallback, AfterAllCallback {

		@Override
		public void beforeAll(ExtensionContext context) {
			log("starting class " + context.getTestClass().orElseThrow().getSimpleName());
		}

		@Override
		public void beforeTestExecution(ExtensionContext context) {
			log("starting method " + context.getTestClass().orElseThrow().getSimpleName() + "."
					+ context.getTestMethod().orElseThrow().getName());
		}

		@Override
		public void afterTestExecution(ExtensionContext context) {
			log("finishing method " + context.getTestClass().orElseThrow().getSimpleName() + "."
					+ context.getTestMethod().orElseThrow().getName());
		}

		@Override
		public void afterAll(ExtensionContext context) {
			log("finishing class " + context.getTestClass().orElseThrow().getSimpleName());
		}
	}

	private static void log(String message) {
		System.out.println("[" + LocalTime.now() + "] " + Thread.currentThread().getName() + " - " + message);
	}
}
