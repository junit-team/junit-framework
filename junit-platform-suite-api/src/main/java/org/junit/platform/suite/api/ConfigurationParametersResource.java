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

import static org.apiguardian.api.API.Status.MAINTAINED;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Repeatable;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.apiguardian.api.API;

/**
 * {@code @ConfigurationParametersResource} is a
 * {@linkplain Repeatable repeatable} annotation that specifies a configuration
 * file in Java's properties format on the classpath to be added to the
 * discovery request when running a test suite on the JUnit Platform.
 *
 * @since 1.11
 * @see ConfigurationParameter
 * @see DisableParentConfigurationParameters
 * @see Suite
 * @see org.junit.platform.launcher.core.LauncherDiscoveryRequestBuilder#configurationParametersResources(String...)
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
@Inherited
@Documented
@API(status = MAINTAINED, since = "1.13.3")
@Repeatable(ConfigurationParametersResources.class)
public @interface ConfigurationParametersResource {

	/**
	 * The classpath location for the desired properties file; never {@code null} or blank.
	 */
	String value();

}
