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

import static org.hamcrest.core.IsNot.not;
import static org.hamcrest.core.StringContains.containsString;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ErrorCollector;

public class JUnit4TestCaseWithErrorCollectorStoringMultipleFailures {
	@Rule
	public ErrorCollector collector = new ErrorCollector();

	@Test
	public void example() {
		collector.addError(new Throwable("first thing went wrong"));
		collector.addError(new Throwable("second thing went wrong"));
		collector.checkThat(getResult(), not(containsString("ERROR!")));
		// all lines will run, and then a combined failure logged at the end.
	}

	private String getResult() {
		return "This is an ERROR!";
	}
}
