/*
 * Copyright 2015-2026 the original author or authors.
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v2.0 which
 * accompanies this distribution and is available at
 *
 * https://www.eclipse.org/legal/epl-v20.html
 */

package org.junit.jupiter.migrationsupport.rules.member;

import org.jspecify.annotations.Nullable;
import org.junit.platform.commons.util.Preconditions;
import org.junit.rules.TestRule;

/**
 * @since 5.0
 */
abstract class AbstractTestRuleAnnotatedMember implements TestRuleAnnotatedMember {

	private final TestRule testRule;

	AbstractTestRuleAnnotatedMember(@Nullable TestRule testRule) {
		this.testRule = Preconditions.notNull(testRule, "TestRule must not be null");
	}

	@Override
	public TestRule getTestRule() {
		return this.testRule;
	}

}
