/*
 * Copyright 2015-2026 the original author or authors.
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v2.0 which
 * accompanies this distribution and is available at
 *
 * https://www.eclipse.org/legal/epl-v20.html
 */

package org.junit.platform.launcher.core;

import static java.util.stream.Collectors.toMap;
import static org.junit.platform.commons.util.StringUtils.nullSafeToString;

import java.util.Map;

import org.junit.platform.engine.ConfigurationParameters;

public class ConfigurationParametersFactoryForTests {

	private ConfigurationParametersFactoryForTests() {
	}

	public static ConfigurationParameters create(Map<String, ?> configParams) {
		return LauncherConfigurationParameters.builder() //
				.explicitParameters(withStringValues(configParams)) //
				.enableImplicitProviders(false) //
				.build();
	}

	private static Map<String, String> withStringValues(Map<String, ?> configParams) {
		return configParams.entrySet().stream().collect(toMap(Map.Entry::getKey, e -> nullSafeToString(e.getValue())));
	}
}
