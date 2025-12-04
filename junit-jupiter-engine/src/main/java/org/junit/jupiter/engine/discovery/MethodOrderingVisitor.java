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

import static java.util.Comparator.comparing;
import static org.junit.platform.commons.support.AnnotationSupport.isAnnotated;

import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.UnaryOperator;

import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.TestMethodOrder;
import org.junit.jupiter.engine.config.JupiterConfiguration;
import org.junit.jupiter.engine.descriptor.ClassBasedTestDescriptor;
import org.junit.jupiter.engine.descriptor.JupiterTestDescriptor;
import org.junit.jupiter.engine.descriptor.MethodBasedTestDescriptor;
import org.junit.platform.commons.support.AnnotationSupport;
import org.junit.platform.commons.support.ReflectionSupport;
import org.junit.platform.commons.util.LruCache;
import org.junit.platform.engine.DiscoveryIssue;
import org.junit.platform.engine.DiscoveryIssue.Severity;
import org.junit.platform.engine.TestDescriptor;
import org.junit.platform.engine.support.discovery.DiscoveryIssueReporter;
import org.junit.platform.engine.support.discovery.DiscoveryIssueReporter.Condition;

/**
 * @since 5.5
 */
class MethodOrderingVisitor extends AbstractOrderingVisitor {

	private final LruCache<ClassBasedTestDescriptor, DescriptorWrapperOrderer<ClassBasedTestDescriptor, MethodOrderer, DefaultMethodDescriptor>> ordererCache = new LruCache<>(
		10);
	private final JupiterConfiguration configuration;
	private final DescriptorWrapperOrderer<ClassBasedTestDescriptor, MethodOrderer, DefaultMethodDescriptor> globalOrderer;
	private final Condition<MethodBasedTestDescriptor> noOrderAnnotation;

	// Not a static field to avoid initialization at build time for GraalVM
	private final UnaryOperator<List<TestDescriptor>> methodsBeforeNestedClassesOrderer;

	MethodOrderingVisitor(JupiterConfiguration configuration, DiscoveryIssueReporter issueReporter) {
		super(issueReporter);
		this.configuration = configuration;
		this.globalOrderer = createGlobalOrderer(configuration);
		this.noOrderAnnotation = issueReporter.createReportingCondition(
			testDescriptor -> !isAnnotated(testDescriptor.getTestMethod(), Order.class), testDescriptor -> {
				String message = """
						Ineffective @Order annotation on method '%s'. \
						It will not be applied because MethodOrderer.OrderAnnotation is not in use. \
						Note that the annotation may be either directly present or meta-present on the method."""//
						.formatted(testDescriptor.getTestMethod().toGenericString());
				return DiscoveryIssue.builder(Severity.INFO, message) //
						.source(testDescriptor.getSource()) //
						.build();
			});
		this.methodsBeforeNestedClassesOrderer = createMethodsBeforeNestedClassesOrderer();
	}

	@Override
	public void visit(TestDescriptor testDescriptor) {
		doWithMatchingDescriptor(ClassBasedTestDescriptor.class, testDescriptor, this::orderContainedMethods,
			descriptor -> "Failed to order methods for " + descriptor.getTestClass());
	}

	@Override
	protected boolean shouldNonMatchingDescriptorsComeBeforeOrderedOnes() {
		// Non-matching descriptors can only contain @Nested test classes which should be
		// added after local test methods.
		return false;
	}

	private void orderContainedMethods(ClassBasedTestDescriptor descriptor) {
		var wrapperOrderer = createAndCacheClassLevelOrderer(descriptor);
		var methodOrderer = wrapperOrderer.getOrderer();

		orderChildrenTestDescriptors(descriptor, //
			MethodBasedTestDescriptor.class, //
			toValidationAction(methodOrderer), //
			DefaultMethodDescriptor::new, //
			wrapperOrderer);

		if (methodOrderer == null) {
			// If there is an orderer, this is ensured by the call above
			descriptor.orderChildren(methodsBeforeNestedClassesOrderer);
		}
		else {
			// Note: MethodOrderer#getDefaultExecutionMode() is guaranteed
			// to be invoked after MethodOrderer#orderMethods().
			methodOrderer.getDefaultExecutionMode() //
					.map(JupiterTestDescriptor::toExecutionMode) //
					.ifPresent(descriptor::setDefaultChildExecutionMode);
		}
	}

