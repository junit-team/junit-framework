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
import org.junit.platform.configuration.api.ConfigurationParameter.Value;

public final class Defaults {

	@ConfigurationParameter(defaultValue = @Value(shorts = 1))
	public static final String SHORTS_PROPERTY_NAME = "org.example.shorts";

	@ConfigurationParameter(defaultValue = @Value(bytes = 0x2A))
	public static final String BYTES_PROPERTY_NAME = "org.example.bytes";

	@ConfigurationParameter(defaultValue = @Value(ints = 42))
	public static final String INTS_PROPERTY_NAME = "org.example.ints";

	@ConfigurationParameter(defaultValue = @Value(longs = 42))
	public static final String LONGS_PROPERTY_NAME = "org.example.longs";

	@ConfigurationParameter(defaultValue = @Value(floats = 42.0f))
	public static final String FLOATS_PROPERTY_NAME = "org.example.floats";

	@ConfigurationParameter(defaultValue = @Value(doubles = 42.0))
	public static final String DOUBLES_PROPERTY_NAME = "org.example.doubles";

	@ConfigurationParameter(defaultValue = @Value(chars = '4'))
	public static final String CHARS_PROPERTY_NAME = "org.example.chars";

	@ConfigurationParameter(defaultValue = @Value(booleans = true))
	public static final String BOOLEANS_PROPERTY_NAME = "org.example.booleans";

	@ConfigurationParameter(defaultValue = @Value(strings = "default"))
	public static final String STRING_PROPERTY_NAME = "org.example.strings";

	@ConfigurationParameter(defaultValue = @Value(classes = Example.class))
	public static final String CLASSES_PROPERTY_NAME = "org.example.classes";

	private record Example() {

	}

}
