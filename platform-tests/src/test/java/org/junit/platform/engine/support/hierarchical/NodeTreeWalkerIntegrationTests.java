/*
 * Copyright 2015-2025 the original author or authors.
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v2.0 which
 * accompanies this distribution and is available at
 *
 * https://www.eclipse.org/legal/epl-v20.html
 */

package org.junit.platform.engine.support.hierarchical;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.InstanceOfAssertFactories.LIST;
import static org.junit.platform.commons.test.PreconditionAssertions.assertPreconditionViolationFor;
import static org.junit.platform.commons.util.CollectionUtils.getOnlyElement;
import static org.junit.platform.engine.discovery.DiscoverySelectors.selectClass;
import static org.junit.platform.engine.support.hierarchical.ExclusiveResource.GLOBAL_READ;
import static org.junit.platform.engine.support.hierarchical.ExclusiveResource.GLOBAL_READ_WRITE;
import static org.junit.platform.engine.support.hierarchical.ExclusiveResource.LockMode.READ;
import static org.junit.platform.engine.support.hierarchical.ExclusiveResource.LockMode.READ_WRITE;
import static org.junit.platform.engine.support.hierarchical.Node.ExecutionMode.SAME_THREAD;
import static org.junit.platform.launcher.core.LauncherDiscoveryRequestBuilder.request;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.locks.Lock;
import java.util.function.Function;

import org.jspecify.annotations.NullMarked;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.ResourceAccessMode;
import org.junit.jupiter.api.parallel.ResourceLock;
import org.junit.jupiter.api.parallel.Resources;
import org.junit.jupiter.engine.JupiterTestEngine;
import org.junit.platform.engine.TestDescriptor;
import org.junit.platform.engine.UniqueId;
import org.junit.platform.engine.support.descriptor.AbstractTestDescriptor;
import org.junit.platform.engine.support.descriptor.EngineDescriptor;

/**
 * @since 1.3
 */
class NodeTreeWalkerIntegrationTests {

	LockManager lockManager = new LockManager();
	NodeTreeWalker nodeTreeWalker = new NodeTreeWalker(lockManager);

	@Test
	void pullUpExclusiveChildResourcesToTestClass() {
		var engineDescriptor = discover(TestCaseWithResourceLock.class);

		var advisor = nodeTreeWalker.walk(engineDescriptor);

		var testClassDescriptor = getOnlyElement(engineDescriptor.getChildren());
		assertThat(advisor.getResourceLock(testClassDescriptor)).extracting(allLocks()) //
				.isEqualTo(List.of(getLock(GLOBAL_READ), getReadWriteLock("a"), getReadWriteLock("b")));
		assertThat(advisor.getForcedExecutionMode(testClassDescriptor)).isEmpty();

		var testMethodDescriptor = getOnlyElement(testClassDescriptor.getChildren());
		assertThat(advisor.getResourceLock(testMethodDescriptor)).extracting(allLocks()).isEqualTo(List.of());
		assertThat(advisor.getForcedExecutionMode(testMethodDescriptor)).contains(SAME_THREAD);
	}

	@Test
	void setsForceExecutionModeForChildrenWithWriteLocksOnClass() {
		var engineDescriptor = discover(TestCaseWithResourceWriteLockOnClass.class);

		var advisor = nodeTreeWalker.walk(engineDescriptor);

		var testClassDescriptor = getOnlyElement(engineDescriptor.getChildren());
		assertThat(advisor.getResourceLock(testClassDescriptor)).extracting(allLocks()) //
				.isEqualTo(List.of(getLock(GLOBAL_READ), getReadWriteLock("a")));
		assertThat(advisor.getForcedExecutionMode(testClassDescriptor)).isEmpty();

		var testMethodDescriptor = getOnlyElement(testClassDescriptor.getChildren());
		assertThat(advisor.getResourceLock(testMethodDescriptor)).extracting(allLocks()).isEqualTo(List.of());
		assertThat(advisor.getForcedExecutionMode(testMethodDescriptor)).contains(SAME_THREAD);
	}

