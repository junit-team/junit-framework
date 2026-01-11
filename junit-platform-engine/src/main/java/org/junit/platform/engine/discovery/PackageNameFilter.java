/*
 * Copyright 2015-2026 the original author or authors.
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v2.0 which
 * accompanies this distribution and is available at
 *
 * https://www.eclipse.org/legal/epl-v20.html
 */

package org.junit.platform.engine.discovery;

import static org.apiguardian.api.API.Status.STABLE;

import java.util.List;

import org.apiguardian.api.API;
import org.junit.platform.engine.DiscoveryFilter;

/**
 * {@link DiscoveryFilter} that is applied to the name of a {@link Package}.
 *
 * @since 1.0
 * @see #includePackageNames(String...)
 * @see #excludePackageNames(String...)
 * @see ClassNameFilter
 */
@API(status = STABLE, since = "1.0")
public interface PackageNameFilter extends DiscoveryFilter<String> {

	/**
	 * Create a new <em>include</em> {@link PackageNameFilter} based on the
	 * supplied package names.
	 *
	 * <p>The names are combined using OR semantics, i.e. if the fully
	 * qualified name of a package starts with at least one of the names,
	 * the package will be included in the result set.
	 *
	 * @param names package names that we be compared against fully qualified
	 * package names; never {@code null}, empty, or containing {@code null}
	 * @see Package#getName()
	 * @see #includePackageNames(List)
	 * @see #excludePackageNames(String...)
	 */
	static PackageNameFilter includePackageNames(String... names) {
		return new IncludePackageNameFilter(names);
	}

	/**
	 * Create a new <em>include</em> {@link PackageNameFilter} based on the
	 * supplied package names.
	 *
	 * <p>The names are combined using OR semantics, i.e. if the fully
	 * qualified name of a package starts with at least one of the names,
	 * the package will be included in the result set.
	 *
	 * @param names package names that we be compared against fully qualified
	 * package names; never {@code null}, empty, or containing {@code null}
	 * @see Package#getName()
	 * @see #includePackageNames(String...)
	 * @see #excludePackageNames(String...)
	 */
	static PackageNameFilter includePackageNames(List<String> names) {
		return includePackageNames(names.toArray(new String[0]));
	}

	/**
	 * Create a new <em>exclude</em> {@link PackageNameFilter} based on the
	 * supplied package names.
	 *
	 * <p>The names are combined using OR semantics, i.e. if the fully
	 * qualified name of a package starts with at least one of the names,
	 * the package will be excluded in the result set.
	 *
	 * @param names package names that we be compared against fully qualified
	 * package names; never {@code null}, empty, or containing {@code null}
	 * @see Package#getName()
	 * @see #excludePackageNames(List)
	 * @see #includePackageNames(String...)
	 */
	static PackageNameFilter excludePackageNames(String... names) {
		return new ExcludePackageNameFilter(names);
	}

	/**
	 * Create a new <em>exclude</em> {@link PackageNameFilter} based on the
	 * supplied package names.
	 *
	 * <p>The names are combined using OR semantics, i.e. if the fully
	 * qualified name of a package starts with at least one of the names,
	 * the package will be excluded in the result set.
	 *
	 * @param names package names that we be compared against fully qualified
	 * package names; never {@code null}, empty, or containing {@code null}
	 * @see Package#getName()
	 * @see #excludePackageNames(String...)
	 * @see #includePackageNames(String...)
	 */
	static PackageNameFilter excludePackageNames(List<String> names) {
		return excludePackageNames(names.toArray(new String[0]));
	}

}
