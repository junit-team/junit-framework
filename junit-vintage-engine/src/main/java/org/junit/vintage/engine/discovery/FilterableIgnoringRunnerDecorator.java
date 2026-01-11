/*
 * Copyright 2015-2026 the original author or authors.
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v2.0 which
 * accompanies this distribution and is available at
 *
 * https://www.eclipse.org/legal/epl-v20.html
 */

package org.junit.vintage.engine.discovery;

import org.junit.platform.commons.util.Preconditions;
import org.junit.runner.Runner;
import org.junit.runner.manipulation.Filter;
import org.junit.runner.manipulation.Filterable;
import org.junit.runner.manipulation.NoTestsRemainException;

/**
 * {@link Filterable} {@link IgnoringRunnerDecorator}.
 *
 * @since 5.1
 */
class FilterableIgnoringRunnerDecorator extends IgnoringRunnerDecorator implements Filterable {

	FilterableIgnoringRunnerDecorator(Runner runner) {
		super(runner);
		Preconditions.condition(runner instanceof Filterable,
			() -> "Runner must be an instance of Filterable: " + runner.getClass().getName());
	}

	@Override
	public void filter(Filter filter) throws NoTestsRemainException {
		((Filterable) runner).filter(filter);
	}
}