	@Test
	void doesntSetForceExecutionModeForChildrenWithReadLocksOnClass() {
		var engineDescriptor = discover(TestCaseWithResourceReadLockOnClass.class);

		var advisor = nodeTreeWalker.walk(engineDescriptor);

		var testClassDescriptor = getOnlyElement(engineDescriptor.getChildren());
		assertThat(advisor.getResourceLock(testClassDescriptor)).extracting(allLocks()) //
				.isEqualTo(List.of(getLock(GLOBAL_READ), getReadLock("a")));
		assertThat(advisor.getForcedExecutionMode(testClassDescriptor)).isEmpty();

		var testMethodDescriptor = getOnlyElement(testClassDescriptor.getChildren());
		assertThat(advisor.getResourceLock(testMethodDescriptor)).extracting(allLocks()).isEqualTo(List.of());
		assertThat(advisor.getForcedExecutionMode(testMethodDescriptor)).isEmpty();
	}

	@Test
	void setsForceExecutionModeForChildrenWithReadLocksOnClassAndWriteLockOnTest() {
		var engineDescriptor = discover(TestCaseWithResourceReadLockOnClassAndWriteClockOnTestCase.class);

		var advisor = nodeTreeWalker.walk(engineDescriptor);

		var testClassDescriptor = getOnlyElement(engineDescriptor.getChildren());
		assertThat(advisor.getResourceLock(testClassDescriptor)).extracting(allLocks()) //
				.isEqualTo(List.of(getLock(GLOBAL_READ), getReadWriteLock("a")));
		assertThat(advisor.getForcedExecutionMode(testClassDescriptor)).isEmpty();

		var testMethodDescriptor = getOnlyElement(testClassDescriptor.getChildren());
		assertThat(advisor.getResourceLock(testMethodDescriptor)).extracting(allLocks()).isEqualTo(List.of());
		assertThat(advisor.getForcedExecutionMode(testMethodDescriptor)).contains(SAME_THREAD);
	}

	@Test
	void doesntSetForceExecutionModeForChildrenWithReadLocksOnClassAndReadLockOnTest() {
		var engineDescriptor = discover(TestCaseWithResourceReadLockOnClassAndReadClockOnTestCase.class);

		var advisor = nodeTreeWalker.walk(engineDescriptor);

		var testClassDescriptor = getOnlyElement(engineDescriptor.getChildren());
		assertThat(advisor.getResourceLock(testClassDescriptor)).extracting(allLocks()) //
				.isEqualTo(List.of(getLock(GLOBAL_READ), getReadLock("a"), getReadLock("b")));
		assertThat(advisor.getForcedExecutionMode(testClassDescriptor)).isEmpty();

		var testMethodDescriptor = getOnlyElement(testClassDescriptor.getChildren());
		assertThat(advisor.getResourceLock(testMethodDescriptor)).extracting(allLocks()).isEqualTo(List.of());
		assertThat(advisor.getForcedExecutionMode(testMethodDescriptor)).isEmpty();
	}

	@Test
	void leavesResourceLockOnTestMethodWhenClassDoesNotUseResource() {
		var engineDescriptor = discover(TestCaseWithoutResourceLock.class);

		var advisor = nodeTreeWalker.walk(engineDescriptor);

		var testClassDescriptor = getOnlyElement(engineDescriptor.getChildren());
		assertThat(advisor.getResourceLock(testClassDescriptor)).extracting(allLocks()) //
				.isEqualTo(List.of(getLock(GLOBAL_READ)));
		assertThat(advisor.getForcedExecutionMode(testClassDescriptor)).isEmpty();

		assertThat(testClassDescriptor.getChildren()).hasSize(2);
		var children = testClassDescriptor.getChildren().iterator();
		var testMethodDescriptor = children.next();
		assertThat(advisor.getResourceLock(testMethodDescriptor)).extracting(allLocks()) //
				.isEqualTo(List.of(getReadWriteLock("a")));
		assertThat(advisor.getForcedExecutionMode(testMethodDescriptor)).isEmpty();

		var nestedTestClassDescriptor = children.next();
		assertThat(advisor.getResourceLock(nestedTestClassDescriptor)).extracting(allLocks()) //
				.isEqualTo(List.of(getReadWriteLock("b"), getReadWriteLock("c")));
		assertThat(advisor.getForcedExecutionMode(nestedTestClassDescriptor)).isEmpty();

		var nestedTestMethodDescriptor = getOnlyElement(nestedTestClassDescriptor.getChildren());
		assertThat(advisor.getResourceLock(nestedTestMethodDescriptor)).extracting(allLocks()).isEqualTo(List.of());
		assertThat(advisor.getForcedExecutionMode(nestedTestMethodDescriptor)).contains(SAME_THREAD);
	}

