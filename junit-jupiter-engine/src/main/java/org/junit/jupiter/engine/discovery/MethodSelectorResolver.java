/*
 * Copyright 2015-2025 the original author or authors.
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v2.0 which
 * accompanies this distribution and is available at
 *
 * https://www.eclipse.org/legal/epl-v20.html
 */

package org.junit.jupiter.engine.discovery;

import static java.util.Collections.emptyList;
import static java.util.Collections.emptySet;
import static java.util.stream.Collectors.toSet;
import static org.junit.platform.engine.discovery.DiscoverySelectors.selectUniqueId;
import static org.junit.platform.engine.support.discovery.SelectorResolver.Resolution.matches;
import static org.junit.platform.engine.support.discovery.SelectorResolver.Resolution.unresolved;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.BiFunction;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.stream.Stream;

import org.junit.jupiter.engine.config.JupiterConfiguration;
import org.junit.jupiter.engine.descriptor.Filterable;
import org.junit.jupiter.engine.descriptor.TestClassAware;
import org.junit.jupiter.engine.descriptor.TestFactoryTestDescriptor;
import org.junit.jupiter.engine.descriptor.TestMethodTestDescriptor;
import org.junit.jupiter.engine.descriptor.TestTemplateInvocationTestDescriptor;
import org.junit.jupiter.engine.descriptor.TestTemplateTestDescriptor;
import org.junit.jupiter.engine.discovery.predicates.IsTestFactoryMethod;
import org.junit.jupiter.engine.discovery.predicates.IsTestMethod;
import org.junit.jupiter.engine.discovery.predicates.IsTestTemplateMethod;
import org.junit.jupiter.engine.discovery.predicates.TestClassPredicates;
import org.junit.platform.engine.DiscoveryIssue;
import org.junit.platform.engine.DiscoveryIssue.Severity;
import org.junit.platform.engine.DiscoverySelector;
import org.junit.platform.engine.TestDescriptor;
import org.junit.platform.engine.UniqueId;
import org.junit.platform.engine.discovery.DiscoverySelectors;
import org.junit.platform.engine.discovery.IterationSelector;
import org.junit.platform.engine.discovery.MethodSelector;
import org.junit.platform.engine.discovery.NestedMethodSelector;
import org.junit.platform.engine.discovery.UniqueIdSelector;
import org.junit.platform.engine.support.descriptor.MethodSource;
import org.junit.platform.engine.support.discovery.DiscoveryIssueReporter;
import org.junit.platform.engine.support.discovery.SelectorResolver;

/**
 * @since 5.5
 */
class MethodSelectorResolver implements SelectorResolver {

	private static final MethodSegmentResolver methodSegmentResolver = new MethodSegmentResolver();
	private final Predicate<Class<?>> testClassPredicate;

	private final JupiterConfiguration configuration;
	private final DiscoveryIssueReporter issueReporter;
	private final List<MethodType> methodTypes;

	MethodSelectorResolver(JupiterConfiguration configuration, DiscoveryIssueReporter issueReporter) {
		this.configuration = configuration;
		this.issueReporter = issueReporter;
		this.methodTypes = MethodType.allPossibilities(issueReporter);
		this.testClassPredicate = new TestClassPredicates(issueReporter).looksLikeNestedOrStandaloneTestClass;
	}

	@Override
	public Resolution resolve(MethodSelector selector, Context context) {
		return resolve(context, emptyList(), selector.getJavaClass(), selector::getJavaMethod, Match::exact);
	}

	@Override
	public Resolution resolve(NestedMethodSelector selector, Context context) {
		return resolve(context, selector.getEnclosingClasses(), selector.getNestedClass(), selector::getMethod,
			Match::exact);
	}

