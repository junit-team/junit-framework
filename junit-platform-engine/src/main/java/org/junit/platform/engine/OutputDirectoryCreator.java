/*
 * Copyright 2015-2025 the original author or authors.
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v2.0 which
 * accompanies this distribution and is available at
 *
 * https://www.eclipse.org/legal/epl-v20.html
 */

package org.junit.platform.engine;

import static org.apiguardian.api.API.Status.MAINTAINED;

import java.io.IOException;
import java.nio.file.Path;

import org.apiguardian.api.API;

/**
 * Provider of output directories for test engines to write reports and other
 * output files to.
 *
 * @since 6.0
 * @see EngineDiscoveryRequest#getOutputDirectoryCreator()
 */
@API(status = MAINTAINED, since = "1.13.3")
public interface OutputDirectoryCreator {

	/**
	 * {@return the root directory for all output files; never {@code null}}
	 */
	Path getRootDirectory();

	/**
	 * Create an output directory for the supplied test descriptor.
	 *
	 * @param testDescriptor the test descriptor for which to create an output
	 * directory; never {@code null}
	 * @return the output directory
	 * @throws IOException if the output directory could not be created
	 */
	Path createOutputDirectory(TestDescriptor testDescriptor) throws IOException;

}
