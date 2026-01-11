/*
 * Copyright 2015-2026 the original author or authors.
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v2.0 which
 * accompanies this distribution and is available at
 *
 * https://www.eclipse.org/legal/epl-v20.html
 */

package org.junit.platform.launcher.tagexpression;

import java.util.ArrayDeque;
import java.util.Deque;

import org.junit.platform.commons.util.Preconditions;

/**
 * @since 1.1
 */
class DequeStack<T> implements Stack<T> {

	private final Deque<T> deque = new ArrayDeque<>();

	@Override
	public void push(T t) {
		deque.addFirst(t);
	}

	@Override
	public T peek() {
		return Preconditions.notNull(deque.peek(), () -> "stack is empty");
	}

	@Override
	public T pop() {
		return Preconditions.notNull(deque.pollFirst(), () -> "stack is empty");
	}

	@Override
	public boolean isEmpty() {
		return deque.isEmpty();
	}

	@Override
	public int size() {
		return deque.size();
	}

}