	@Override
	public Resolution resolve(DiscoverySelector selector, Context context) {
		if (selector instanceof DeclaredMethodSelector methodSelector) {
			var testClasses = methodSelector.testClasses();
			if (testClasses.size() == 1) {
				return resolve(context, emptyList(), testClasses.get(0), methodSelector::method, Match::exact);
			}
			int lastIndex = testClasses.size() - 1;
			return resolve(context, testClasses.subList(0, lastIndex), testClasses.get(lastIndex),
				methodSelector::method, Match::exact);
		}
		return unresolved();
	}

	private Resolution resolve(Context context, List<Class<?>> enclosingClasses, Class<?> testClass,
			Supplier<Method> methodSupplier,
			BiFunction<TestDescriptor, Supplier<Set<? extends DiscoverySelector>>, Match> matchFactory) {
		if (!testClassPredicate.test(testClass)) {
			return unresolved();
		}
		Method method = methodSupplier.get();
		// @formatter:off
		Set<Match> matches = methodTypes.stream()
				.map(methodType -> methodType.resolve(enclosingClasses, testClass, method, context, configuration))
				.flatMap(Optional::stream)
				.map(testDescriptor -> matchFactory.apply(testDescriptor, expansionCallback(testDescriptor)))
				.collect(toSet());
		// @formatter:on
		if (matches.size() > 1) {
			Stream<TestDescriptor> testDescriptors = matches.stream().map(Match::getTestDescriptor);
			String message = """
					Possible configuration error: method [%s] resulted in multiple TestDescriptors %s. \
					This is typically the result of annotating a method with multiple competing annotations \
					such as @Test, @RepeatedTest, @ParameterizedTest, @TestFactory, etc.""".formatted(
				method.toGenericString(), testDescriptors.map(d -> d.getClass().getName()).toList());
			issueReporter.reportIssue(
				DiscoveryIssue.builder(Severity.WARNING, message).source(MethodSource.from(method)));
		}
		return matches.isEmpty() ? unresolved() : matches(matches);
	}

	@Override
	public Resolution resolve(UniqueIdSelector selector, Context context) {
		UniqueId uniqueId = selector.getUniqueId();
		// @formatter:off
		return methodTypes.stream()
				.map(methodType -> methodType.resolveUniqueIdIntoTestDescriptor(uniqueId, context, configuration))
				.flatMap(Optional::stream)
				.map(testDescriptor -> {
					boolean exactMatch = uniqueId.equals(testDescriptor.getUniqueId());
					if (testDescriptor instanceof Filterable filterable) {
						if (exactMatch) {
							filterable.getDynamicDescendantFilter().allowAll();
						}
						else {
							filterable.getDynamicDescendantFilter().allowUniqueIdPrefix(uniqueId);
						}
					}
					return Resolution.match(exactMatch ? Match.exact(testDescriptor) : Match.partial(testDescriptor, expansionCallback(testDescriptor)));
				})
				.findFirst()
				.orElse(unresolved());
		// @formatter:on
	}

	@Override
	public Resolution resolve(IterationSelector selector, Context context) {
		if (selector.getParentSelector() instanceof MethodSelector methodSelector) {
			return resolve(context, emptyList(), methodSelector.getJavaClass(), methodSelector::getJavaMethod,
				(testDescriptor, childSelectorsSupplier) -> {
					if (testDescriptor instanceof Filterable filterable) {
						filterable.getDynamicDescendantFilter().allowIndex(selector.getIterationIndices());
					}
					return Match.partial(testDescriptor, childSelectorsSupplier);
				});
		}
		return unresolved();
	}

	private Supplier<Set<? extends DiscoverySelector>> expansionCallback(TestDescriptor testDescriptor) {
		return () -> {
			if (testDescriptor instanceof Filterable filterable) {
				filterable.getDynamicDescendantFilter().allowAll();
			}
			return emptySet();
		};
	}

	private static class MethodType {

