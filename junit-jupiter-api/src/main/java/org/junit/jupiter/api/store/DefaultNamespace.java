/*
 * Copyright 2015-2026 the original author or authors.
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v2.0 which
 * accompanies this distribution and is available at
 *
 * https://www.eclipse.org/legal/epl-v20.html
 */

package org.junit.jupiter.api.store;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.junit.platform.commons.util.Preconditions;

/**
 * @since 6.2
 */
final class DefaultNamespace implements Namespace {

	static DefaultNamespace create(Object... parts) {
		Preconditions.notEmpty(parts, "parts array must not be null or empty");
		Preconditions.containsNoNullElements(parts, "individual parts must not be null");
		return new DefaultNamespace(List.of(parts));
	}

	private final List<Object> parts;

	private DefaultNamespace(List<Object> parts) {
		this.parts = List.copyOf(parts);
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) {
			return true;
		}
		return o instanceof Namespace that && this.parts.equals(that.getParts());
	}

	@Override
	public int hashCode() {
		return this.parts.hashCode();
	}

	@Override
	public DefaultNamespace append(Object... parts) {
		Preconditions.notEmpty(parts, "parts array must not be null or empty");
		Preconditions.containsNoNullElements(parts, "individual parts must not be null");
		ArrayList<Object> newParts = new ArrayList<>(this.parts.size() + parts.length);
		newParts.addAll(this.parts);
		Collections.addAll(newParts, parts);
		return new DefaultNamespace(newParts);
	}

	@Override
	public List<Object> getParts() {
		return parts;
	}

}
