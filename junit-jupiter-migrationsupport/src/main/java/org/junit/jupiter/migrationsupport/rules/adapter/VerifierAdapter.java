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

import static org.apiguardian.api.API.Status.INTERNAL;

import org.apiguardian.api.API;
import org.junit.jupiter.migrationsupport.rules.member.TestRuleAnnotatedMember;
import org.junit.rules.Verifier;

/**
 * @since 5.0
 */
@API(status = INTERNAL, since = "5.0")
public class VerifierAdapter extends AbstractTestRuleAdapter {

	public VerifierAdapter(TestRuleAnnotatedMember annotatedMember) {
		super(annotatedMember, Verifier.class);
	}

	@Override
	public void after() {
		executeMethod("verify");
	}

}
