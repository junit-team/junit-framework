/*
 * Copyright 2015-2026 the original author or authors.
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v2.0 which
 * accompanies this distribution and is available at
 *
 * https://www.eclipse.org/legal/epl-v20.html
 */

package org.junit.platform.engine;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import org.junit.platform.fakes.TestDescriptorStub;

/**
 * @since 1.0
 */
class TestDescriptorTests {

	@Test
	void isRootWithoutParent() {
		TestDescriptor root = new TestDescriptorStub(UniqueId.root("root", "id"), "id");

		assertTrue(root.isRoot());
	}

	@Test
	void isRootWithParent() {
		TestDescriptor child = new TestDescriptorStub(UniqueId.root("child", "child"), "child");
		child.setParent(new TestDescriptorStub(UniqueId.root("root", "root"), "root"));

		assertFalse(child.isRoot());
	}
}
