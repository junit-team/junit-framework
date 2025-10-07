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

import static org.junit.platform.commons.test.PreconditionAssertions.assertPreconditionViolationNotNullFor;
import static org.junit.platform.commons.test.PreconditionAssertions.assertPreconditionViolationNotNullOrEmptyFor;

import java.util.List;

import org.junit.jupiter.api.Test;

public class ListenerRegistryTests {

	@SuppressWarnings("DataFlowIssue")
	@Test
	void registerWithNullArray() {
		var registry = ListenerRegistry.create(List::getFirst);

		assertPreconditionViolationNotNullOrEmptyFor("listeners array", () -> registry.addAll((Object[]) null));
	}

	@Test
	void registerWithEmptyArray() {
		var registry = ListenerRegistry.create(List::getFirst);

		assertPreconditionViolationNotNullOrEmptyFor("listeners array", registry::addAll);
	}

	@SuppressWarnings("DataFlowIssue")
	@Test
	void registerWithArrayContainingNullElements() {
		var registry = ListenerRegistry.create(List::getFirst);

		assertPreconditionViolationNotNullFor("individual listeners", () -> registry.addAll(new Object[] { null }));
	}
}
