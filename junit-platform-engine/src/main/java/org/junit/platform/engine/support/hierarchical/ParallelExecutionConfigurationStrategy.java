/*
 * Copyright 2015-2026 the original author or authors.
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v2.0 which
 * accompanies this distribution and is available at
 *
 * https://www.eclipse.org/legal/epl-v20.html
 */

package org.junit.platform.engine.support.hierarchical;

import static org.apiguardian.api.API.Status.STABLE;

import org.apiguardian.api.API;
import org.junit.platform.engine.ConfigurationParameters;

/**
 * A strategy to use for configuring parallel test execution.
 *
 * @since 1.3
 * @see DefaultParallelExecutionConfigurationStrategy
 */
@API(status = STABLE, since = "1.10")
public interface ParallelExecutionConfigurationStrategy {

	/**
	 * Create a configuration for parallel test execution based on the supplied
	 * {@link ConfigurationParameters}.
	 */
	ParallelExecutionConfiguration createConfiguration(ConfigurationParameters configurationParameters);

}
