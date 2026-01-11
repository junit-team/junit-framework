/*
 * Copyright 2015-2026 the original author or authors.
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v2.0 which
 * accompanies this distribution and is available at
 *
 * https://www.eclipse.org/legal/epl-v20.html
 */

package org.junit.vintage.engine;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

/**
 * Basic assertions regarding {@link org.junit.platform.engine.TestEngine}
 * functionality in JUnit Vintage.
 *
 * @since 4.12
 */
@SuppressWarnings("deprecation")
class VintageTestEngineBasicTests {

	private final VintageTestEngine vintage = new VintageTestEngine();

	@Test
	void id() {
		assertEquals("junit-vintage", vintage.getId());
	}

	@Test
	void groupId() {
		assertEquals("org.junit.vintage", vintage.getGroupId().get());
	}

	@Test
	void artifactId() {
		assertEquals("junit-vintage-engine", vintage.getArtifactId().get());
	}

}
