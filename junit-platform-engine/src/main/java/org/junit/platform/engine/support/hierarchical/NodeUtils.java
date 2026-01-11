/*
 * Copyright 2015-2026 the original author or authors.
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v2.0 which
 * accompanies this distribution and is available at
 *
 * https://www.eclipse.org/legal/epl-v20.html
 */

package org.junit.platform.engine.support.hierarchical;

import org.junit.platform.engine.TestDescriptor;

/**
 * @since 1.3.1
 */
final class NodeUtils {

	private NodeUtils() {
		/* no-op */
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	static <C extends EngineExecutionContext> Node<C> asNode(TestDescriptor testDescriptor) {
		return (testDescriptor instanceof Node node ? node : noOpNode);
	}

	@SuppressWarnings("rawtypes")
	private static final Node noOpNode = new Node() {
	};

}
