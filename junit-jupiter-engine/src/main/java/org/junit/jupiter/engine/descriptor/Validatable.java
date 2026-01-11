/*
 * Copyright 2015-2026 the original author or authors.
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v2.0 which
 * accompanies this distribution and is available at
 *
 * https://www.eclipse.org/legal/epl-v20.html
 */

package org.junit.jupiter.engine.descriptor;

import static org.apiguardian.api.API.Status.INTERNAL;

import java.util.List;

import org.apiguardian.api.API;
import org.junit.platform.engine.DiscoveryIssue;
import org.junit.platform.engine.support.discovery.DiscoveryIssueReporter;

/**
 * Interface for descriptors that can be validated during discovery.
 *
 * @since 5.13
 */
@API(status = INTERNAL, since = "5.13")
public interface Validatable {

	/**
	 * Validate the state of this descriptor and report any issues found to the
	 * supplied {@link DiscoveryIssueReporter}.
	 */
	void validate(DiscoveryIssueReporter reporter);

	/**
	 * Report and clear the given list of {@link DiscoveryIssue}s using the
	 * supplied {@link DiscoveryIssueReporter}.
	 */
	static void reportAndClear(List<DiscoveryIssue> issues, DiscoveryIssueReporter reporter) {
		issues.forEach(reporter::reportIssue);
		issues.clear();
	}

}