	private DescriptorWrapperOrderer<ClassBasedTestDescriptor, MethodOrderer, DefaultMethodDescriptor> createGlobalOrderer(
			JupiterConfiguration configuration) {
		MethodOrderer methodOrderer = configuration.getDefaultTestMethodOrderer().orElse(null);
		return methodOrderer == null ? DescriptorWrapperOrderer.noop() : createDescriptorWrapperOrderer(methodOrderer);
	}

	private DescriptorWrapperOrderer<ClassBasedTestDescriptor, MethodOrderer, DefaultMethodDescriptor> createAndCacheClassLevelOrderer(
			ClassBasedTestDescriptor classBasedTestDescriptor) {
		var orderer = createClassLevelOrderer(classBasedTestDescriptor);
		ordererCache.put(classBasedTestDescriptor, orderer);
		return orderer;
	}

	private DescriptorWrapperOrderer<ClassBasedTestDescriptor, MethodOrderer, DefaultMethodDescriptor> createClassLevelOrderer(
			ClassBasedTestDescriptor classBasedTestDescriptor) {
		return AnnotationSupport.findAnnotation(classBasedTestDescriptor.getTestClass(), TestMethodOrder.class)//
				.map(TestMethodOrder::value)//
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

	private DescriptorWrapperOrderer<ClassBasedTestDescriptor, MethodOrderer, DefaultMethodDescriptor> createDescriptorWrapperOrderer(
			Class<? extends MethodOrderer> ordererClass) {
		if (ordererClass == MethodOrderer.Default.class) {
			return globalOrderer;
		}
		return createDescriptorWrapperOrderer(ReflectionSupport.newInstance(ordererClass));
	}

	private DescriptorWrapperOrderer<ClassBasedTestDescriptor, MethodOrderer, DefaultMethodDescriptor> createDescriptorWrapperOrderer(
			MethodOrderer methodOrderer) {
		OrderingAction<ClassBasedTestDescriptor, DefaultMethodDescriptor> orderingAction = (parent,
				methodDescriptors) -> methodOrderer.orderMethods(
					new DefaultMethodOrdererContext(parent.getTestClass(), methodDescriptors, this.configuration));

		MessageGenerator<ClassBasedTestDescriptor> descriptorsAddedMessageGenerator = (parent,
				number) -> "MethodOrderer [%s] added %d MethodDescriptor(s) for test class [%s] which will be ignored.".formatted(
					methodOrderer.getClass().getName(), number, parent.getTestClass().getName());
		MessageGenerator<ClassBasedTestDescriptor> descriptorsRemovedMessageGenerator = (parent,
				number) -> "MethodOrderer [%s] removed %d MethodDescriptor(s) for test class [%s] which will be retained with arbitrary ordering.".formatted(
					methodOrderer.getClass().getName(), number, parent.getTestClass().getName());

		return new DescriptorWrapperOrderer<>(methodOrderer, orderingAction, descriptorsAddedMessageGenerator,
			descriptorsRemovedMessageGenerator);
	}

	private Optional<Consumer<MethodBasedTestDescriptor>> toValidationAction(@Nullable MethodOrderer methodOrderer) {
		if (methodOrderer instanceof MethodOrderer.OrderAnnotation) {
			return Optional.empty();
		}
		return Optional.of(noOrderAnnotation::check);
	}

	private static UnaryOperator<List<TestDescriptor>> createMethodsBeforeNestedClassesOrderer() {
		var methodsFirst = comparing(MethodBasedTestDescriptor.class::isInstance).reversed();
		return children -> {
			children.sort(methodsFirst);
			return children;
		};
	}

}
