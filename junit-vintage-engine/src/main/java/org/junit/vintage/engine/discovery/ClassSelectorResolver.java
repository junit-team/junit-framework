/*
 * Copyright 2015-2026 the original author or authors.
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v2.0 which
 * accompanies this distribution and is available at
 *
 * https://www.eclipse.org/legal/epl-v20.html
 */

package org.junit.vintage.engine.discovery;

import static java.util.Collections.emptySet;
import static org.junit.platform.engine.support.discovery.SelectorResolver.Resolution.unresolved;
import static org.junit.vintage.engine.descriptor.VintageTestDescriptor.SEGMENT_TYPE_RUNNER;

import java.util.Optional;

import org.junit.platform.commons.JUnitException;
import org.junit.platform.commons.support.ReflectionSupport;
import org.junit.platform.commons.support.scanning.ClassFilter;
import org.junit.platform.engine.TestDescriptor;
import org.junit.platform.engine.UniqueId;
import org.junit.platform.engine.UniqueId.Segment;
import org.junit.platform.engine.discovery.ClassSelector;
import org.junit.platform.engine.discovery.UniqueIdSelector;
import org.junit.platform.engine.support.discovery.SelectorResolver;
import org.junit.runner.Runner;
import org.junit.vintage.engine.descriptor.RunnerTestDescriptor;

/**
 * @since 4.12
 */
class ClassSelectorResolver implements SelectorResolver {

	private static final DefensiveAllDefaultPossibilitiesBuilder RUNNER_BUILDER = new DefensiveAllDefaultPossibilitiesBuilder();

	private final ClassFilter classFilter;

	ClassSelectorResolver(ClassFilter classFilter) {
		this.classFilter = classFilter;
	}

	@Override
	public Resolution resolve(ClassSelector selector, Context context) {
		if (classFilter.match(selector.getClassName())) {
			return resolveTestClassThatPassedNameFilter(selector.getJavaClass(), context);
		}
		return unresolved();
	}

	@Override
	public Resolution resolve(UniqueIdSelector selector, Context context) {
		Segment lastSegment = selector.getUniqueId().getLastSegment();
		if (SEGMENT_TYPE_RUNNER.equals(lastSegment.getType())) {
			String testClassName = lastSegment.getValue();
			if (classFilter.match(testClassName)) {
				Class<?> testClass = ReflectionSupport.tryToLoadClass(testClassName)//
						.getNonNullOrThrow(cause -> new JUnitException("Unknown class: " + testClassName, cause));
				return resolveTestClassThatPassedNameFilter(testClass, context);
			}
		}
		return unresolved();
	}

	private Resolution resolveTestClassThatPassedNameFilter(Class<?> testClass, Context context) {
		if (!classFilter.match(testClass)) {
			return unresolved();
		}
		Runner runner = RUNNER_BUILDER.safeRunnerForClass(testClass);
		if (runner == null) {
			return unresolved();
		}
		return context.addToParent(parent -> Optional.of(createRunnerTestDescriptor(parent, testClass, runner))).map(
			runnerTestDescriptor -> Match.exact(runnerTestDescriptor, () -> {
				runnerTestDescriptor.clearFilters();
				return emptySet();
			})).map(Resolution::match).orElse(unresolved());
	}

	private RunnerTestDescriptor createRunnerTestDescriptor(TestDescriptor parent, Class<?> testClass, Runner runner) {
		UniqueId uniqueId = parent.getUniqueId().append(SEGMENT_TYPE_RUNNER, testClass.getName());
		return new RunnerTestDescriptor(uniqueId, testClass, runner, RUNNER_BUILDER.isIgnored(runner));
	}

}
