/*
 * Copyright 2015-2026 the original author or authors.
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v2.0 which
 * accompanies this distribution and is available at
 *
 * https://www.eclipse.org/legal/epl-v20.html
 */

package org.junit.vintage.engine.samples.junit4;

import org.junit.runner.Description;
import org.junit.runner.manipulation.Filter;
import org.junit.runner.manipulation.Filterable;
import org.junit.runner.manipulation.NoTestsRemainException;

public class DynamicRunner extends ConfigurableRunner implements Filterable {

	public DynamicRunner(Class<?> testClass) {
		super(testClass);
	}

	@Override
	public Description getDescription() {
		return Description.createSuiteDescription(testClass);
	}

	@Override
	public void filter(Filter filter) throws NoTestsRemainException {
		filteredChildren.removeIf(each -> !filter.shouldRun(each));
		if (filteredChildren.isEmpty()) {
			throw new NoTestsRemainException();
		}
	}
}
