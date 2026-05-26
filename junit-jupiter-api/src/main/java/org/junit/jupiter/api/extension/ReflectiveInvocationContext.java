/*
 * Copyright 2015-2026 the original author or authors.
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v2.0 which
 * accompanies this distribution and is available at
 *
 * https://www.eclipse.org/legal/epl-v20.html
 */

package org.junit.jupiter.api.extension;

import static org.apiguardian.api.API.Status.STABLE;

import java.lang.reflect.Executable;
import java.util.List;
import java.util.Optional;

import org.apiguardian.api.API;

/**
 * {@code ReflectiveInvocationContext} encapsulates the <em>context</em> of
 * a reflective invocation of an executable (method or constructor).
 *
 * <p>This interface is not intended to be implemented by clients.
 *
 * @since 5.5
 */
@API(status = STABLE, since = "5.10")
public interface ReflectiveInvocationContext<T extends Executable> {

	/**
	 * Get the target class of this invocation context.
	 *
	 * <p>If this invocation context represents an instance method, this
	 * method returns the class of the object the method will be invoked on,
	 * not the class it is declared in. Otherwise, if this invocation
	 * represents a static method or constructor, this method returns the
	 * class the method or constructor is declared in.
	 *
	 * @return the target class of this invocation context; never
	 * {@code null}
	 */
	Class<?> getTargetClass();

	/**
	 * Get the method or constructor of this invocation context.
	 *
	 * @return the executable of this invocation context; never {@code null}
	 */
	T getExecutable();

	/**
	 * Get the arguments of the executable in this invocation context.
	 *
	 * @return the arguments of the executable in this invocation context;
	 * immutable and never {@code null}
	 */
	List<Object> getArguments();

	/**
	 * Get the target object of this invocation context, if available.
	 *
	 * <p>If this invocation context represents an instance method, this
	 * method returns the object the method will be invoked on. Otherwise,
	 * if this invocation context represents a static method or
	 * constructor, this method returns {@link Optional#empty() empty()}.
	 *
	 * @return the target of the executable of this invocation context; never
	 * {@code null} but potentially empty
	 */
	Optional<Object> getTarget();

}
