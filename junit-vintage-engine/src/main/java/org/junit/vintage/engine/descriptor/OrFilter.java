/*
 * Copyright 2015-2026 the original author or authors.
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v2.0 which
 * accompanies this distribution and is available at
 *
 * https://www.eclipse.org/legal/epl-v20.html
 */

package org.junit.vintage.engine.descriptor;

import static java.util.stream.Collectors.joining;

import java.util.Collection;

import org.junit.platform.commons.util.Preconditions;
import org.junit.runner.Description;
import org.junit.runner.manipulation.Filter;

/**
 * @since 5.4
 */
class OrFilter extends Filter {

	private final Collection<? extends Filter> filters;

	OrFilter(Collection<? extends Filter> filters) {
		this.filters = Preconditions.notEmpty(filters, "filters must not be empty");
	}

	@Override
	public boolean shouldRun(Description description) {
		return filters.stream().anyMatch(filter -> filter.shouldRun(description));
	}

	@Override
	public String describe() {
		return filters.stream().map(Filter::describe).collect(joining(" OR "));
	}

}
