/*
 * Copyright 2015-2026 the original author or authors.
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v2.0 which
 * accompanies this distribution and is available at
 *
 * https://www.eclipse.org/legal/epl-v20.html
 */

package org.junit.jupiter.api;

import static org.apiguardian.api.API.Status.STABLE;

import java.util.List;
import java.util.Optional;

import org.apiguardian.api.API;

/**
 * {@code ClassOrdererContext} encapsulates the <em>context</em> in which
 * a {@link ClassOrderer} will be invoked.
 *
 * @since 5.8
 * @see ClassOrderer
 * @see ClassDescriptor
 */
@API(status = STABLE, since = "5.10")
public interface ClassOrdererContext {

	/**
	 * Get the list of {@linkplain ClassDescriptor class descriptors} to
	 * order.
	 *
	 * @return the list of class descriptors; never {@code null}
	 */
	List<? extends ClassDescriptor> getClassDescriptors();

	/**
	 * Get the configuration parameter stored under the specified {@code key}.
	 *
	 * <p>If no such key is present in the {@code ConfigurationParameters} for
	 * the JUnit Platform, an attempt will be made to look up the value as a
	 * JVM system property. If no such system property exists, an attempt will
	 * be made to look up the value in the JUnit Platform properties file.
	 *
	 * @param key the key to look up; never {@code null} or blank
	 * @return an {@code Optional} containing the value; never {@code null}
	 * but potentially empty
	 *
	 * @see System#getProperty(String)
	 * @see org.junit.platform.engine.ConfigurationParameters
	 */
	Optional<String> getConfigurationParameter(String key);

}
