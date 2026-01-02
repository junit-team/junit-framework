/*
 * Copyright 2015-2025 the original author or authors.
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v2.0 which
 * accompanies this distribution and is available at
 *
 * https://www.eclipse.org/legal/epl-v20.html
 */

package org.junit.platform.engine.support.store;

import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.EqualsAndHashCodeAssertions.assertEqualsAndHashCode;

import org.junit.jupiter.api.Test;

class NamespaceTests {

	@Test
	void namespacesEqualForSamePartsSequence() {
		Namespace ns1 = Namespace.create("part1", "part2");
		Namespace ns2 = Namespace.create("part1", "part2");
		Namespace ns3 = Namespace.create("part2", "part1");

		assertEqualsAndHashCode(ns1, ns2, ns3);
	}

	@Test
	void orderOfNamespacePartsDoesMatter() {
		Namespace ns1 = Namespace.create("part1", "part2");
		Namespace ns2 = Namespace.create("part2", "part1");

		assertNotEquals(ns1, ns2);
	}
}
