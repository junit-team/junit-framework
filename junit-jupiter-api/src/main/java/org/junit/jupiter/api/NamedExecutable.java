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

import static org.apiguardian.api.API.Status.MAINTAINED;

import java.util.Iterator;
import java.util.stream.Stream;

import org.apiguardian.api.API;
import org.junit.jupiter.api.function.Executable;

/**
 * {@code NamedExecutable} joins {@code Executable} and {@code Named} in a
 * one self-typed functional interface.
 *
 * <p>The default implementation of {@link #getName()} returns the result of
 * calling {@link Object#toString()} on the implementing instance but may be
 * overridden by concrete implementations to provide a more meaningful name.
 *
 * <p>It is recommended to implement this interface using a record type.
 *
 * @since 5.11
 * @see DynamicTest#stream(Stream)
 * @see DynamicTest#stream(Iterator)
 */
@FunctionalInterface
@API(status = MAINTAINED, since = "5.13.3")
public interface NamedExecutable extends Named<Executable>, Executable {
	@Override
	default String getName() {
		return toString();
	}

	@Override
	default Executable getPayload() {
		return this;
	}
}
