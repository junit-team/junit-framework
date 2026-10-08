/*
 * Copyright 2026 the original author or authors.
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v2.0 which
 * accompanies this distribution and is available at
 *
 * https://www.eclipse.org/legal/epl-v20.html
 */

package example;

import org.junit.platform.configuration.api.ConfigurationParameter;
import org.junit.platform.configuration.api.ConfigurationParameter.Value;
import org.junit.platform.configuration.api.ConfigurationParameterGroup;

// tag::user_guide[]

/**
 * Collection of configuration parameters for the example project.
 */
@ConfigurationParameterGroup("org.example")
public class ConfigurationParametersDemo {

	enum ExecutionMode {

		/**
		 * Executes with a fixed number of threads.
		 */
		FIXED,

		/**
		 * Executes with a dynamic number of threads.
		 */
		DYNAMIC
	}

	/**
	 * Property name used to set the default test execution mode: {@value}.
	 * <p>
	 * Test can be executed with either a fixed number of threads, a number
	 * based on the available cores or a custom strategy.
	 */
	@ConfigurationParameter(type = ExecutionMode.class, defaultValue = @Value(stringValue = "fixed"))
	public static final String EXECUTION_MODE_PROPERTY_NAME = "org.example.execution-mode";
}
// end::user_guide[]
