/*
 * Copyright 2015-2026 the original author or authors.
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v2.0 which
 * accompanies this distribution and is available at
 *
 * https://www.eclipse.org/legal/epl-v20.html
 */

package org.junit.platform.engine.support.store;

import static org.apiguardian.api.API.Status.EXPERIMENTAL;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.apiguardian.api.API;
import org.junit.platform.commons.util.Preconditions;

/**
 * A {@code Namespace} is used to provide a <em>scope</em> for data saved by
 * extensions within a {@link NamespacedHierarchicalStore}.
 *
 * <p>Storing data in custom namespaces allows extensions to avoid accidentally
 * mixing data between extensions or across different invocations within the
 * lifecycle of a single extension.
 */
@API(status = EXPERIMENTAL, since = "6.0")
public final class Namespace {

	/**
	 * The default, global namespace which allows access to stored data from
	 * all extensions.
	 */
	public static final Namespace GLOBAL = Namespace.create(new Object());

	/**
	 * Create a namespace which restricts access to data to all extensions
	 * which use the same sequence of {@code parts} for creating a namespace.
	 *
	 * <p>The order of the {@code parts} is significant.
	 *
	 * <p>Internally the {@code parts} are compared using {@link Object#equals(Object)}.
	 */
	public static Namespace create(Object... parts) {
		Preconditions.notEmpty(parts, "parts array must not be null or empty");
		Preconditions.containsNoNullElements(parts, "individual parts must not be null");
		return new Namespace(List.of(parts));
	}

	/**
	 * Create a namespace which restricts access to data to all extensions
	 * which use the same sequence of {@code objects} for creating a namespace.
	 *
	 * <p>The order of the {@code objects} is significant.
	 *
	 * <p>Internally the {@code objects} are compared using {@link Object#equals(Object)}.
	 */
	public static Namespace create(List<Object> objects) {
		Preconditions.notEmpty(objects, "objects list must not be null or empty");
		Preconditions.containsNoNullElements(objects, "individual objects must not be null");
		return new Namespace(objects);
	}

	private final List<Object> parts;

	private Namespace(List<Object> parts) {
		this.parts = List.copyOf(parts);
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) {
			return true;
		}
		if (o == null || getClass() != o.getClass()) {
			return false;
		}
		Namespace that = (Namespace) o;
		return this.parts.equals(that.parts);
	}

	@Override
	public int hashCode() {
		return this.parts.hashCode();
	}

	/**
	 * Create a new namespace by appending the supplied {@code parts} to the
	 * existing sequence of parts in this namespace.
	 *
	 * @return new namespace; never {@code null}
	 */
	public Namespace append(Object... parts) {
		Preconditions.notEmpty(parts, "parts array must not be null or empty");
		Preconditions.containsNoNullElements(parts, "individual parts must not be null");
		ArrayList<Object> newParts = new ArrayList<>(this.parts.size() + parts.length);
		newParts.addAll(this.parts);
		Collections.addAll(newParts, parts);
		return new Namespace(newParts);
	}
}
