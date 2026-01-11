/*
 * Copyright 2015-2026 the original author or authors.
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v2.0 which
 * accompanies this distribution and is available at
 *
 * https://www.eclipse.org/legal/epl-v20.html
 */

package org.junit.jupiter.migrationsupport.rules.adapter;

import static java.lang.Boolean.TRUE;
import static org.apiguardian.api.API.Status.INTERNAL;

import org.apiguardian.api.API;
import org.junit.jupiter.migrationsupport.rules.member.TestRuleAnnotatedMember;
import org.junit.rules.ExpectedException;

/**
 * @since 5.0
 */
@API(status = INTERNAL, since = "5.0")
public class ExpectedExceptionAdapter extends AbstractTestRuleAdapter {

	public ExpectedExceptionAdapter(TestRuleAnnotatedMember annotatedMember) {
		super(annotatedMember, ExpectedException.class);
	}

	@Override
	public void handleTestExecutionException(Throwable cause) throws Throwable {
		executeMethod("handleException", new Class<?>[] { Throwable.class }, cause);
	}

	@Override
	public void after() {
		if (TRUE.equals(executeMethod("isAnyExceptionExpected"))) {
			executeMethod("failDueToMissingException");
		}
	}
}
