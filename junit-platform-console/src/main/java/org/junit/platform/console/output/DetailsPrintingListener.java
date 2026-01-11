/*
 * Copyright 2015-2026 the original author or authors.
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v2.0 which
 * accompanies this distribution and is available at
 *
 * https://www.eclipse.org/legal/epl-v20.html
 */

package org.junit.platform.console.output;

import static org.apiguardian.api.API.Status.INTERNAL;

import java.util.regex.Pattern;

import org.apiguardian.api.API;
import org.junit.platform.launcher.TestExecutionListener;
import org.junit.platform.launcher.TestPlan;

/**
 * @since 1.0
 */
@API(status = INTERNAL, since = "1.14")
public interface DetailsPrintingListener extends TestExecutionListener {

	Pattern LINE_START_PATTERN = Pattern.compile("(?m)^");

	void listTests(TestPlan testPlan);

	static String indented(String message, String indentation) {
		return LINE_START_PATTERN.matcher(message).replaceAll(indentation).strip();
	}
}
