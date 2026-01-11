/*
 * Copyright 2015-2026 the original author or authors.
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v2.0 which
 * accompanies this distribution and is available at
 *
 * https://www.eclipse.org/legal/epl-v20.html
 */

package org.junit.platform.engine.discovery;

import static org.junit.jupiter.api.EqualsAndHashCodeAssertions.assertEqualsAndHashCode;

import org.junit.jupiter.api.Test;

/**
 * Unit tests for {@link DirectorySelector}.
 *
 * @since 1.3
 * @see DiscoverySelectorsTests
 */
class DirectorySelectorTests {

	@Test
	void equalsAndHashCode() {
		var selector1 = new DirectorySelector("/example/foo");
		var selector2 = new DirectorySelector("/example/foo");
		var selector3 = new DirectorySelector("/example/bar");

		assertEqualsAndHashCode(selector1, selector2, selector3);
	}

}
