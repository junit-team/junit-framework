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

import java.util.List;

import org.junit.jupiter.api.Test;

public class ListenerRegistryTests {

	@SuppressWarnings("DataFlowIssue")
	@Test
	void registerWithNullArray() {
		var registry = ListenerRegistry.create(List::getFirst);

		assertPreconditionViolationFor(() -> registry.addAll((Object[]) null)).withMessageContaining(
			"listeners array must not be null or empty");
	}

	@Test
	void registerWithEmptyArray() {
		var registry = ListenerRegistry.create(List::getFirst);

		assertPreconditionViolationFor(registry::addAll).withMessageContaining(
			"listeners array must not be null or empty");
	}

	@SuppressWarnings("DataFlowIssue")
	@Test
	void registerWithArrayContainingNullElements() {
		var registry = ListenerRegistry.create(List::getFirst);

		assertPreconditionViolationFor(() -> registry.addAll(new Object[] { null })).withMessageContaining(
			"individual listeners must not be null");
	}
}
