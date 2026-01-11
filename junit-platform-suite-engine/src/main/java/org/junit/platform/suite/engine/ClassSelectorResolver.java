/*
 * Copyright 2015-2026 the original author or authors.
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v2.0 which
 * accompanies this distribution and is available at
 *
 * https://www.eclipse.org/legal/epl-v20.html
 */

package org.junit.platform.suite.engine;

import static org.junit.platform.engine.support.discovery.SelectorResolver.Resolution.unresolved;

import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;

import org.junit.platform.commons.support.ReflectionSupport;
import org.junit.platform.engine.ConfigurationParameters;
import org.junit.platform.engine.DiscoveryIssue;
import org.junit.platform.engine.DiscoveryIssue.Severity;
import org.junit.platform.engine.EngineDiscoveryListener;
import org.junit.platform.engine.OutputDirectoryCreator;
import org.junit.platform.engine.TestDescriptor;
import org.junit.platform.engine.UniqueId;
import org.junit.platform.engine.UniqueId.Segment;
import org.junit.platform.engine.discovery.ClassSelector;
import org.junit.platform.engine.discovery.UniqueIdSelector;
import org.junit.platform.engine.support.descriptor.ClassSource;
import org.junit.platform.engine.support.discovery.DiscoveryIssueReporter;
import org.junit.platform.engine.support.discovery.SelectorResolver;

/**
 * @since 1.8
 */
final class ClassSelectorResolver implements SelectorResolver {

	private final IsSuiteClass isSuiteClass;
	private final Predicate<String> classNameFilter;
	private final SuiteEngineDescriptor suiteEngineDescriptor;
	private final ConfigurationParameters configurationParameters;
	private final OutputDirectoryCreator outputDirectoryCreator;
	private final EngineDiscoveryListener discoveryListener;
	private final DiscoveryIssueReporter issueReporter;

	ClassSelectorResolver(Predicate<String> classNameFilter, SuiteEngineDescriptor suiteEngineDescriptor,
			ConfigurationParameters configurationParameters, OutputDirectoryCreator outputDirectoryCreator,
			EngineDiscoveryListener discoveryListener, DiscoveryIssueReporter issueReporter) {

		this.isSuiteClass = new IsSuiteClass(issueReporter);
		this.classNameFilter = classNameFilter;
		this.suiteEngineDescriptor = suiteEngineDescriptor;
		this.configurationParameters = configurationParameters;
		this.outputDirectoryCreator = outputDirectoryCreator;
		this.discoveryListener = discoveryListener;
		this.issueReporter = issueReporter;
	}

	@Override
	public Resolution resolve(ClassSelector selector, Context context) {
		Class<?> testClass = selector.getJavaClass();
		if (isSuiteClass.test(testClass)) {
			if (classNameFilter.test(testClass.getName())) {
				// @formatter:off
				Optional<SuiteTestDescriptor> suiteWithDiscoveryRequest = context
						.addToParent(parent -> newSuiteDescriptor(testClass, parent))
						.map(suite -> suite.addDiscoveryRequestFrom(testClass));
				return toResolution(suiteWithDiscoveryRequest);
				// @formatter:on
			}
		}
		return unresolved();
	}

	@Override
	public Resolution resolve(UniqueIdSelector selector, Context context) {
		UniqueId uniqueId = selector.getUniqueId();
		UniqueId engineId = suiteEngineDescriptor.getUniqueId();
		List<Segment> resolvedSegments = engineId.getSegments();
		// @formatter:off
		return uniqueId.getSegments()
				.stream()
				.skip(resolvedSegments.size())
				.findFirst()
				.filter(suiteSegment -> SuiteTestDescriptor.SEGMENT_TYPE.equals(suiteSegment.getType()))
				.flatMap(ClassSelectorResolver::tryLoadSuiteClass)
				.filter(isSuiteClass)
				.map(suiteClass -> context
						.addToParent(parent -> newSuiteDescriptor(suiteClass, parent))
						.map(suite -> uniqueId.equals(suite.getUniqueId())
								// The uniqueId selector either targeted a class annotated with @Suite;
								? suite.addDiscoveryRequestFrom(suiteClass)
								// or a specific test in that suite
								: suite.addDiscoveryRequestFrom(uniqueId)))
				.map(ClassSelectorResolver::toResolution)
				.orElseGet(Resolution::unresolved);
		// @formatter:on
	}

	private static Optional<Class<?>> tryLoadSuiteClass(UniqueId.Segment segment) {
		return ReflectionSupport.tryToLoadClass(segment.getValue()).toOptional();
	}

	private static Resolution toResolution(Optional<SuiteTestDescriptor> suite) {
		return suite.map(Match::exact).map(Resolution::match).orElseGet(Resolution::unresolved);
	}

	private Optional<SuiteTestDescriptor> newSuiteDescriptor(Class<?> suiteClass, TestDescriptor parent) {
		UniqueId id = parent.getUniqueId().append(SuiteTestDescriptor.SEGMENT_TYPE, suiteClass.getName());
		if (containsCycle(id)) {
			issueReporter.reportIssue(
				DiscoveryIssue.builder(Severity.INFO, createConfigContainsCycleMessage(suiteClass, id)) //
						.source(ClassSource.from(suiteClass)));
			return Optional.empty();
		}

		return Optional.of(new SuiteTestDescriptor(id, suiteClass, configurationParameters, outputDirectoryCreator,
			discoveryListener, issueReporter));
	}

	private static boolean containsCycle(UniqueId id) {
		List<Segment> segments = id.getSegments();
		List<Segment> engineAndSuiteSegment = segments.subList(segments.size() - 2, segments.size());
		List<Segment> ancestorSegments = segments.subList(0, segments.size() - 2);
		for (int i = 0; i < ancestorSegments.size() - 1; i++) {
			List<Segment> candidate = ancestorSegments.subList(i, i + 2);
			if (engineAndSuiteSegment.equals(candidate)) {
				return true;
			}
		}
		return false;
	}

	private static String createConfigContainsCycleMessage(Class<?> suiteClass, UniqueId suiteId) {
		return "The suite configuration of [%s] resulted in a cycle [%s] and will not be discovered a second time.".formatted(
			suiteClass.getName(), suiteId);
	}

}
