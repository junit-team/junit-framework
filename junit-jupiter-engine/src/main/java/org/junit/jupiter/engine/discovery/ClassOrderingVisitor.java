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

import static org.junit.platform.commons.support.AnnotationSupport.isAnnotated;

import java.util.Optional;
import java.util.function.Consumer;

import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.ClassOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.TestClassOrder;
import org.junit.jupiter.engine.config.JupiterConfiguration;
import org.junit.jupiter.engine.descriptor.ClassBasedTestDescriptor;
import org.junit.jupiter.engine.descriptor.JupiterEngineDescriptor;
import org.junit.jupiter.engine.descriptor.TestClassAware;
import org.junit.platform.commons.support.AnnotationSupport;
import org.junit.platform.commons.support.ReflectionSupport;
import org.junit.platform.commons.util.LruCache;
import org.junit.platform.engine.DiscoveryIssue;
import org.junit.platform.engine.DiscoveryIssue.Severity;
import org.junit.platform.engine.TestDescriptor;
import org.junit.platform.engine.support.discovery.DiscoveryIssueReporter;
import org.junit.platform.engine.support.discovery.DiscoveryIssueReporter.Condition;

/**
 * @since 5.8
 */
class ClassOrderingVisitor extends AbstractOrderingVisitor {

	private final LruCache<ClassBasedTestDescriptor, DescriptorWrapperOrderer<TestDescriptor, ClassOrderer, DefaultClassDescriptor>> ordererCache = new LruCache<>(
		10);
	private final JupiterConfiguration configuration;
	private final DescriptorWrapperOrderer<TestDescriptor, ClassOrderer, DefaultClassDescriptor> globalOrderer;
	private final Condition<ClassBasedTestDescriptor> noOrderAnnotation;

	ClassOrderingVisitor(JupiterConfiguration configuration, DiscoveryIssueReporter issueReporter) {
		super(issueReporter);
		this.configuration = configuration;
		this.globalOrderer = createGlobalOrderer(configuration);
		this.noOrderAnnotation = issueReporter.createReportingCondition(
			testDescriptor -> !isAnnotated(testDescriptor.getTestClass(), Order.class), testDescriptor -> {
				String message = """
						Ineffective @Order annotation on class '%s'. \
						It will not be applied because ClassOrderer.OrderAnnotation is not in use. \
						Note that the annotation may be either directly present or meta-present on the class."""//
						.formatted(testDescriptor.getTestClass().getName());
				return DiscoveryIssue.builder(Severity.INFO, message) //
						.source(testDescriptor.getSource()) //
						.build();
			});
	}

	@Override
	public void visit(TestDescriptor testDescriptor) {
		doWithMatchingDescriptor(JupiterEngineDescriptor.class, testDescriptor, this::orderTopLevelClasses,
			descriptor -> "Failed to order top-level classes");
		doWithMatchingDescriptor(ClassBasedTestDescriptor.class, testDescriptor, this::orderNestedClasses,
			descriptor -> "Failed to order nested classes for " + descriptor.getTestClass());
	}

	@Override
	protected boolean shouldNonMatchingDescriptorsComeBeforeOrderedOnes() {
		// Non-matching descriptors can only occur when ordering nested classes in which
		// case they contain only local test methods (for @Nested classes) which must be
		// executed before tests in @Nested test classes. So we add the test methods before
		// adding the @Nested test classes.
		return true;
	}

	private void orderTopLevelClasses(JupiterEngineDescriptor engineDescriptor) {
		orderChildrenTestDescriptors(//
			engineDescriptor, //
			ClassBasedTestDescriptor.class, //
			toValidationAction(globalOrderer.getOrderer()), //
			DefaultClassDescriptor::new, //
			globalOrderer);
	}

	private void orderNestedClasses(ClassBasedTestDescriptor descriptor) {
		var wrapperOrderer = createAndCacheClassLevelOrderer(descriptor);
		orderChildrenTestDescriptors(//
			descriptor, //
			ClassBasedTestDescriptor.class, //
			toValidationAction(wrapperOrderer.getOrderer()), //
			DefaultClassDescriptor::new, //
			wrapperOrderer);
	}