	@Test
	void coarsensGlobalLockToEngineDescriptorChild() {
		var engineDescriptor = discover(TestCaseWithGlobalLockRequiringChild.class);

		var advisor = nodeTreeWalker.walk(engineDescriptor);

		var testClassDescriptor = getOnlyElement(engineDescriptor.getChildren());
		assertThat(advisor.getResourceLock(testClassDescriptor)).extracting(allLocks()) //
				.isEqualTo(List.of(getLock(GLOBAL_READ_WRITE)));
		assertThat(advisor.getForcedExecutionMode(testClassDescriptor)).isEmpty();

		var nestedTestClassDescriptor = getOnlyElement(testClassDescriptor.getChildren());
		assertThat(advisor.getResourceLock(nestedTestClassDescriptor)).extracting(allLocks()) //
				.isEqualTo(List.of());
		assertThat(advisor.getForcedExecutionMode(nestedTestClassDescriptor)).contains(SAME_THREAD);

		var testMethodDescriptor = getOnlyElement(nestedTestClassDescriptor.getChildren());
		assertThat(advisor.getResourceLock(testMethodDescriptor)).extracting(allLocks()) //
				.isEqualTo(List.of());
		assertThat(advisor.getForcedExecutionMode(testMethodDescriptor)).contains(SAME_THREAD);
	}

	@Test
	void putsGlobalReadLockOnFirstNodeThatRequiresIt() {
		var engineDescriptor = new EngineDescriptor(UniqueId.forEngine("dummy"), "Dummy");

		var containerWithoutBehavior = new NodeStub(engineDescriptor.getUniqueId().append("container", "1"),
			"Container 1") //
					.withGlobalReadLockRequired(false);
		var test1 = new NodeStub(containerWithoutBehavior.getUniqueId().append("test", "1"), "Test 1") //
				.withExclusiveResource(new ExclusiveResource("key1", READ_WRITE));
		containerWithoutBehavior.addChild(test1);

		var containerWithBehavior = new NodeStub(engineDescriptor.getUniqueId().append("container", "2"), "Container 2") //
				.withGlobalReadLockRequired(true);
		var test2 = new NodeStub(containerWithBehavior.getUniqueId().append("test", "2"), "Test 2") //
				.withExclusiveResource(new ExclusiveResource("key2", READ_WRITE));
		containerWithBehavior.addChild(test2);

		engineDescriptor.addChild(containerWithoutBehavior);
		engineDescriptor.addChild(containerWithBehavior);

		var advisor = nodeTreeWalker.walk(engineDescriptor);

		assertThat(advisor.getResourceLock(containerWithoutBehavior)) //
				.extracting(allLocks(), LIST) //
				.isEmpty();
		assertThat(advisor.getResourceLock(test1)) //
				.extracting(allLocks(), LIST) //
				.containsExactly(getLock(GLOBAL_READ), getReadWriteLock("key1"));

		assertThat(advisor.getResourceLock(containerWithBehavior)) //
				.extracting(allLocks(), LIST) //
				.containsExactly(getLock(GLOBAL_READ));
		assertThat(advisor.getResourceLock(test2)) //
				.extracting(allLocks(), LIST) //
				.containsExactly(getReadWriteLock("key2"));
	}

