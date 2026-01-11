/*
 * Copyright 2015-2026 the original author or authors.
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v2.0 which
 * accompanies this distribution and is available at
 *
 * https://www.eclipse.org/legal/epl-v20.html
 */

package org.junit.platform.console.output;

import org.junit.platform.engine.TestExecutionResult;
import org.junit.platform.launcher.TestIdentifier;

/**
 * @since 1.9
 */
enum Style {

	NONE, SUCCESSFUL, ABORTED, FAILED, SKIPPED, CONTAINER, TEST, DYNAMIC, REPORTED;

	static Style valueOf(TestExecutionResult result) {
		return switch (result.getStatus()) {
			case SUCCESSFUL -> Style.SUCCESSFUL;
			case ABORTED -> Style.ABORTED;
			case FAILED -> Style.FAILED;
		};
	}

	static Style valueOf(TestIdentifier testIdentifier) {
		return testIdentifier.isContainer() ? CONTAINER : TEST;
	}

}
