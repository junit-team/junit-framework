/*
 * Copyright 2015-2026 the original author or authors.
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v2.0 which
 * accompanies this distribution and is available at
 *
 * https://www.eclipse.org/legal/epl-v20.html
 */

package org.junit.jupiter.engine;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.engine.kotlin.ArbitraryNamingKotlinTestCase.METHOD_NAME;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.engine.kotlin.ArbitraryNamingKotlinTestCase;
import org.junit.platform.engine.TestDescriptor;
import org.junit.platform.testkit.engine.EngineExecutionResults;

/**
 * Integration tests for JVM languages that allow special characters
 * in method names (e.g., Kotlin, Groovy, etc.) which are forbidden in
 * Java source code.
 *
 * @since 5.1
 */
class AtypicalJvmMethodNameTests extends AbstractJupiterTestEngineTests {

	@Test
	void kotlinTestWithMethodNameContainingSpecialCharacters() {
		EngineExecutionResults executionResults = executeTestsForClass(ArbitraryNamingKotlinTestCase.class);
		assertThat(executionResults.testEvents().started().count()).isEqualTo(2);

		TestDescriptor testDescriptor1 = executionResults.testEvents().succeeded().list().get(0).getTestDescriptor();
		assertAll(//
			() -> assertEquals(METHOD_NAME + "()", testDescriptor1.getDisplayName()), //
			() -> assertEquals(METHOD_NAME + "()", testDescriptor1.getLegacyReportingName()));

		TestDescriptor testDescriptor2 = executionResults.testEvents().succeeded().list().get(1).getTestDescriptor();
		assertAll(//
			() -> assertEquals("test name ends with parentheses()()", testDescriptor2.getDisplayName()), //
			() -> assertEquals("test name ends with parentheses()()", testDescriptor2.getLegacyReportingName()));
	}

}
