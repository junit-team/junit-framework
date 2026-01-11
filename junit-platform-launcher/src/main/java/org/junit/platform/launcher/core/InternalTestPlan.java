/*
 * Copyright 2015-2026 the original author or authors.
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v2.0 which
 * accompanies this distribution and is available at
 *
 * https://www.eclipse.org/legal/epl-v20.html
 */

package org.junit.platform.launcher.core;

import java.util.Optional;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Predicate;

import org.junit.platform.commons.PreconditionViolationException;
import org.junit.platform.engine.UniqueId;
import org.junit.platform.launcher.TestIdentifier;
import org.junit.platform.launcher.TestPlan;

/**
 * @since 1.4
 */
class InternalTestPlan extends TestPlan {

	private final AtomicBoolean executionStarted = new AtomicBoolean(false);
	private final LauncherDiscoveryResult discoveryResult;
	private final TestPlan delegate;

	static InternalTestPlan from(LauncherDiscoveryResult discoveryResult) {
		TestPlan delegate = TestPlan.from(discoveryResult.containsCriticalIssuesOrContainsTests(),
			discoveryResult.getEngineTestDescriptors(), discoveryResult.getConfigurationParameters(),
			discoveryResult.getOutputDirectoryCreator());
		return new InternalTestPlan(discoveryResult, delegate);
	}

	private InternalTestPlan(LauncherDiscoveryResult discoveryResult, TestPlan delegate) {
		super(delegate.containsTests(), delegate.getConfigurationParameters(), delegate.getOutputDirectoryCreator());
		this.discoveryResult = discoveryResult;
		this.delegate = delegate;
	}

	void markStarted() {
		if (!executionStarted.compareAndSet(false, true)) {
			throw new PreconditionViolationException("TestPlan must only be executed once");
		}
	}

	LauncherDiscoveryResult getDiscoveryResult() {
		return discoveryResult;
	}

	public TestPlan getDelegate() {
		return delegate;
	}

	@Override
	public void addInternal(TestIdentifier testIdentifier) {
		delegate.addInternal(testIdentifier);
	}

	@Override
	public Set<TestIdentifier> getRoots() {
		return delegate.getRoots();
	}

	@Override
	public Optional<TestIdentifier> getParent(TestIdentifier child) {
		return delegate.getParent(child);
	}

	@Override
	public Set<TestIdentifier> getChildren(TestIdentifier parent) {
		return delegate.getChildren(parent);
	}

	@Override
	public Set<TestIdentifier> getChildren(UniqueId parentId) {
		return delegate.getChildren(parentId);
	}

	@Override
	public TestIdentifier getTestIdentifier(UniqueId uniqueId) {
		return delegate.getTestIdentifier(uniqueId);
	}

	@Override
	public long countTestIdentifiers(Predicate<? super TestIdentifier> predicate) {
		return delegate.countTestIdentifiers(predicate);
	}

	@Override
	public Set<TestIdentifier> getDescendants(TestIdentifier parent) {
		return delegate.getDescendants(parent);
	}

	@Override
	public boolean containsTests() {
		return delegate.containsTests();
	}

}
