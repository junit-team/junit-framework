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

import static org.junit.platform.engine.discovery.DiscoverySelectors.selectClass;
import static org.junit.platform.engine.discovery.DiscoverySelectors.selectUniqueId;
import static org.junit.platform.engine.support.discovery.SelectorResolver.Resolution.unresolved;
import static org.junit.vintage.engine.descriptor.VintageTestDescriptor.SEGMENT_TYPE_RUNNER;

import java.util.Optional;
import java.util.function.Function;

import org.junit.platform.engine.DiscoverySelector;
import org.junit.platform.engine.TestDescriptor;
import org.junit.platform.engine.UniqueId;
import org.junit.platform.engine.discovery.MethodSelector;
import org.junit.platform.engine.discovery.UniqueIdSelector;
import org.junit.platform.engine.support.discovery.SelectorResolver;
import org.junit.runner.Description;
import org.junit.runner.manipulation.Filter;
import org.junit.vintage.engine.descriptor.DescriptionUtils;
import org.junit.vintage.engine.descriptor.RunnerTestDescriptor;

/**
 * @since 4.12
 */
class MethodSelectorResolver implements SelectorResolver {

	@Override
	public Resolution resolve(MethodSelector selector, Context context) {
		Class<?> testClass = selector.getJavaClass();
		return resolveParentAndAddFilter(context, selectClass(testClass), parent -> toMethodFilter(selector));
	}

	@Override
	public Resolution resolve(UniqueIdSelector selector, Context context) {
		for (UniqueId current = selector.getUniqueId(); !current.getSegments().isEmpty(); current = current.removeLastSegment()) {
			if (SEGMENT_TYPE_RUNNER.equals(current.getLastSegment().getType())) {
				return resolveParentAndAddFilter(context, selectUniqueId(current),
					parent -> toUniqueIdFilter(parent, selector.getUniqueId()));
			}
		}
		return unresolved();
	}

	private Resolution resolveParentAndAddFilter(Context context, DiscoverySelector selector,
			Function<RunnerTestDescriptor, Filter> filterCreator) {
		return context.resolve(selector).flatMap(parent -> addFilter(parent, filterCreator)).map(
			this::toResolution).orElse(unresolved());
	}

	private Optional<RunnerTestDescriptor> addFilter(TestDescriptor parent,
			Function<RunnerTestDescriptor, Filter> filterCreator) {
		if (parent instanceof RunnerTestDescriptor runnerTestDescriptor) {
			runnerTestDescriptor.getFilters().ifPresent(
				filters -> filters.add(filterCreator.apply(runnerTestDescriptor)));
			return Optional.of(runnerTestDescriptor);
		}
		return Optional.empty();
	}

	private Resolution toResolution(RunnerTestDescriptor parent) {
		return Resolution.match(Match.partial(parent));
	}

	private Filter toMethodFilter(MethodSelector methodSelector) {
		Class<?> testClass = methodSelector.getJavaClass();
		String methodName = methodSelector.getMethodName();
		return matchMethodDescription(Description.createTestDescription(testClass, methodName));
	}

	private Filter toUniqueIdFilter(RunnerTestDescriptor runnerTestDescriptor, UniqueId uniqueId) {
		return new UniqueIdFilter(runnerTestDescriptor, uniqueId);
	}

	/**
	 * The method {@link Filter#matchMethodDescription(Description)} returns a
	 * filter that does not account for the case when the description is for a
	 * {@link org.junit.runners.Parameterized} runner.
	 */
	private static Filter matchMethodDescription(final Description desiredDescription) {
		String desiredMethodName = DescriptionUtils.getMethodName(desiredDescription);
		return new Filter() {

			@Override
			public boolean shouldRun(Description description) {
				if (description.isTest()) {
					return desiredDescription.equals(description) || isParameterizedMethod(description);
				}

				// explicitly check if any children want to run
				for (Description each : description.getChildren()) {
					if (shouldRun(each)) {
						return true;
					}
				}
				return false;
			}

			private boolean isParameterizedMethod(Description description) {
				String methodName = DescriptionUtils.getMethodName(description);
				return methodName != null && methodName.startsWith(desiredMethodName + "[");
			}

			@Override
			public String describe() {
				return "Method %s".formatted(desiredDescription.getDisplayName());
			}
		};
	}

}
