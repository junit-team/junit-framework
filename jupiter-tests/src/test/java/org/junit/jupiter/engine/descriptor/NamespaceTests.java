/*
 * Copyright 2015-2025 the original author or authors.
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v2.0 which
 * accompanies this distribution and is available at
 *
 * https://www.eclipse.org/legal/epl-v20.html
 */

package org.junit.jupiter.engine.descriptor;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtensionContext.Namespace;

class NamespaceTests {

	@Test
	void namespacesEqualForSamePartsSequence() {
		Namespace ns1 = Namespace.create("part1", "part2");
		Namespace ns2 = Namespace.create("part1", "part2");

		assertEquals(ns1, ns2);
	}

	@Test
	void orderOfNamespacePartsDoesMatter() {
		Namespace ns1 = Namespace.create("part1", "part2");
		Namespace ns2 = Namespace.create("part2", "part1");

		assertNotEquals(ns1, ns2);
	}
}
