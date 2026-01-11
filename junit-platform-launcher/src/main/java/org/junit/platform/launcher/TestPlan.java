/*
 * Copyright 2015-2026 the original author or authors.
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v2.0 which
 * accompanies this distribution and is available at
 *
 * https://www.eclipse.org/legal/epl-v20.html
 */

package org.junit.platform.launcher;

import static java.util.Collections.emptySet;
import static java.util.Collections.synchronizedSet;
import static java.util.Collections.unmodifiableSet;
import static org.apiguardian.api.API.Status.DEPRECATED;
import static org.apiguardian.api.API.Status.INTERNAL;
import static org.apiguardian.api.API.Status.MAINTAINED;
import static org.apiguardian.api.API.Status.STABLE;

import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Predicate;

import org.apiguardian.api.API;
import org.junit.platform.commons.PreconditionViolationException;
import org.junit.platform.commons.util.Preconditions;
import org.junit.platform.engine.ConfigurationParameters;
import org.junit.platform.engine.OutputDirectoryCreator;
import org.junit.platform.engine.TestDescriptor;
import org.junit.platform.engine.UniqueId;

/**
 * {@code TestPlan} describes the tree of tests and containers as discovered
 * by a {@link Launcher}.
 *
 * <p>Tests and containers are represented by {@link TestIdentifier} instances.
 * The complete set of identifiers comprises a tree-like structure. However,
 * each identifier only stores the unique ID of its parent. This class provides
 * a number of helpful methods to retrieve the
 * {@linkplain #getParent(TestIdentifier) parent},
 * {@linkplain #getChildren(TestIdentifier) children}, and
 * {@linkplain #getDescendants(TestIdentifier) descendants} of an identifier.
 *
 * <p>While the contained instances of {@link TestIdentifier} are immutable,
 * instances of this class contain mutable state. For example, when a dynamic
 * test is registered at runtime, it is added to the original test plan and
 * reported to {@link TestExecutionListener} implementations.
 *
 * <p>This class is not intended to be extended by clients.
 *
 * @since 1.0
 * @see Launcher
 * @see TestExecutionListener
 */
@API(status = STABLE, since = "1.0")
public class TestPlan {

	private final Set<TestIdentifier> roots = synchronizedSet(new LinkedHashSet<>(4));
	private final Map<UniqueId, Set<TestIdentifier>> children = new ConcurrentHashMap<>(32);
	private final Map<UniqueId, TestIdentifier> allIdentifiers = new ConcurrentHashMap<>(32);

	private final boolean containsTests;
	private final ConfigurationParameters configurationParameters;
	private final OutputDirectoryCreator outputDirectoryCreator;

	/**
	 * Construct a new {@code TestPlan} from the supplied collection of
	 * {@link TestDescriptor TestDescriptors}.
	 *
	 * <p>Each supplied {@code TestDescriptor} is expected to be a descriptor
	 * for a {@link org.junit.platform.engine.TestEngine TestEngine}.
	 *
	 * @param containsTests whether the test plan contains tests
	 * @param engineDescriptors the engine test descriptors from which the test
	 * plan should be created; never {@code null}
	 * @param configurationParameters the {@code ConfigurationParameters} for
	 * this test plan; never {@code null}
	 * @param outputDirectoryCreator the {@code OutputDirectoryProvider} for
	 * this test plan; never {@code null}
	 * @return a new test plan
	 */
	@API(status = INTERNAL, since = "1.14")
	public static TestPlan from(boolean containsTests, Collection<TestDescriptor> engineDescriptors,
			ConfigurationParameters configurationParameters, OutputDirectoryCreator outputDirectoryCreator) {
		Preconditions.notNull(engineDescriptors, "Cannot create TestPlan from a null collection of TestDescriptors");
		Preconditions.notNull(configurationParameters, "Cannot create TestPlan from null ConfigurationParameters");
		TestPlan testPlan = new TestPlan(containsTests, configurationParameters, outputDirectoryCreator);
		TestDescriptor.Visitor visitor = descriptor -> testPlan.addInternal(TestIdentifier.from(descriptor));
		engineDescriptors.forEach(engineDescriptor -> engineDescriptor.accept(visitor));
		return testPlan;
	}

	@API(status = INTERNAL, since = "1.4")
	protected TestPlan(boolean containsTests, ConfigurationParameters configurationParameters,
			OutputDirectoryCreator outputDirectoryCreator) {
		this.containsTests = containsTests;
		this.configurationParameters = configurationParameters;
		this.outputDirectoryCreator = outputDirectoryCreator;
	}

