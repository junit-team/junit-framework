/*
 * Copyright 2015-2026 the original author or authors.
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v2.0 which
 * accompanies this distribution and is available at
 *
 * https://www.eclipse.org/legal/epl-v20.html
 */

package org.junit.vintage.engine.samples.junit4;

import org.junit.runner.Description;
import org.junit.runner.RunWith;
import org.junit.runner.notification.RunNotifier;

@RunWith(JUnit4TestCaseWithRunnerWithDuplicateChangingChildDescriptions.Runner.class)
public class JUnit4TestCaseWithRunnerWithDuplicateChangingChildDescriptions {
	public static class Runner extends org.junit.runner.Runner {

		private final Class<?> testClass;

		public Runner(Class<?> testClass) {
			this.testClass = testClass;
		}

		@Override
		public Description getDescription() {
			var suiteDescription = Description.createSuiteDescription(testClass);
			suiteDescription.addChild(getContainerDescription("1st"));
			suiteDescription.addChild(getContainerDescription("2nd"));
			return suiteDescription;
		}

		private Description getContainerDescription(String name) {
			var parent = Description.createSuiteDescription(name);
			parent.addChild(getLeafDescription());
			parent.addChild(getLeafDescription());
			return parent;
		}

		private Description getLeafDescription() {
			return Description.createTestDescription(testClass, "leaf");
		}

		@Override
		public void run(RunNotifier notifier) {
			for (var i = 0; i < 2; i++) {
				notifier.fireTestIgnored(getLeafDescription());
				notifier.fireTestStarted(getLeafDescription());
				notifier.fireTestFinished(getLeafDescription());
			}
		}
	}
}
