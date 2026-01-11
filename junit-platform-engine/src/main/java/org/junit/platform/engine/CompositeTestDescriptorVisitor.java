/*
 * Copyright 2015-2026 the original author or authors.
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v2.0 which
 * accompanies this distribution and is available at
 *
 * https://www.eclipse.org/legal/epl-v20.html
 */

package org.junit.platform.engine;

import java.util.Arrays;

import org.junit.platform.commons.util.Preconditions;

/**
 * @since 1.13
 */
final class CompositeTestDescriptorVisitor implements TestDescriptor.Visitor {

	private final TestDescriptor.Visitor[] visitors;

	static TestDescriptor.Visitor from(TestDescriptor.Visitor... visitors) {
		Preconditions.notNull(visitors, "visitors must not be null");
		Preconditions.notEmpty(visitors, "visitors must not be empty");
		Preconditions.containsNoNullElements(visitors, "visitors must not contain any null elements");
		return visitors.length == 1 ? visitors[0] : new CompositeTestDescriptorVisitor(visitors);
	}

	private CompositeTestDescriptorVisitor(TestDescriptor.Visitor[] visitors) {
		this.visitors = Arrays.copyOf(visitors, visitors.length);
	}

	@Override
	public void visit(TestDescriptor descriptor) {
		for (TestDescriptor.Visitor visitor : visitors) {
			visitor.visit(descriptor);
		}
	}
}
