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
import org.junit.platform.configuration.api.ConfigurationParameter.Hint;
import org.junit.platform.configuration.api.ConfigurationParameter.Hints;

public final class HintsWithPermitsAdditionalValues {

	@ConfigurationParameter(hints = @Hints(permitsAdditionalValues = true, value = @Hint(value = "42 ns")))
	public static final String EXAMPLE_PROPERTY_NAME = "org.example.property";

}
