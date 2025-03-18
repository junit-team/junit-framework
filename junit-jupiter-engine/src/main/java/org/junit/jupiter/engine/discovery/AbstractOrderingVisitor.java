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
import static java.util.stream.Collectors.toCollection;
import static java.util.stream.Collectors.toList;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Stream;

import org.junit.platform.commons.logging.Logger;
import org.junit.platform.commons.logging.LoggerFactory;
import org.junit.platform.commons.util.UnrecoverableExceptions;
import org.junit.platform.engine.TestDescriptor;

/**
 * Abstract base class for {@linkplain TestDescriptor.Visitor visitors} that
 * order children nodes.
 *
 * @since 5.8
 */
abstract class AbstractOrderingVisitor implements TestDescriptor.Visitor {

	private static final Logger logger = LoggerFactory.getLogger(AbstractOrderingVisitor.class);

	/**
	 * @param <PARENT> the parent container type to search in for matching children
	 */
	@SuppressWarnings("unchecked")
	protected <PARENT extends TestDescriptor> void doWithMatchingDescriptor(Class<PARENT> parentTestDescriptorType,
			TestDescriptor testDescriptor, Consumer<PARENT> action, Function<PARENT, String> errorMessageBuilder) {

		if (parentTestDescriptorType.isInstance(testDescriptor)) {
			PARENT parentTestDescriptor = (PARENT) testDescriptor;
			try {
				action.accept(parentTestDescriptor);
			}
			catch (Throwable t) {
				UnrecoverableExceptions.rethrowIfUnrecoverable(t);
				logger.error(t, () -> errorMessageBuilder.apply(parentTestDescriptor));
			}
		}
	}

	/**
	 * @param <CHILD> the type of children (containers or tests) to order
	 */
	protected <CHILD extends TestDescriptor, WRAPPER extends AbstractAnnotatedDescriptorWrapper<?>> void orderChildrenTestDescriptors(
			TestDescriptor parentTestDescriptor, Class<CHILD> matchingChildrenType,
			Function<CHILD, WRAPPER> descriptorWrapperFactory,
			DescriptorWrapperOrderer<WRAPPER> descriptorWrapperOrderer) {

		if (!descriptorWrapperOrderer.canOrderWrappers()) {
			return;
		}

		List<WRAPPER> matchingDescriptorWrappers = parentTestDescriptor.getChildren()//
				.stream()//
				.filter(matchingChildrenType::isInstance)//
				.map(matchingChildrenType::cast)//
				.map(descriptorWrapperFactory)//
				.collect(toCollection(ArrayList::new));

		// If there are no children to order, abort early.
		if (matchingDescriptorWrappers.isEmpty()) {
			return;
		}

		parentTestDescriptor.orderChildren(children -> {
			Stream<TestDescriptor> nonMatchingTestDescriptors = children.stream()//
					.filter(childTestDescriptor -> !matchingChildrenType.isInstance(childTestDescriptor));

			descriptorWrapperOrderer.orderWrappers(matchingDescriptorWrappers);

			Stream<TestDescriptor> orderedTestDescriptors = matchingDescriptorWrappers.stream()//
					.map(AbstractAnnotatedDescriptorWrapper::getTestDescriptor);

			if (shouldNonMatchingDescriptorsComeBeforeOrderedOnes()) {
				return Stream.concat(nonMatchingTestDescriptors, orderedTestDescriptors)//
						.collect(toList());
			}
			else {
				return Stream.concat(orderedTestDescriptors, nonMatchingTestDescriptors)//
						.collect(toList());
			}
		});
	}

	protected abstract boolean shouldNonMatchingDescriptorsComeBeforeOrderedOnes();

	/**
	 * @param <WRAPPER> the wrapper type for the children to order
	 */
	protected static class DescriptorWrapperOrderer<WRAPPER> {

		private static final DescriptorWrapperOrderer<?> NOOP = new DescriptorWrapperOrderer<>(null, __ -> "",
			___ -> "");

		@SuppressWarnings("unchecked")
		protected static <WRAPPER> DescriptorWrapperOrderer<WRAPPER> noop() {
			return (DescriptorWrapperOrderer<WRAPPER>) NOOP;
		}

		private final Consumer<List<WRAPPER>> orderingAction;
		private final MessageGenerator descriptorsAddedMessageGenerator;
		private final MessageGenerator descriptorsRemovedMessageGenerator;

		DescriptorWrapperOrderer(Consumer<List<WRAPPER>> orderingAction,
				MessageGenerator descriptorsAddedMessageGenerator,
				MessageGenerator descriptorsRemovedMessageGenerator) {

			this.orderingAction = orderingAction;
			this.descriptorsAddedMessageGenerator = descriptorsAddedMessageGenerator;
			this.descriptorsRemovedMessageGenerator = descriptorsRemovedMessageGenerator;
		}

		private boolean canOrderWrappers() {
			return this.orderingAction != null;
		}

		private void orderWrappers(List<WRAPPER> wrappers) {
			List<WRAPPER> orderedWrappers = new ArrayList<>(wrappers);
			this.orderingAction.accept(orderedWrappers);
			Map<Object, Integer> distinctWrappersToIndex = distinctWrappersToIndex(orderedWrappers);

			int difference = orderedWrappers.size() - wrappers.size();
			int distinctDifference = distinctWrappersToIndex.size() - wrappers.size();
			if (difference > 0) { // difference >= distinctDifference
				logDescriptorsAddedWarning(difference);
			}
			if (distinctDifference < 0) { // distinctDifference <= difference
				logDescriptorsRemovedWarning(distinctDifference);
			}

			wrappers.sort(comparing(wrapper -> distinctWrappersToIndex.getOrDefault(wrapper, -1)));
		}

		private Map<Object, Integer> distinctWrappersToIndex(List<?> wrappers) {
			Map<Object, Integer> toIndex = new HashMap<>();
			for (int i = 0; i < wrappers.size(); i++) {
				// Avoid ClassCastException if a misbehaving ordering action added a non-WRAPPER
				Object wrapper = wrappers.get(i);
				if (!toIndex.containsKey(wrapper)) {
					toIndex.put(wrapper, i);
				}
			}
			return toIndex;
		}

		private void logDescriptorsAddedWarning(int number) {
			logger.warn(() -> this.descriptorsAddedMessageGenerator.generateMessage(number));
		}

		private void logDescriptorsRemovedWarning(int number) {
			logger.warn(() -> this.descriptorsRemovedMessageGenerator.generateMessage(Math.abs(number)));
		}

	}

	@FunctionalInterface
	protected interface MessageGenerator {

		String generateMessage(int number);
	}

}
