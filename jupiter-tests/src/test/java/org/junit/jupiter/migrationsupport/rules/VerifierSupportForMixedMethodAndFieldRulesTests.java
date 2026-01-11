/*
 * Copyright 2015-2026 the original author or authors.
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v2.0 which
 * accompanies this distribution and is available at
 *
 * https://www.eclipse.org/legal/epl-v20.html
 */

package org.junit.jupiter.migrationsupport.rules;

import static org.junit.jupiter.migrationsupport.rules.FailAfterAllHelper.fail;

import org.junit.Rule;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.rules.Verifier;

@SuppressWarnings("removal")
@ExtendWith(VerifierSupport.class)
public class VerifierSupportForMixedMethodAndFieldRulesTests {

	private static boolean afterOfRule1WasExecuted = false;
	private static boolean afterOfRule2WasExecuted = false;

	@Rule
	public Verifier verifier1 = new Verifier() {

		@Override
		protected void verify() {
			afterOfRule1WasExecuted = true;
		}
	};

	private Verifier verifier2 = new Verifier() {

		@Override
		protected void verify() {
			afterOfRule2WasExecuted = true;
		}
	};

	@Rule
	public Verifier getVerifier2() {
		return verifier2;
	}

	@Test
	void testNothing() {
		//needed to start the test process at all
	}

	@AfterAll
	static void afterMethodsOfBothRulesWereExecuted() {
		if (!afterOfRule1WasExecuted) {
			fail();
		}
		if (!afterOfRule2WasExecuted) {
			fail();
		}
	}

}
