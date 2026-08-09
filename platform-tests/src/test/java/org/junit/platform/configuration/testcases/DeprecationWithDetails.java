/*
 * Copyright 2026 the original author or authors.
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v2.0 which
 * accompanies this distribution and is available at
 *
 * https://www.eclipse.org/legal/epl-v20.html
 */

package org.junit.platform.configuration.testcases;

import org.junit.platform.configuration.api.ConfigurationParameter;

public final class DeprecationWithDetails {

	@ConfigurationParameter(deprecation = @ConfigurationParameter.Deprecation(reason = "This property was migrated to com.example", replacement = "com.example.property", since = "2.0.0"))
	public static final String EXAMPLE_PROPERTY_NAME = "org.example.property";

}
