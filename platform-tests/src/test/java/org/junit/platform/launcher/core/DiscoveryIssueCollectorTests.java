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

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import org.junit.jupiter.api.Test;
import org.junit.platform.engine.DiscoveryIssue;
import org.junit.platform.engine.DiscoveryIssue.Severity;
import org.junit.platform.engine.UniqueId;

class DiscoveryIssueCollectorTests {

	@Test
	void reportsCollectedDiscoveryIssues() {
		var collector = new DiscoveryIssueCollector(mock());
		var issue = DiscoveryIssue.create(Severity.ERROR, "hello");
		collector.issueEncountered(UniqueId.forEngine("dummy"), issue);
		assertThat(collector.toNotifier().getAllIssues()).containsExactly(issue);
	}

}
