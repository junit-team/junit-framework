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
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.apiguardian.api.API;

/**
 * Disable parent configuration parameters.
 *
 * <p>By default, a suite discovers tests using the configuration parameters
 * explicitly configured via {@link ConfigurationParameter @ConfigurationParameter}
 * and {@link ConfigurationParametersResource} as well as the configuration
 * parameters from the discovery request that was used to discover the suite.
 *
 * <p>Annotating a suite with this annotation disables the latter source so
 * that only explicit configuration parameters and resources are taken into
 * account.
 *
 * @since 1.8
 * @see ConfigurationParameter
 * @see ConfigurationParametersResource
 * @see Suite
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
@Inherited
@Documented
@API(status = STABLE, since = "1.10")
public @interface DisableParentConfigurationParameters {
}
