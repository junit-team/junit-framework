/*
 * Copyright 2015-2025 the original author or authors.
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v2.0 which
 * accompanies this distribution and is available at
 *
 * https://www.eclipse.org/legal/epl-v20.html
 */

package org.junit.platform.engine;

import static java.util.Arrays.asList;
import static org.apiguardian.api.API.Status.DEPRECATED;
import static org.apiguardian.api.API.Status.STABLE;
import static org.junit.platform.commons.util.CollectionUtils.getOnlyElement;
import static org.junit.platform.engine.CompositeFilter.alwaysIncluded;

import java.util.Collection;
import java.util.function.Function;
import java.util.function.Predicate;

import org.apiguardian.api.API;
import org.junit.platform.commons.util.Preconditions;

/**
 * A {@link Filter} can be applied to determine if an object should be
 * <em>included</em> or <em>excluded</em> in a result set.
 *
 * <p>For example, tests may be filtered during or after test discovery
 * based on certain criteria.
 *
 * <p>Clients should not implement this interface directly but rather one of
 * its subinterfaces.
 *
 * @since 1.0
 * @see DiscoveryFilter
 */
@FunctionalInterface
@API(status = STABLE, since = "1.0")
public interface Filter<T> {

	/**
	 * Return a filter that will include elements if and only if all of the
	 * filters in the supplied array of {@link Filter filters} include it.
	 *
	 * <p>If the array is empty, the returned filter will include all elements
	 * it is asked to filter.
	 *
	 * @param filters the array of filters to compose; never {@code null}
	 * @see #composeFilters(Collection)
	 */
	@SafeVarargs
	@SuppressWarnings("varargs")
	static <T> Filter<T> composeFilters(Filter<T>... filters) {
		Preconditions.notNull(filters, "filters array must not be null");
		Preconditions.containsNoNullElements(filters, "individual filters must not be null");

		if (filters.length == 0) {
			return alwaysIncluded();
		}
		if (filters.length == 1) {
			return filters[0];
		}
		return new CompositeFilter<>(asList(filters));
	}

	/**
	 * Return a filter that will include elements if and only if all of the
	 * filters in the supplied collection of {@link Filter filters} include it.
	 *
	 * <p>If the collection is empty, the returned filter will include all
	 * elements it is asked to filter.
	 *
	 * @param filters the collection of filters to compose; never {@code null}
	 * @see #composeFilters(Filter...)
	 */
	static <T> Filter<T> composeFilters(Collection<? extends Filter<T>> filters) {
		Preconditions.notNull(filters, "Filters must not be null");

		if (filters.isEmpty()) {
			return alwaysIncluded();
		}
		if (filters.size() == 1) {
			return getOnlyElement(filters);
		}
		return new CompositeFilter<>(filters);
	}

	/**
	 * Return a filter that will include elements if and only if the adapted
	 * {@code Filter} includes the value converted using the supplied
	 * {@link Function}.
	 *
	 * @param adaptee the filter to be adapted
	 * @param converter the converter function to apply
	 * @deprecated without replacement
	 */
	@API(status = DEPRECATED, since = "6.0")
	@Deprecated(since = "6.0", forRemoval = true)
	static <T, V> Filter<T> adaptFilter(Filter<V> adaptee, Function<T, V> converter) {
		return input -> adaptee.apply(converter.apply(input));
	}

	/**
	 * Apply this filter to the supplied object.
	 */
	FilterResult apply(T object);

	/**
	 * Return a {@link Predicate} that returns {@code true} if this filter
	 * <em>includes</em> the object supplied to the predicate's
	 * {@link Predicate#test test} method.
	 */
	default Predicate<T> toPredicate() {
		return object -> apply(object).included();
	}

}
