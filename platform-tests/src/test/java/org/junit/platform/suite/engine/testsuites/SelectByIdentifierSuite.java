/*
 * Copyright 2015-2026 the original author or authors.
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v2.0 which
 * accompanies this distribution and is available at
 *
 * https://www.eclipse.org/legal/epl-v20.html
 */

package org.junit.platform.suite.engine.testsuites;

import org.junit.platform.suite.api.IncludeClassNamePatterns;
import org.junit.platform.suite.api.Select;
import org.junit.platform.suite.api.Suite;

/**
 * @since 1.11
 */
@Suite
@IncludeClassNamePatterns(".*")
@Select("class:org.junit.platform.suite.engine.testcases.SingleTestTestCase")
public class SelectByIdentifierSuite {
}
