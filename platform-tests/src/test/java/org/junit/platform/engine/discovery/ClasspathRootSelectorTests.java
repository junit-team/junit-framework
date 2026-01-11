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

import java.net.URI;

import org.junit.jupiter.api.Test;

/**
 * Unit tests for {@link ClasspathRootSelector}.
 *
 * @since 1.3
 * @see DiscoverySelectorsTests
 */
class ClasspathRootSelectorTests {

	@Test
	void equalsAndHashCode() throws Exception {
		var selector1 = new ClasspathRootSelector(new URI("file://example/path"));
		var selector2 = new ClasspathRootSelector(new URI("file://example/path"));
		var selector3 = new ClasspathRootSelector(new URI("file://example/foo"));

		assertEqualsAndHashCode(selector1, selector2, selector3);
	}

}
