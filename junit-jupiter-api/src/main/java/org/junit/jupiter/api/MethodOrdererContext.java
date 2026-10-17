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

import java.util.List;

import org.apiguardian.api.API;

/**
 * {@code MethodOrdererContext} encapsulates the <em>context</em> in which
 * a {@link MethodOrderer} will be invoked.
 *
 * @since 5.4
 * @see MethodOrderer
 * @see MethodDescriptor
 */
@API(status = STABLE, since = "5.7")
public interface MethodOrdererContext extends DiscoveryContext {

	/**
	 * Get the test class for this context.
	 *
	 * @return the test class; never {@code null}
	 */
	Class<?> getTestClass();

	/**
	 * Get the list of {@linkplain MethodDescriptor method descriptors} to
	 * order.
	 *
	 * @return the list of method descriptors; never {@code null}
	 */
	List<? extends MethodDescriptor> getMethodDescriptors();

}
