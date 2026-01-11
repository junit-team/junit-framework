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

import java.io.IOException;
import java.nio.file.Path;

import org.junit.platform.engine.OutputDirectoryCreator;
import org.junit.platform.engine.TestDescriptor;

/**
 * @since 1.14
 */
@SuppressWarnings("removal")
class OutputDirectoryProviderAdapter implements OutputDirectoryProvider {

	private final OutputDirectoryCreator outputDirectoryCreator;

	OutputDirectoryProviderAdapter(OutputDirectoryCreator outputDirectoryCreator) {
		this.outputDirectoryCreator = outputDirectoryCreator;
	}

	@Override
	public Path getRootDirectory() {
		return this.outputDirectoryCreator.getRootDirectory();
	}

	@Override
	public Path createOutputDirectory(TestDescriptor testDescriptor) throws IOException {
		return this.outputDirectoryCreator.createOutputDirectory(testDescriptor);
	}
}
