/*
 * Copyright 2015-2026 the original author or authors.
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v2.0 which
 * accompanies this distribution and is available at
 *
 * https://www.eclipse.org/legal/epl-v20.html
 */

package org.junit.jupiter.api;

import static org.apiguardian.api.API.Status.STABLE;

import org.apiguardian.api.API;
import org.jspecify.annotations.Nullable;
import org.junit.platform.commons.util.Preconditions;

/**
 * {@code Named} is a container that associates a name with a given payload.
 *
 * @param <T> the type of the payload
 *
 * @since 5.8
 */
@API(status = STABLE, since = "5.8")
public interface Named<T extends @Nullable Object> {

	/**
	 * Factory method for creating an instance of {@code Named} based on a
	 * {@code name} and a {@code payload}.
	 *
	 * @param name the name associated with the payload; never {@code null} or
	 * blank
	 * @param payload the object that serves as the payload; may be {@code null}
	 * depending on the use case
	 * @param <T> the type of the payload
	 * @return an instance of {@code Named}; never {@code null}
	 * @see #named(String, java.lang.Object)
	 */
	static <T extends @Nullable Object> Named<T> of(String name, T payload) {
		Preconditions.notBlank(name, "name must not be null or blank");

		return new Named<>() {
			@Override
			public String getName() {
				return name;
			}

			@Override
			public T getPayload() {
				return payload;
			}

			@Override
			public String toString() {
				return name;
			}
		};
	}

	/**
	 * Factory method for creating an instance of {@code Named} based on a
	 * {@code name} and a {@code payload}.
	 *
	 * <p>This method is an <em>alias</em> for {@link Named#of} and is
	 * intended to be used when statically imported &mdash; for example, via:
	 * {@code import static org.junit.jupiter.api.Named.named;}
	 *
	 * @param name the name associated with the payload; never {@code null} or
	 * blank
	 * @param payload the object that serves as the payload; may be {@code null}
	 * depending on the use case
	 * @param <T> the type of the payload
	 * @return an instance of {@code Named}; never {@code null}
	 */
	static <T extends @Nullable Object> Named<T> named(String name, T payload) {
		return of(name, payload);
	}

	/**
	 * Get the name of the payload.
	 *
	 * @return the name of the payload; never {@code null} or blank
	 */
	String getName();

	/**
	 * Get the payload.
	 *
	 * @return the payload; may be {@code null} depending on the use case
	 */
	T getPayload();

}
