/*
 * Copyright 2015-2026 the original author or authors.
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v2.0 which
 * accompanies this distribution and is available at
 *
 * https://www.eclipse.org/legal/epl-v20.html
 */

package org.junit.jupiter.engine.discovery;

import static java.util.Collections.emptyList;
import static java.util.function.Predicate.isEqual;
import static java.util.stream.Collectors.toCollection;
import static java.util.stream.Collectors.toSet;
import static org.junit.jupiter.engine.descriptor.NestedClassTestDescriptor.getEnclosingTestClasses;
import static org.junit.jupiter.engine.discovery.predicates.TestClassPredicates.NestedClassInvalidityReason.NOT_INNER;
import static org.junit.platform.commons.support.HierarchyTraversalMode.TOP_DOWN;
import static org.junit.platform.commons.support.ReflectionSupport.findMethods;
import static org.junit.platform.commons.util.FunctionUtils.where;
import static org.junit.platform.commons.util.ReflectionUtils.isInnerClass;
import static org.junit.platform.commons.util.ReflectionUtils.isNotAbstract;
import static org.junit.platform.commons.util.ReflectionUtils.streamNestedClasses;
import static org.junit.platform.engine.discovery.DiscoverySelectors.selectUniqueId;
import static org.junit.platform.engine.support.discovery.SelectorResolver.Resolution.unresolved;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.BiFunction;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.stream.Stream;

import org.junit.jupiter.api.extension.ClassTemplateInvocationContext;
import org.junit.jupiter.engine.config.JupiterConfiguration;
import org.junit.jupiter.engine.descriptor.ClassBasedTestDescriptor;
import org.junit.jupiter.engine.descriptor.ClassTemplateInvocationTestDescriptor;
import org.junit.jupiter.engine.descriptor.ClassTemplateTestDescriptor;
import org.junit.jupiter.engine.descriptor.ClassTestDescriptor;
import org.junit.jupiter.engine.descriptor.Filterable;
import org.junit.jupiter.engine.descriptor.NestedClassTestDescriptor;
import org.junit.jupiter.engine.descriptor.TestClassAware;
import org.junit.jupiter.engine.discovery.predicates.TestClassPredicates;
import org.junit.platform.commons.support.ReflectionSupport;
import org.junit.platform.commons.util.ReflectionUtils;
import org.junit.platform.commons.util.ReflectionUtils.CycleErrorHandling;
import org.junit.platform.engine.DiscoveryIssue;
import org.junit.platform.engine.DiscoveryIssue.Severity;
import org.junit.platform.engine.DiscoverySelector;
import org.junit.platform.engine.TestDescriptor;
import org.junit.platform.engine.UniqueId;
import org.junit.platform.engine.discovery.ClassSelector;
import org.junit.platform.engine.discovery.DiscoverySelectors;
import org.junit.platform.engine.discovery.IterationSelector;
import org.junit.platform.engine.discovery.NestedClassSelector;
import org.junit.platform.engine.discovery.UniqueIdSelector;
import org.junit.platform.engine.support.descriptor.ClassSource;
import org.junit.platform.engine.support.discovery.DiscoveryIssueReporter;
import org.junit.platform.engine.support.discovery.SelectorResolver;

/**
 * @since 5.5
 */
class ClassSelectorResolver implements SelectorResolver {

	private final Predicate<String> classNameFilter;
	private final JupiterConfiguration configuration;
	private final TestClassPredicates predicates;
	private final DiscoveryIssueReporter issueReporter;

	ClassSelectorResolver(Predicate<String> classNameFilter, JupiterConfiguration configuration,
			DiscoveryIssueReporter issueReporter) {
		this.classNameFilter = classNameFilter;
		this.configuration = configuration;
		this.predicates = new TestClassPredicates(issueReporter);
		this.issueReporter = issueReporter;
	}

	@Override
	public Resolution resolve(ClassSelector selector, Context context) {
		Class<?> testClass = selector.getJavaClass();

		if (this.predicates.isAnnotatedWithNested.test(testClass)) {
			// Class name filter is not applied to nested test classes
			var invalidityReason = this.predicates.validateNestedTestClass(testClass);
			if (invalidityReason == null) {
				return toResolution(
					context.addToParent(() -> DiscoverySelectors.selectClass(testClass.getEnclosingClass()),
						parent -> Optional.of(newMemberClassTestDescriptor(parent, testClass))));
			}
			if (invalidityReason == NOT_INNER) {
				return resolveStandaloneTestClass(context, testClass);
			}
			return unresolved();
		}
		return resolveStandaloneTestClass(context, testClass);
	}