	@Test
	void doesNotAllowExclusiveResourcesWithoutRequiringGlobalReadLock() {
		var engineDescriptor = new EngineDescriptor(UniqueId.forEngine("dummy"), "Dummy");
		var invalidNode = new NodeStub(engineDescriptor.getUniqueId().append("container", "1"), "Container") //
				.withGlobalReadLockRequired(false) //
				.withExclusiveResource(new ExclusiveResource("key", READ_WRITE));
		engineDescriptor.addChild(invalidNode);

		assertPreconditionViolationFor(() -> nodeTreeWalker.walk(engineDescriptor)) //
				.withMessage("Node requiring exclusive resources must also require global read lock: " + invalidNode);
	}

	private static Function<org.junit.platform.engine.support.hierarchical.ResourceLock, List<Lock>> allLocks() {
		return ResourceLockSupport::getLocks;
	}

	private Lock getReadWriteLock(String key) {
		return getLock(new ExclusiveResource(key, READ_WRITE));
	}

	private Lock getReadLock(String key) {
		return getLock(new ExclusiveResource(key, READ));
	}

	private Lock getLock(ExclusiveResource exclusiveResource) {
		return getOnlyElement(ResourceLockSupport.getLocks(lockManager.getLockForResource(exclusiveResource)));
	}

	private TestDescriptor discover(Class<?> testClass) {
		var discoveryRequest = request().selectors(selectClass(testClass)).build();
		return new JupiterTestEngine().discover(discoveryRequest, UniqueId.forEngine("junit-jupiter"));
	}

	@ResourceLock("a")
	static class TestCaseWithResourceLock {
		@Test
		@ResourceLock("b")
		void test() {
		}
	}

	static class TestCaseWithoutResourceLock {
		@Test
		@ResourceLock("a")
		void test() {
		}

		@Nested
		@ResourceLock("c")
		class NestedTestCaseWithResourceLock {
			@Test
			@ResourceLock("b")
			void test() {
			}
		}
	}

	@ResourceLock(Resources.SYSTEM_PROPERTIES)
	static class TestCaseWithGlobalLockRequiringChild {
		@Nested
		class NestedTestCaseWithResourceLock {
			@Test
			@ResourceLock(ExclusiveResource.GLOBAL_KEY)
			void test() {
			}
		}
	}

	@ResourceLock("a")
	static class TestCaseWithResourceWriteLockOnClass {
		@Test
		void test() {
		}
	}

	@ResourceLock(value = "a", mode = ResourceAccessMode.READ)
	static class TestCaseWithResourceReadLockOnClass {
		@Test
		void test() {
		}
	}

	@ResourceLock(value = "a", mode = ResourceAccessMode.READ)
	static class TestCaseWithResourceReadLockOnClassAndWriteClockOnTestCase {
		@Test
		@ResourceLock("a")
		void test() {
		}
	}

	@ResourceLock(value = "a", mode = ResourceAccessMode.READ)
	static class TestCaseWithResourceReadLockOnClassAndReadClockOnTestCase {
		@Test
		@ResourceLock(value = "b", mode = ResourceAccessMode.READ)
		void test() {
		}
	}

	@NullMarked
	static class NodeStub extends AbstractTestDescriptor implements Node<EngineExecutionContext> {

		private final Set<ExclusiveResource> exclusiveResources = new LinkedHashSet<>();
		private boolean globalReadLockRequired = true;

		NodeStub(UniqueId uniqueId, String displayName) {
			super(uniqueId, displayName);
		}

		NodeStub withExclusiveResource(ExclusiveResource exclusiveResource) {
			exclusiveResources.add(exclusiveResource);
			return this;
		}

		NodeStub withGlobalReadLockRequired(boolean globalReadLockRequired) {
			this.globalReadLockRequired = globalReadLockRequired;
			return this;
		}

		@Override
		public boolean isGlobalReadLockRequired() {
			return globalReadLockRequired;
		}

		@Override
		public Type getType() {
			throw new UnsupportedOperationException("should not be called");
		}

		@Override
		public Set<ExclusiveResource> getExclusiveResources() {
			return exclusiveResources;
		}
	}

}