	@API(status = INTERNAL, since = "1.8")
	public void addInternal(TestIdentifier testIdentifier) {
		Preconditions.notNull(testIdentifier, "testIdentifier must not be null");
		allIdentifiers.put(testIdentifier.getUniqueIdObject(), testIdentifier);

		// Root identifiers. Typically, a test engine.
		if (testIdentifier.getParentIdObject().isEmpty()) {
			roots.add(testIdentifier);
			return;
		}

		// Identifiers without a parent in this test plan. Could be a test
		// engine that is used in a suite.
		UniqueId parentId = testIdentifier.getParentIdObject().get();
		if (!allIdentifiers.containsKey(parentId)) {
			roots.add(testIdentifier);
			return;
		}

		Set<TestIdentifier> directChildren = children.computeIfAbsent(parentId,
			key -> synchronizedSet(new LinkedHashSet<>(16)));
		directChildren.add(testIdentifier);
	}

	/**
	 * Get the root {@link TestIdentifier TestIdentifiers} for this test plan.
	 *
	 * @return an unmodifiable set of the root identifiers
	 */
	public Set<TestIdentifier> getRoots() {
		return unmodifiableSet(roots);
	}

	/**
	 * Get the parent of the supplied {@link TestIdentifier}.
	 *
	 * @param child the identifier to look up the parent for; never {@code null}
	 * @return an {@code Optional} containing the parent, if present
	 */
	public Optional<TestIdentifier> getParent(TestIdentifier child) {
		Preconditions.notNull(child, "child must not be null");
		return child.getParentIdObject().map(this::getTestIdentifier);
	}

	/**
	 * Get the children of the supplied {@link TestIdentifier}.
	 *
	 * @param parent the identifier to look up the children for; never {@code null}
	 * @return an unmodifiable set of the parent's children, potentially empty
	 * @see #getChildren(UniqueId)
	 */
	public Set<TestIdentifier> getChildren(TestIdentifier parent) {
		Preconditions.notNull(parent, "parent must not be null");
		return getChildren(parent.getUniqueIdObject());
	}

	/**
	 * Get the children of the supplied unique ID.
	 *
	 * @param parentId the unique ID to look up the children for; never
	 * {@code null}
	 * @return an unmodifiable set of the parent's children, potentially empty
	 * @see #getChildren(TestIdentifier)
	 */
	@API(status = MAINTAINED, since = "1.10")
	public Set<TestIdentifier> getChildren(UniqueId parentId) {
		return children.containsKey(parentId) ? unmodifiableSet(children.get(parentId)) : emptySet();
	}

	/**
	 * Get the {@link TestIdentifier} with the supplied unique ID.
	 *
	 * @param uniqueId the unique ID to look up the identifier for; never
	 * {@code null} or blank
	 * @return the identifier with the supplied unique ID; never {@code null}
	 * @throws PreconditionViolationException if no {@code TestIdentifier}
	 * with the supplied unique ID is present in this test plan
	 * @deprecated Use {@link #getTestIdentifier(UniqueId)} instead.
	 */
	@API(status = DEPRECATED, since = "1.10", consumers = "Gradle")
	@Deprecated(since = "1.10")
	public TestIdentifier getTestIdentifier(String uniqueId) throws PreconditionViolationException {
		Preconditions.notBlank(uniqueId, "unique ID must not be null or blank");
		return getTestIdentifier(UniqueId.parse(uniqueId));
	}

	/**
	 * Get the {@link TestIdentifier} with the supplied unique ID.
	 *
	 * @param uniqueId the unique ID to look up the identifier for; never
	 * {@code null}
	 * @return the identifier with the supplied unique ID; never {@code null}
	 * @throws PreconditionViolationException if no {@code TestIdentifier}
	 * with the supplied unique ID is present in this test plan
	 */
	@API(status = MAINTAINED, since = "1.10")
	public TestIdentifier getTestIdentifier(UniqueId uniqueId) {
		Preconditions.notNull(uniqueId, () -> "uniqueId must not be null");
		return Preconditions.notNull(allIdentifiers.get(uniqueId),
			() -> "No TestIdentifier with unique ID [" + uniqueId + "] has been added to this TestPlan.");
	}

