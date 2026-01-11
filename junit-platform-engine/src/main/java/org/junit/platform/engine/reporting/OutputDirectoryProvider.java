/*
 * Copyright 2015-2026 the original author or authors.
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v2.0 which
 * accompanies this distribution and is available at
 *
 * https://www.eclipse.org/legal/epl-v20.html
 */

package org.junit.platform.engine.reporting;

import static org.apiguardian.api.API.Status.DEPRECATED;
import static org.apiguardian.api.API.Status.INTERNAL;

import org.apiguardian.api.API;
import org.junit.platform.engine.EngineDiscoveryRequest;
import org.junit.platform.engine.OutputDirectoryCreator;

/**
 * Provider of output directories for test engines to write reports and other
 * output files to.
 *
 * @since 1.12
 * @see EngineDiscoveryRequest#getOutputDirectoryProvider()
 * @deprecated Please implement {@link OutputDirectoryCreator} instead
 */
@SuppressWarnings("removal")
@Deprecated(since = "1.14", forRemoval = true)
@API(status = DEPRECATED, since = "1.14")
public interface OutputDirectoryProvider extends OutputDirectoryCreator {

	/**
	 * Cast or adapt an {@link OutputDirectoryCreator} to a
	 * {@code OutputDirectoryProvider}.
	 *
	 * @since 1.14
	 */
	@API(status = INTERNAL, since = "1.14")
	static OutputDirectoryProvider castOrAdapt(OutputDirectoryCreator outputDirectoryCreator) {
		if (outputDirectoryCreator instanceof OutputDirectoryProvider provider) {
			return provider;
		}
		return new OutputDirectoryProviderAdapter(outputDirectoryCreator);
	}

}
