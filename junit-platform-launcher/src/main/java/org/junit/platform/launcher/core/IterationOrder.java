/*
 * Copyright 2015-2026 the original author or authors.
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v2.0 which
 * accompanies this distribution and is available at
 *
 * https://www.eclipse.org/legal/epl-v20.html
 */

package org.junit.platform.launcher.core;

import static org.junit.platform.commons.util.CollectionUtils.forEachInReverseOrder;

import java.util.List;
import java.util.function.Consumer;

enum IterationOrder {

	ORIGINAL {
		@Override
		<T> void forEach(List<T> listeners, Consumer<T> action) {
			listeners.forEach(action);
		}
	},

	REVERSED {
		@Override
		<T> void forEach(List<T> listeners, Consumer<T> action) {
			forEachInReverseOrder(listeners, action);
		}
	};

	abstract <T> void forEach(List<T> listeners, Consumer<T> action);
}