	/**
	 * Count all {@link TestIdentifier TestIdentifiers} that satisfy the
	 * given {@linkplain Predicate predicate}.
	 *
	 * @param predicate a predicate which returns {@code true} for identifiers
	 * to be counted; never {@code null}
	 * @return the number of identifiers that satisfy the supplied predicate
	 */
	public long countTestIdentifiers(Predicate<? super TestIdentifier> predicate) {
		Preconditions.notNull(predicate, "Predicate must not be null");
		return allIdentifiers.values().stream().filter(predicate).count();
	}

	/**
	 * Get all descendants of the supplied {@link TestIdentifier} (i.e.,
	 * all of its children and their children, recursively).
	 *
	 * @param parent the identifier to look up the descendants for; never {@code null}
	 * @return an unmodifiable set of the parent's descendants, potentially empty
	 */
	public Set<TestIdentifier> getDescendants(TestIdentifier parent) {
		Preconditions.notNull(parent, "parent must not be null");
		Set<TestIdentifier> result = new LinkedHashSet<>(16);
		Set<TestIdentifier> children = getChildren(parent);
		result.addAll(children);
		for (TestIdentifier child : children) {
			result.addAll(getDescendants(child));
		}
		return unmodifiableSet(result);
	}

	/**
	 * Return whether this test plan contains any tests.
	 *
	 * <p>A test plan contains tests, if at least one of the contained engine
	 * descriptors {@linkplain TestDescriptor#containsTests(TestDescriptor)
	 * contains tests}.
	 *
	 * @return {@code true} if this test plan contains tests
	 * @see TestDescriptor#containsTests(TestDescriptor)
	 */
	public boolean containsTests() {
		return containsTests;
	}

	/**
	 * Get the {@link ConfigurationParameters} for this test plan.
	 *
	 * @return the configuration parameters; never {@code null}
	 * @since 1.8
	 */
	@API(status = MAINTAINED, since = "1.8")
	public ConfigurationParameters getConfigurationParameters() {
		return this.configurationParameters;
	}

	/**
	 * Get the
	 * {@link org.junit.platform.engine.reporting.OutputDirectoryProvider} for
	 * this test plan.
	 *
	 * @return the output directory provider; never {@code null}
	 * @since 1.12
	 * @deprecated Please use {@link #getOutputDirectoryCreator()} instead
	 */
	@SuppressWarnings("removal")
	@API(status = DEPRECATED, since = "1.14")
	@Deprecated(since = "1.14", forRemoval = true)
	public org.junit.platform.engine.reporting.OutputDirectoryProvider getOutputDirectoryProvider() {
		return org.junit.platform.engine.reporting.OutputDirectoryProvider.castOrAdapt(getOutputDirectoryCreator());
	}

	/**
	 * Get the {@link OutputDirectoryCreator} for this test plan.
	 *
	 * @return the output directory creator; never {@code null}
	 * @since 1.14
	 */
	@API(status = MAINTAINED, since = "1.14")
	public OutputDirectoryCreator getOutputDirectoryCreator() {
		return outputDirectoryCreator;
	}

	/**
	 * Accept the supplied {@link Visitor} for a depth-first traversal of the
	 * test plan.
	 *
	 * @param visitor the visitor to accept; never {@code null}
	 * @since 1.10
	 */
	@API(status = MAINTAINED, since = "1.13.3")
	public void accept(Visitor visitor) {
		getRoots().forEach(it -> accept(visitor, it));
	}

	private void accept(Visitor visitor, TestIdentifier testIdentifier) {
		if (testIdentifier.isContainer()) {
			visitor.preVisitContainer(testIdentifier);
		}
		visitor.visit(testIdentifier);
		getChildren(testIdentifier).forEach(it -> accept(visitor, it));
		if (testIdentifier.isContainer()) {
			visitor.postVisitContainer(testIdentifier);
		}
	}

	/**
	 * Visitor for {@link TestIdentifier TestIdentifiers} in a {@link TestPlan}.
	 *
	 * @since 1.10
	 */
	@API(status = MAINTAINED, since = "1.13.3")
	public interface Visitor {

		/**
		 * Called before visiting a container.
		 *
		 * @see TestIdentifier#isContainer()
		 */
		default void preVisitContainer(TestIdentifier testIdentifier) {
		}

		/**
		 * Called for all test identifiers regardless of their type.
		 */
		default void visit(TestIdentifier testIdentifier) {
		}

		/**
		 * Called after visiting a container.
		 *
		 * @see TestIdentifier#isContainer()
		 */
		default void postVisitContainer(TestIdentifier testIdentifier) {
		}
	}
}