	private Resolution resolveStandaloneTestClass(Context context, Class<?> testClass) {
		if (isAcceptedStandaloneTestClass(testClass)) {
			return toResolution(
				context.addToParent(parent -> Optional.of(newStandaloneClassTestDescriptor(parent, testClass))));
		}
		return unresolved();
	}

	private boolean isAcceptedStandaloneTestClass(Class<?> testClass) {
		return this.classNameFilter.test(testClass.getName()) //
				&& this.predicates.looksLikeIntendedTestClass(testClass) //
				&& this.predicates.isValidStandaloneTestClass(testClass);
	}

	@Override
	public Resolution resolve(NestedClassSelector selector, Context context) {
		Class<?> nestedClass = selector.getNestedClass();
		if (this.predicates.isAnnotatedWithNested.test(nestedClass)) {
			if (this.predicates.isValidNestedTestClass(nestedClass)) {
				return toResolution(context.addToParent(() -> selectClass(selector.getEnclosingClasses()),
					parent -> Optional.of(newMemberClassTestDescriptor(parent, nestedClass))));
			}
		}
		else if (isInnerClass(nestedClass) && isNotAbstract(nestedClass)
				&& predicates.looksLikeIntendedTestClass(nestedClass)) {
			String message = "Inner class '%s' looks like it was intended to be a test class but will not be executed. It must be static or annotated with @Nested.".formatted(
				nestedClass.getName());
			issueReporter.reportIssue(DiscoveryIssue.builder(Severity.WARNING, message) //
					.source(ClassSource.from(nestedClass)));
		}
		return unresolved();
	}

	@Override
	public Resolution resolve(UniqueIdSelector selector, Context context) {
		UniqueId uniqueId = selector.getUniqueId();
		UniqueId.Segment lastSegment = uniqueId.getLastSegment();
		return switch (lastSegment.getType()) {
			case ClassTestDescriptor.SEGMENT_TYPE -> //
					resolveStandaloneClassUniqueId(context, lastSegment, __ -> true, this::newClassTestDescriptor);
			case ClassTemplateTestDescriptor.STANDALONE_CLASS_SEGMENT_TYPE -> //
					resolveStandaloneClassUniqueId(context, lastSegment, this.predicates.isAnnotatedWithClassTemplate,
						this::newClassTemplateTestDescriptor);
			case NestedClassTestDescriptor.SEGMENT_TYPE -> //
					resolveNestedClassUniqueId(context, uniqueId, __ -> true, this::newNestedClassTestDescriptor);
			case ClassTemplateTestDescriptor.NESTED_CLASS_SEGMENT_TYPE -> //
					resolveNestedClassUniqueId(context, uniqueId, this.predicates.isAnnotatedWithClassTemplate,
						this::newNestedClassTemplateTestDescriptor);
			case ClassTemplateInvocationTestDescriptor.SEGMENT_TYPE -> {
				Optional<ClassTemplateInvocationTestDescriptor> testDescriptor = context.addToParent(
					() -> selectUniqueId(uniqueId.removeLastSegment()), parent -> {
						int index = Integer.parseInt(lastSegment.getValue().substring(1));
						return Optional.of(newDummyClassTemplateInvocationTestDescriptor(parent, index));
					});
				yield toInvocationMatch(testDescriptor) //
						.map(Resolution::match) //
						.orElse(unresolved());
			}
			default -> unresolved();
		};
	}

	@Override
	public Resolution resolve(IterationSelector selector, Context context) {
		DiscoverySelector parentSelector = selector.getParentSelector();
		if (parentSelector instanceof ClassSelector classSelector
				&& this.predicates.isAnnotatedWithClassTemplate.test(classSelector.getJavaClass())) {
			return resolveIterations(selector, context);
		}
		if (parentSelector instanceof NestedClassSelector nestedClassSelector
				&& this.predicates.isAnnotatedWithClassTemplate.test(nestedClassSelector.getNestedClass())) {
			return resolveIterations(selector, context);
		}
		return unresolved();
	}

