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
import org.junit.platform.engine.UniqueId;

/**
 * Unit tests for {@link UniqueIdSelector}.
 *
 * @since 1.3
 * @see DiscoverySelectorsTests
 */
class UniqueIdSelectorTests {

	@Test
	void equalsAndHashCode() {
		var testEngine = UniqueId.forEngine("test-engine");
		var selector1 = new UniqueIdSelector(testEngine.append("test-class", "org.example.TestClass"));
		var selector2 = new UniqueIdSelector(testEngine.append("test-class", "org.example.TestClass"));
		var selector3 = new UniqueIdSelector(testEngine.append("test-class", "org.example.FooBar"));

		assertEqualsAndHashCode(selector1, selector2, selector3);
	}

}
