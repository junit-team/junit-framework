/*
 * Copyright 2015-2026 the original author or authors.
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v2.0 which
 * accompanies this distribution and is available at
 *
 * https://www.eclipse.org/legal/epl-v20.html
 */

package org.junit.platform.suite.api;

import static org.apiguardian.api.API.Status.STABLE;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Repeatable;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.apiguardian.api.API;

/**
 * {@code @ConfigurationParameter} is a {@linkplain Repeatable repeatable}
 * annotation that specifies a configuration {@link #key key} and
 * {@link #value value} pair to be added to the discovery request when running
 * a test suite on the JUnit Platform.
 *
 * @since 1.8
 * @see ConfigurationParametersResource
 * @see DisableParentConfigurationParameters
 * @see Suite
 * @see org.junit.platform.launcher.core.LauncherDiscoveryRequestBuilder#configurationParameter(String, String)
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
@Inherited
@Documented
@API(status = STABLE, since = "1.10")
@Repeatable(ConfigurationParameters.class)
public @interface ConfigurationParameter {

	/**
	 * The configuration parameter key under which to add the {@link #value() value}
	 * to the discovery request; never {@code null} or blank.
	 */
	String key();

	/**
	 * The value to add to the discovery request for the specified {@link #key() key}.
	 */
	String value();

}
