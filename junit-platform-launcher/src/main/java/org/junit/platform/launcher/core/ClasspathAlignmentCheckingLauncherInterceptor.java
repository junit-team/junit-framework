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

import java.util.Optional;

import org.junit.platform.commons.JUnitException;
import org.junit.platform.launcher.LauncherInterceptor;

class ClasspathAlignmentCheckingLauncherInterceptor implements LauncherInterceptor {

	static final LauncherInterceptor INSTANCE = new ClasspathAlignmentCheckingLauncherInterceptor();

	@Override
	public <T> T intercept(Invocation<T> invocation) {
		try {
			return invocation.proceed();
		}
		catch (LinkageError e) {
			Optional<JUnitException> exception = ClasspathAlignmentChecker.check(e);
			if (exception.isPresent()) {
				throw exception.get();
			}
			throw e;
		}
	}

	@Override
	public void close() {
		// do nothing
	}
}