		static List<MethodType> allPossibilities(DiscoveryIssueReporter issueReporter) {
			return Arrays.asList( //
				new MethodType(new IsTestMethod(issueReporter), TestMethodTestDescriptor::new,
					TestMethodTestDescriptor.SEGMENT_TYPE), //
				new MethodType(new IsTestFactoryMethod(issueReporter), TestFactoryTestDescriptor::new,
					TestFactoryTestDescriptor.SEGMENT_TYPE, TestFactoryTestDescriptor.DYNAMIC_CONTAINER_SEGMENT_TYPE,
					TestFactoryTestDescriptor.DYNAMIC_TEST_SEGMENT_TYPE), //
				new MethodType(new IsTestTemplateMethod(issueReporter), TestTemplateTestDescriptor::new,
					TestTemplateTestDescriptor.SEGMENT_TYPE, TestTemplateInvocationTestDescriptor.SEGMENT_TYPE) //
			);
		}

		private final Predicate<Method> methodPredicate;
		private final TestDescriptorFactory testDescriptorFactory;
		private final String segmentType;
		private final Set<String> dynamicDescendantSegmentTypes;

		private MethodType(Predicate<Method> methodPredicate, TestDescriptorFactory testDescriptorFactory,
				String segmentType, String... dynamicDescendantSegmentTypes) {
			this.methodPredicate = methodPredicate;
			this.testDescriptorFactory = testDescriptorFactory;
			this.segmentType = segmentType;
			this.dynamicDescendantSegmentTypes = new LinkedHashSet<>(Arrays.asList(dynamicDescendantSegmentTypes));
		}

		Optional<TestDescriptor> resolve(List<Class<?>> enclosingClasses, Class<?> testClass, Method method,
				Context context, JupiterConfiguration configuration) {
			if (!methodPredicate.test(method)) {
				return Optional.empty();
			}
			return context.addToParent(() -> selectClass(enclosingClasses, testClass), //
				parent -> Optional.of(createTestDescriptor(parent, testClass, method, configuration)));
		}

		private DiscoverySelector selectClass(List<Class<?>> enclosingClasses, Class<?> testClass) {
			if (enclosingClasses.isEmpty()) {
				return DiscoverySelectors.selectClass(testClass);
			}
			return DiscoverySelectors.selectNestedClass(enclosingClasses, testClass);
		}

		Optional<TestDescriptor> resolveUniqueIdIntoTestDescriptor(UniqueId uniqueId, Context context,
				JupiterConfiguration configuration) {
			UniqueId.Segment lastSegment = uniqueId.getLastSegment();
			if (segmentType.equals(lastSegment.getType())) {
				return context.addToParent(() -> selectUniqueId(uniqueId.removeLastSegment()), parent -> {
					String methodSpecPart = lastSegment.getValue();
					Class<?> testClass = ((TestClassAware) parent).getTestClass();
					// @formatter:off
					return methodSegmentResolver.findMethod(methodSpecPart, testClass)
							.filter(methodPredicate)
							.map(method -> createTestDescriptor(parent, testClass, method, configuration));
					// @formatter:on
				});
			}
			if (dynamicDescendantSegmentTypes.contains(lastSegment.getType())) {
				return resolveUniqueIdIntoTestDescriptor(uniqueId.removeLastSegment(), context, configuration);
			}
			return Optional.empty();
		}

		private TestDescriptor createTestDescriptor(TestDescriptor parent, Class<?> testClass, Method method,
				JupiterConfiguration configuration) {
			UniqueId uniqueId = createUniqueId(method, parent, testClass);
			return testDescriptorFactory.create(uniqueId, testClass, method,
				((TestClassAware) parent)::getEnclosingTestClasses, configuration);
		}

		private UniqueId createUniqueId(Method method, TestDescriptor parent, Class<?> testClass) {
			return parent.getUniqueId().append(segmentType,
				methodSegmentResolver.formatMethodSpecPart(method, testClass));
		}

		interface TestDescriptorFactory {
			TestDescriptor create(UniqueId uniqueId, Class<?> testClass, Method method,
					Supplier<List<Class<?>>> enclosingInstanceTypes, JupiterConfiguration configuration);
		}

	}

}
