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

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;

/**
 * @since 5.0
 */
class PackagePrivateParent {

	@BeforeAll
	static void beforeAll() {
		BridgeMethodTests.sequence.add("static parent.beforeAll()");
	}

	@AfterAll
	static void afterAll() {
		BridgeMethodTests.sequence.add("static parent.afterAll()");
	}

	@BeforeEach
	public void beforeEach() {
		BridgeMethodTests.sequence.add("parent.beforeEach()");
	}

	@AfterEach
	public void afterEach() {
		BridgeMethodTests.sequence.add("parent.afterEach()");
	}

}
