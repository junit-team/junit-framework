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

import static org.apiguardian.api.API.Status.EXPERIMENTAL;

import java.io.Serial;

import org.apiguardian.api.API;
import org.junit.platform.commons.JUnitException;

/**
 * {@code DiscoveryIssueException} is an exception that is thrown if an engine
 * reports critical issues during test discovery.
 *
 * @since 1.13
 */
@API(status = EXPERIMENTAL, since = "6.0")
public class DiscoveryIssueException extends JUnitException {

	@Serial
	private static final long serialVersionUID = 1L;

	DiscoveryIssueException(String message) {
		super(message, null, false, false);
	}
}
