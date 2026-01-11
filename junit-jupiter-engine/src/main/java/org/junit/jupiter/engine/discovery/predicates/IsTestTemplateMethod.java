/*
 * Copyright 2015-2026 the original author or authors.
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v2.0 which
 * accompanies this distribution and is available at
 *
 * https://www.eclipse.org/legal/epl-v20.html
 */

package org.junit.jupiter.engine.discovery.predicates;

import static org.apiguardian.api.API.Status.INTERNAL;

import org.apiguardian.api.API;
import org.junit.jupiter.api.TestTemplate;
import org.junit.platform.engine.support.discovery.DiscoveryIssueReporter;

/**
 * Test if a method is a JUnit Jupiter {@link TestTemplate @TestTemplate} method.
 *
 * @since 5.0
 */
@API(status = INTERNAL, since = "5.0")
public class IsTestTemplateMethod extends IsTestableMethod {

	public IsTestTemplateMethod(DiscoveryIssueReporter issueReporter) {
		super(TestTemplate.class, IsTestableMethod::hasVoidReturnType, issueReporter);
	}

}
