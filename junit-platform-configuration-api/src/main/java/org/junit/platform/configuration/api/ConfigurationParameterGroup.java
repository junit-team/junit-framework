/*
 * Copyright 2026 the original author or authors.
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v2.0 which
 * accompanies this distribution and is available at
 *
 * https://www.eclipse.org/legal/epl-v20.html
 */

package org.junit.platform.configuration.api;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.apiguardian.api.API;

/**
 * Defines a configuration parameter group. A group is a collection of
 * configuration parameters that have the same prefix.
 * <p>
 * This annotation should be used to facilitate the automated
 * generation of documentation.
 *
 * @since 6.2
 */
@API(status = API.Status.EXPERIMENTAL, since = "6.2")
@Retention(RetentionPolicy.SOURCE)
@Target(ElementType.TYPE)
public @interface ConfigurationParameterGroup {

	/**
	 * The name of the group.
	 */
	String value();

}
