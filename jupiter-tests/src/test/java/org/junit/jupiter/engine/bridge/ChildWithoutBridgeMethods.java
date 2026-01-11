/*
 * Copyright 2015-2026 the original author or authors.
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v2.0 which
 * accompanies this distribution and is available at
 *
 * https://www.eclipse.org/legal/epl-v20.html
 */

package org.junit.jupiter.engine.bridge;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * @since 5.0
 */
// modifier "public" is *not* present for not creating bridge methods by the compiler
class ChildWithoutBridgeMethods extends PackagePrivateParent {

	@BeforeEach
	public void anotherBeforeEach() {
		BridgeMethodTests.sequence.add("child.anotherBeforeEach()");
	}

	@Test
	void test() {
		BridgeMethodTests.sequence.add("child.test()");
	}

	@AfterEach
	public void anotherAfterEach() {
		BridgeMethodTests.sequence.add("child.anotherAfterEach()");
	}

}
