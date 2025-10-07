/*
 * Copyright 2015-2025 the original author or authors.
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v2.0 which
 * accompanies this distribution and is available at
 *
 * https://www.eclipse.org/legal/epl-v20.html
 */

package org.junit.platform.engine;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.platform.commons.test.PreconditionAssertions.assertPreconditionViolationFor;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;

import org.junit.jupiter.api.Test;
import org.junit.platform.engine.TestDescriptor.Visitor;
import org.mockito.InOrder;

class CompositeTestDescriptorVisitorTests {

	@SuppressWarnings("DataFlowIssue")
	@Test
	void checksPreconditions() {
		assertPreconditionViolationFor(Visitor::composite);
		assertPreconditionViolationFor(() -> Visitor.composite((Visitor[]) null));
		assertPreconditionViolationFor(() -> Visitor.composite((Visitor) null));
	}

	@Test
	void optimizesForSingleVisitor() {
		Visitor visitor = mock();

		assertSame(visitor, Visitor.composite(visitor));
	}

	@Test
	void callsAllVisitorsInOrder() {
		Visitor visitor1 = mock("visitor1");
		Visitor visitor2 = mock("visitor2");
		TestDescriptor testDescriptor = mock();

		var composite = Visitor.composite(visitor1, visitor2);
		composite.visit(testDescriptor);

		InOrder inOrder = inOrder(visitor1, visitor2);
		inOrder.verify(visitor1).visit(testDescriptor);
		inOrder.verify(visitor2).visit(testDescriptor);
		inOrder.verifyNoMoreInteractions();
	}

}
