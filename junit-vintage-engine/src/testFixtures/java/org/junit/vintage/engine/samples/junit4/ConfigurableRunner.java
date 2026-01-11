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

import static java.util.stream.IntStream.range;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.junit.runner.Description;
import org.junit.runner.Runner;
import org.junit.runner.notification.RunNotifier;

/**
 * @since 5.1
 */
abstract class ConfigurableRunner extends Runner {

	@Retention(RetentionPolicy.RUNTIME)
	@Target(ElementType.TYPE)
	public @interface ChildCount {

		int value();

	}

	protected final Class<?> testClass;
	protected final List<Description> filteredChildren = new ArrayList<>();

	ConfigurableRunner(Class<?> testClass) {
		this.testClass = testClass;
		var childCountAnnotation = testClass.getAnnotation(ChildCount.class);
		int childCount = Optional.ofNullable(childCountAnnotation).map(ChildCount::value).orElse(0);
		// @formatter:off
		range(0, childCount)
				.mapToObj(index -> Description.createTestDescription(testClass, "Test #" + index))
				.forEach(filteredChildren::add);
		// @formatter:on
	}

	@Override
	public Description getDescription() {
		var suiteDescription = Description.createSuiteDescription(testClass);
		filteredChildren.forEach(suiteDescription::addChild);
		return suiteDescription;
	}

	@Override
	public void run(RunNotifier notifier) {
		filteredChildren.forEach(child -> {
			notifier.fireTestStarted(child);
			notifier.fireTestFinished(child);
		});
	}

}
