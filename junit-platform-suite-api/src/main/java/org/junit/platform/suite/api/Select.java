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
 * {@code @Select} is a {@linkplain Repeatable repeatable} annotation that
 * specifies which tests to <em>select</em> based on prefixed
 * {@linkplain org.junit.platform.engine.DiscoverySelectorIdentifier selector identifiers}.
 *
 * @since 1.11
 * @see Suite
 * @see org.junit.platform.engine.discovery.DiscoverySelectors#parse(String)
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
@Inherited
@Documented
@API(status = MAINTAINED, since = "1.13.3")
@Repeatable(Selects.class)
public @interface Select {

	/**
	 * One or more prefixed
	 * {@linkplain org.junit.platform.engine.DiscoverySelectorIdentifier selector identifiers}
	 * to select.
	 */
	String[] value();

}
