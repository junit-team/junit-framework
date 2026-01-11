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

import static org.junit.jupiter.engine.Constants.DEFAULT_TEST_INSTANCE_LIFECYCLE_PROPERTY_NAME;

import org.junit.platform.suite.api.ConfigurationParameter;
import org.junit.platform.suite.api.SelectClasses;
import org.junit.platform.suite.api.Suite;
import org.junit.platform.suite.engine.testcases.ConfigurationSensitiveTestCase;

@Suite
@ConfigurationParameter(key = DEFAULT_TEST_INSTANCE_LIFECYCLE_PROPERTY_NAME, value = "per_class")
@SelectClasses(ConfigurationSensitiveTestCase.class)
public class ConfigurationSuite {
}
