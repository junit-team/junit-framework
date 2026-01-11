/*
 * Copyright 2015-2026 the original author or authors.
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v2.0 which
 * accompanies this distribution and is available at
 *
 * https://www.eclipse.org/legal/epl-v20.html
 */

package org.junit.platform.launcher.core;

import java.nio.file.Path;

import org.junit.platform.commons.JUnitException;
import org.junit.platform.engine.OutputDirectoryCreator;

public class OutputDirectoryCreators {

	public static OutputDirectoryCreator dummyOutputDirectoryCreator() {
		return new HierarchicalOutputDirectoryCreator(() -> {
			throw new JUnitException("This should not be called; use a real implementation instead");
		});
	}

	public static OutputDirectoryCreator hierarchicalOutputDirectoryCreator(Path rootDir) {
		return new HierarchicalOutputDirectoryCreator(() -> rootDir);
	}

	private OutputDirectoryCreators() {
	}
}
