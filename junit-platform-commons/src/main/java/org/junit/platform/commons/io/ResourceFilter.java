/*
 * Copyright 2015-2026 the original author or authors.
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
import org.jspecify.annotations.Nullable;
import org.junit.platform.commons.PreconditionViolationException;
import org.junit.platform.commons.annotation.Contract;

/**
 * Resource filter used by reflection and classpath scanning support.
 *
 * @since 1.14
 * @see Resource
 */
@API(status = MAINTAINED, since = "1.14")
public class ResourceFilter {

	/**
	 * Create a {@link ResourceFilter} instance from a predicate.
	 *
	 * @param resourcePredicate the resource predicate; never {@code null}
	 * @return an instance of {@code ResourceFilter}; never {@code null}
	 */
	public static ResourceFilter of(Predicate<? super Resource> resourcePredicate) {
		return new ResourceFilter(checkNotNull(resourcePredicate, "resourcePredicate"));
	}

	private final Predicate<? super Resource> predicate;

	private ResourceFilter(Predicate<? super Resource> predicate) {
		this.predicate = predicate;
	}

	/**
	 * Test whether the given resource matches this filter.
	 *
	 * @param resource the resource to test; never {@code null}
	 * @return {@code true} if the resource matches this filter, otherwise
	 * {@code false}
	 */
	public boolean match(Resource resource) {
		return predicate.test(checkNotNull(resource, "resource"));
	}

	// Cannot use Preconditions due to package cycle
	@Contract("null, _ -> fail; !null, _ -> param1")
	private static <T> T checkNotNull(@Nullable T input, String title) {
		if (input == null) {
			throw new PreconditionViolationException(title + " must not be null");
		}
		return input;
	}

}
