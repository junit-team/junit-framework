/*
 * Copyright 2015-2025 the original author or authors.
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v2.0 which
 * accompanies this distribution and is available at
 *
 * https://www.eclipse.org/legal/epl-v20.html
 */

package org.junit.platform.commons.io;

import static org.apiguardian.api.API.Status.MAINTAINED;

import java.util.function.Predicate;

import org.apiguardian.api.API;

/**
 * Resource filter used by reflection and classpath scanning support.
 *
 * @since 6.0
 * @see Resource
 */
@API(status = MAINTAINED, since = "6.0")
public class ResourceFilter {

	/**
	 * Create a {@link ResourceFilter} instance from a predicate.
	 *
	 * @param resourcePredicate the resource predicate; never {@code null}
	 * @return an instance of {@code ResourceFilter}; never {@code null}
	 */
	public static ResourceFilter of(Predicate<? super Resource> resourcePredicate) {
		return new ResourceFilter(resourcePredicate);
	}

	private final Predicate<? super Resource> predicate;

	private ResourceFilter(Predicate<? super Resource> predicate) {
		this.predicate = predicate;
	}

	public boolean match(Resource resource) {
		return predicate.test(resource);
	}

}
