/*
 * Copyright 2015-2025 the original author or authors.
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v2.0 which
 * accompanies this distribution and is available at
 *
 * https://www.eclipse.org/legal/epl-v20.html
 */

package org.junit.jupiter.testkit.assertion;

import org.junit.jupiter.testkit.assertion.reportentry.ReportEntryAssert;
import org.junit.jupiter.testkit.assertion.single.TestCaseAssert;
import org.junit.jupiter.testkit.assertion.suite.TestSuiteAssert;

/**
 * Base interface for all {@link org.junit.jupiter.testkit.ExecutionResults} assertions.
 */
public interface ExecutionResultAssert extends TestSuiteAssert, TestCaseAssert, ReportEntryAssert {
}