	private Resolution resolveIterations(IterationSelector selector, Context context) {
		DiscoverySelector parentSelector = selector.getParentSelector();
		Set<Match> matches = selector.getIterationIndices().stream() //
				.map(index -> context.addToParent(() -> parentSelector,
					parent -> Optional.of(newDummyClassTemplateInvocationTestDescriptor(parent, index + 1)))) //
				.map(this::toInvocationMatch) //
				.flatMap(Optional::stream) //
				.collect(toSet());
		return matches.isEmpty() ? unresolved() : Resolution.matches(matches);
	}

	private Resolution resolveStandaloneClassUniqueId(Context context, UniqueId.Segment lastSegment,
			Predicate<? super Class<?>> condition,
			BiFunction<TestDescriptor, Class<?>, ClassBasedTestDescriptor> factory) {

		String className = lastSegment.getValue();
		return ReflectionSupport.tryToLoadClass(className).toOptional() //
				.filter(this.predicates::isValidStandaloneTestClass) //
				.filter(condition) //
				.map(testClass -> toResolution(
					context.addToParent(parent -> Optional.of(factory.apply(parent, testClass))))) //
				.orElse(unresolved());
	}

	private Resolution resolveNestedClassUniqueId(Context context, UniqueId uniqueId,
			Predicate<? super Class<?>> condition,
			BiFunction<TestDescriptor, Class<?>, ClassBasedTestDescriptor> factory) {

		String simpleClassName = uniqueId.getLastSegment().getValue();
		return toResolution(context.addToParent(() -> selectUniqueId(uniqueId.removeLastSegment()), parent -> {
			Class<?> parentTestClass = ((TestClassAware) parent).getTestClass();
			return ReflectionSupport.findNestedClasses(parentTestClass,
				this.predicates.isAnnotatedWithNestedAndValid.and(
					where(Class::getSimpleName, isEqual(simpleClassName)))).stream() //
					.findFirst() //
					.filter(condition) //
					.map(testClass -> factory.apply(parent, testClass));
		}));
	}

	private ClassTemplateInvocationTestDescriptor newDummyClassTemplateInvocationTestDescriptor(TestDescriptor parent,
			int index) {
		UniqueId uniqueId = parent.getUniqueId().append(ClassTemplateInvocationTestDescriptor.SEGMENT_TYPE,
			"#" + index);
		return new ClassTemplateInvocationTestDescriptor(uniqueId, (ClassTemplateTestDescriptor) parent,
			DummyClassTemplateInvocationContext.INSTANCE, index, parent.getSource().orElse(null), configuration);
	}

	private ClassBasedTestDescriptor newStandaloneClassTestDescriptor(TestDescriptor parent, Class<?> testClass) {
		return this.predicates.isAnnotatedWithClassTemplate.test(testClass) //
				? newClassTemplateTestDescriptor(parent, testClass) //
				: newClassTestDescriptor(parent, testClass);
	}

	private ClassTemplateTestDescriptor newClassTemplateTestDescriptor(TestDescriptor parent, Class<?> testClass) {
		return newClassTemplateTestDescriptor(parent, ClassTemplateTestDescriptor.STANDALONE_CLASS_SEGMENT_TYPE,
			newClassTestDescriptor(parent, testClass));
	}

	private ClassTestDescriptor newClassTestDescriptor(TestDescriptor parent, Class<?> testClass) {
		return new ClassTestDescriptor(
			parent.getUniqueId().append(ClassTestDescriptor.SEGMENT_TYPE, testClass.getName()), testClass,
			configuration);
	}

	private ClassBasedTestDescriptor newMemberClassTestDescriptor(TestDescriptor parent, Class<?> testClass) {
		return this.predicates.isAnnotatedWithClassTemplate.test(testClass) //
				? newNestedClassTemplateTestDescriptor(parent, testClass) //
				: newNestedClassTestDescriptor(parent, testClass);
	}

	private ClassTemplateTestDescriptor newNestedClassTemplateTestDescriptor(TestDescriptor parent,
			Class<?> testClass) {
		return newClassTemplateTestDescriptor(parent, ClassTemplateTestDescriptor.NESTED_CLASS_SEGMENT_TYPE,
			newNestedClassTestDescriptor(parent, testClass));
	}

