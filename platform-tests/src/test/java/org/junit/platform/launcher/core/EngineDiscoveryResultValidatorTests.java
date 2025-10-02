/*
 * Copyright 2015-2025 the original author or authors.
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v2.0 which
 * accompanies this distribution and is available at
 *
 * https://www.eclipse.org/legal/epl-v20.html
 */

package org.junit.platform.launcher.core;

import static org.junit.platform.commons.test.PreconditionAssertions.assertPreconditionViolationFor;

import org.junit.jupiter.api.Test;
import org.junit.platform.engine.TestDescriptor;
import org.junit.platform.engine.TestEngine;
import org.junit.platform.engine.UniqueId;
import org.junit.platform.fakes.TestDescriptorStub;
import org.junit.platform.fakes.TestEngineStub;

/**
 * @since 1.3
 */
class EngineDiscoveryResultValidatorTests {

	private final TestEngine testEngine = new TestEngineStub("my-engine");
	private final EngineDiscoveryResultValidator validator = new EngineDiscoveryResultValidator();

	@Test
	void detectCycleWithDoubleRoot() {
		var root = new TestDescriptorStub(UniqueId.forEngine("root"), "root");
		validator.validate(testEngine, root);

		root.addChild(root);
		assertPreconditionViolationFor(() -> validator.validate(testEngine, root)).withMessage("""
				The discover() method for TestEngine with ID 'my-engine' returned a cyclic graph; \
				[engine:root] exists in at least two paths:
				(1) [engine:root]
				(2) [engine:root] -> [engine:root]""");
	}

	@Test
	void detectCycleWithDoubleGroup() {
		var rootId = UniqueId.forEngine("root");
		var root = new TestDescriptorStub(rootId, "root");
		TestDescriptor group1 = new TestDescriptorStub(rootId.append("group", "1"), "1");
		TestDescriptor group2 = new TestDescriptorStub(rootId.append("group", "2"), "2");
		root.addChild(group1);
		root.addChild(group2);
		validator.validate(testEngine, root);

		group2.addChild(group1);
		assertPreconditionViolationFor(() -> validator.validate(testEngine, root)).withMessage("""
				The discover() method for TestEngine with ID 'my-engine' returned a cyclic graph; \
				[engine:root]/[group:1] exists in at least two paths:
				(1) [engine:root] -> [engine:root]/[group:1]
				(2) [engine:root] -> [engine:root]/[group:2] -> [engine:root]/[group:1]""");
	}

	@Test
	void detectCycleWithDoubleTest() {
		var rootId = UniqueId.forEngine("root");
		var root = new TestDescriptorStub(rootId, "root");
		TestDescriptor group1 = new TestDescriptorStub(rootId.append("group", "1"), "1");
		TestDescriptor group2 = new TestDescriptorStub(rootId.append("group", "2"), "2");
		root.addChild(group1);
		root.addChild(group2);
		TestDescriptor test1 = new TestDescriptorStub(group1.getUniqueId().append("test", "1"), "1-1");
		TestDescriptor test2 = new TestDescriptorStub(group2.getUniqueId().append("test", "2"), "2-2");
		group1.addChild(test1);
		group2.addChild(test2);
		validator.validate(testEngine, root);

		group2.addChild(test1);
		assertPreconditionViolationFor(() -> validator.validate(testEngine, root)).withMessage("""
				The discover() method for TestEngine with ID 'my-engine' returned a cyclic graph; \
				[engine:root]/[group:1]/[test:1] exists in at least two paths:
				(1) [engine:root] -> [engine:root]/[group:1] -> [engine:root]/[group:1]/[test:1]
				(2) [engine:root] -> [engine:root]/[group:2] -> [engine:root]/[group:1]/[test:1]""");
	}

}