	private DescriptorWrapperOrderer<TestDescriptor, ClassOrderer, DefaultClassDescriptor> createGlobalOrderer(
			JupiterConfiguration configuration) {
		ClassOrderer classOrderer = configuration.getDefaultTestClassOrderer().orElse(null);
		return classOrderer == null ? DescriptorWrapperOrderer.noop() : createDescriptorWrapperOrderer(classOrderer);
	}

	private DescriptorWrapperOrderer<TestDescriptor, ClassOrderer, DefaultClassDescriptor> createAndCacheClassLevelOrderer(
			ClassBasedTestDescriptor classBasedTestDescriptor) {
		var orderer = createClassLevelOrderer(classBasedTestDescriptor);
		ordererCache.put(classBasedTestDescriptor, orderer);
		return orderer;
	}

	private DescriptorWrapperOrderer<TestDescriptor, ClassOrderer, DefaultClassDescriptor> createClassLevelOrderer(
			ClassBasedTestDescriptor classBasedTestDescriptor) {
		return AnnotationSupport.findAnnotation(classBasedTestDescriptor.getTestClass(), TestClassOrder.class)//
				.map(TestClassOrder::value)//
				.map(this::createDescriptorWrapperOrderer)//
				.orElseGet(() -> {
					Object parent = classBasedTestDescriptor.getParent().orElse(null);
					if (parent instanceof ClassBasedTestDescriptor parentClassTestDescriptor) {
						var cacheEntry = ordererCache.get(parentClassTestDescriptor);
						return cacheEntry != null ? cacheEntry : createClassLevelOrderer(parentClassTestDescriptor);
					}
					return globalOrderer;
				});
	}

	private DescriptorWrapperOrderer<TestDescriptor, ClassOrderer, DefaultClassDescriptor> createDescriptorWrapperOrderer(
			Class<? extends ClassOrderer> ordererClass) {
		if (ordererClass == ClassOrderer.Default.class) {
			return globalOrderer;
		}
		return createDescriptorWrapperOrderer(ReflectionSupport.newInstance(ordererClass));
	}

	private DescriptorWrapperOrderer<TestDescriptor, ClassOrderer, DefaultClassDescriptor> createDescriptorWrapperOrderer(
			ClassOrderer classOrderer) {
		OrderingAction<TestDescriptor, DefaultClassDescriptor> orderingAction = (__,
				classDescriptors) -> classOrderer.orderClasses(
					new DefaultClassOrdererContext(classDescriptors, this.configuration));

		MessageGenerator<TestDescriptor> descriptorsAddedMessageGenerator = (parent,
				number) -> "ClassOrderer [%s] added %d %s which will be ignored.".formatted(
					classOrderer.getClass().getName(), number, describeClassDescriptors(parent));
		MessageGenerator<TestDescriptor> descriptorsRemovedMessageGenerator = (parent,
				number) -> "ClassOrderer [%s] removed %d %s which will be retained with arbitrary ordering.".formatted(
					classOrderer.getClass().getName(), number, describeClassDescriptors(parent));

		return new DescriptorWrapperOrderer<>(classOrderer, orderingAction, descriptorsAddedMessageGenerator,
			descriptorsRemovedMessageGenerator);
	}

	private static String describeClassDescriptors(TestDescriptor parent) {
		return parent instanceof TestClassAware testClassAware //
				? "nested ClassDescriptor(s) for test class [%s]".formatted(testClassAware.getTestClass().getName()) //
				: "top-level ClassDescriptor(s)";
	}

	private Optional<Consumer<ClassBasedTestDescriptor>> toValidationAction(@Nullable ClassOrderer orderer) {
		if (orderer instanceof ClassOrderer.OrderAnnotation) {
			return Optional.empty();
		}
		return Optional.of(noOrderAnnotation::check);
	}

}