	private NestedClassTestDescriptor newNestedClassTestDescriptor(TestDescriptor parent, Class<?> testClass) {
		UniqueId uniqueId = parent.getUniqueId().append(NestedClassTestDescriptor.SEGMENT_TYPE,
			testClass.getSimpleName());
		return new NestedClassTestDescriptor(uniqueId, testClass, () -> getEnclosingTestClasses(parent), configuration);
	}

	private ClassTemplateTestDescriptor newClassTemplateTestDescriptor(TestDescriptor parent, String segmentType,
			ClassBasedTestDescriptor delegate) {

		delegate.setParent(parent);
		String segmentValue = delegate.getUniqueId().getLastSegment().getValue();
		UniqueId uniqueId = parent.getUniqueId().append(segmentType, segmentValue);
		return new ClassTemplateTestDescriptor(uniqueId, delegate);
	}

	private Optional<Match> toInvocationMatch(Optional<ClassTemplateInvocationTestDescriptor> testDescriptor) {
		return testDescriptor //
				.map(it -> Match.exact(it, expansionCallback(it,
					() -> it.getParent().map(parent -> getTestClasses((TestClassAware) parent)).orElse(emptyList()))));
	}

	private Resolution toResolution(Optional<? extends ClassBasedTestDescriptor> testDescriptor) {
		return testDescriptor //
				.map(it -> Resolution.match(Match.exact(it, expansionCallback(it)))) //
				.orElse(unresolved());
	}

	private Supplier<Set<? extends DiscoverySelector>> expansionCallback(ClassBasedTestDescriptor testDescriptor) {
		return expansionCallback(testDescriptor, () -> getTestClasses(testDescriptor));
	}

	private static List<Class<?>> getTestClasses(TestClassAware testDescriptor) {
		List<Class<?>> testClasses = new ArrayList<>(testDescriptor.getEnclosingTestClasses());
		testClasses.add(testDescriptor.getTestClass());
		return testClasses;
	}

	private Supplier<Set<? extends DiscoverySelector>> expansionCallback(TestDescriptor testDescriptor,
			Supplier<List<Class<?>>> testClassesSupplier) {
		return () -> {
			if (testDescriptor instanceof Filterable filterable) {
				filterable.getDynamicDescendantFilter().allowAll();
			}
			List<Class<?>> testClasses = testClassesSupplier.get();
			Class<?> testClass = testClasses.get(testClasses.size() - 1);
			Stream<DiscoverySelector> methods = findMethods(testClass,
				this.predicates.isTestOrTestFactoryOrTestTemplateMethod, TOP_DOWN).stream() //
						.map(method -> selectMethod(testClasses, method));
			Stream<Class<?>> annotatedNestedClasses = streamNestedClasses(testClass,
				this.predicates.isAnnotatedWithNested);
			Stream<Class<?>> notAnnotatedInnerClasses = streamNestedClasses(testClass,
				this.predicates.isAnnotatedWithNested.negate().and(ReflectionUtils::isInnerClass),
				CycleErrorHandling.ABORT_VISIT);
			var nestedClasses = Stream.concat(annotatedNestedClasses, notAnnotatedInnerClasses) //
					.map(nestedClass -> DiscoverySelectors.selectNestedClass(testClasses, nestedClass));
			return Stream.concat(methods, nestedClasses).collect(
				toCollection((Supplier<Set<DiscoverySelector>>) LinkedHashSet::new));
		};
	}

	private DiscoverySelector selectClass(List<Class<?>> classes) {
		if (classes.size() == 1) {
			return DiscoverySelectors.selectClass(classes.get(0));
		}
		int lastIndex = classes.size() - 1;
		return DiscoverySelectors.selectNestedClass(classes.subList(0, lastIndex), classes.get(lastIndex));
	}

	private DiscoverySelector selectMethod(List<Class<?>> classes, Method method) {
		return new DeclaredMethodSelector(classes, method);
	}

	static class DummyClassTemplateInvocationContext implements ClassTemplateInvocationContext {
		private static final DummyClassTemplateInvocationContext INSTANCE = new DummyClassTemplateInvocationContext();
	}
}
