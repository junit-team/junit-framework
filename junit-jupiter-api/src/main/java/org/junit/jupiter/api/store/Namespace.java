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

import static org.apiguardian.api.API.Status.INTERNAL;
import static org.apiguardian.api.API.Status.MAINTAINED;

import java.util.List;

import org.apiguardian.api.API;

/**
 * A {@code Namespace} is used to provide a <em>scope</em> for data saved
 * within a {@link Store}.
 *
 * <p>Storing data in custom namespaces allows avoiding accidentally mixing
 * data between different users of a store.
 *
 * <p>Two namespaces are equal if and only if their {@linkplain #getParts()
 * parts} are equal, regardless of their concrete implementation class.
 *
 * @since 6.2
 * @see org.junit.jupiter.api.extension.ExtensionContext.Namespace
 */
@API(status = MAINTAINED, since = "6.2")
public interface Namespace {

	/**
	 * The default, global namespace which allows access to stored data from
	 * all users of a store.
	 */
	Namespace GLOBAL = create(new Object());

	/**
	 * Create a namespace which restricts access to data to all users which
	 * use the same sequence of {@code parts} for creating a namespace.
	 *
	 * <p>The order of the {@code parts} is significant.
	 *
	 * <p>Internally the {@code parts} are compared using {@link Object#equals(Object)}.
	 */
	static Namespace create(Object... parts) {
		return DefaultNamespace.create(parts);
	}

	/**
	 * Create a new namespace by appending the supplied {@code parts} to the
	 * existing sequence of parts in this namespace.
	 *
	 * @return new namespace; never {@code null}
	 */
	Namespace append(Object... parts);

	/**
	 * {@return the parts of this namespace}
	 */
	@API(status = INTERNAL, since = "6.2")
	List<Object> getParts();

}
