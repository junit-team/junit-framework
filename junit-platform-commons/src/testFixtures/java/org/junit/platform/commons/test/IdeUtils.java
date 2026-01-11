/*
 * Copyright 2015-2026 the original author or authors.
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v2.0 which
 * accompanies this distribution and is available at
 *
 * https://www.eclipse.org/legal/epl-v20.html
 */

package org.junit.platform.commons.test;

/**
 * Collection of test utilities for IDEs.
 */
public class IdeUtils {

	/**
	 * Determine if the current code is running in the Eclipse IDE.
	 * <p>Copied from {@code org.springframework.core.testfixture.ide.IdeUtils}.
	 */
	public static boolean runningInEclipse() {
		return StackWalker.getInstance().walk(
			stream -> stream.anyMatch(stackFrame -> stackFrame.getClassName().startsWith("org.eclipse.jdt")));
	}

	private IdeUtils() {
	}

}
