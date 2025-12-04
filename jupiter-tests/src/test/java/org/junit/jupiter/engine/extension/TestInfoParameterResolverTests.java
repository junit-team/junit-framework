/*
 * Copyright 2015-2025 the original author or authors.
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v2.0 which
 * accompanies this distribution and is available at
 *
 * https://www.eclipse.org/legal/epl-v20.html
 */

package org.junit.jupiter.engine.extension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.platform.engine.discovery.DiscoverySelectors.selectClass;
import static org.junit.platform.launcher.LauncherConstants.CRITICAL_DISCOVERY_ISSUE_SEVERITY_PROPERTY_NAME;

import java.util.Arrays;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInfo;
import org.junit.jupiter.engine.AbstractJupiterTestEngineTests;
import org.junit.platform.engine.DiscoveryIssue.Severity;

/**
 * Integration tests for {@link TestInfoParameterResolver}.
 *
 * @since 5.0
 */
@Tag("class-tag")
class TestInfoParameterResolverTests extends AbstractJupiterTestEngineTests {

	private static final List<String> allDisplayNames = Arrays.asList("defaultDisplayName(TestInfo)",
		"custom display name", "getTags(TestInfo)", "customDisplayNameThatIsEmpty()");

	TestInfoParameterResolverTests(TestInfo testInfo) {
		assertThat(testInfo.getTestClass()).contains(TestInfoParameterResolverTests.class);
		assertThat(testInfo.getTestMethod()).isPresent();
	}

	@Test
	void defaultDisplayName(TestInfo testInfo) {
		assertEquals("defaultDisplayName(TestInfo)", testInfo.getDisplayName());
	}

	@Test
	@DisplayName("custom display name")
	void providedDisplayName(TestInfo testInfo) {
		assertEquals("custom display name", testInfo.getDisplayName());
	}

	@Test
	void customDisplayNameThatIsEmpty() {
		executeTests(request -> request //
				.selectors(selectClass(BlankDisplayNameTestCase.class)) //
				.configurationParameter(CRITICAL_DISCOVERY_ISSUE_SEVERITY_PROPERTY_NAME, Severity.ERROR.name())) //
						.testEvents() //
						.assertStatistics(stats -> stats.started(1).succeeded(1));
	}

	@Test
	@Tag("method-tag")
	void getTags(TestInfo testInfo) {
		assertEquals(2, testInfo.getTags().size());
		assertTrue(testInfo.getTags().contains("method-tag"));
		assertTrue(testInfo.getTags().contains("class-tag"));
	}

	@BeforeEach
	@AfterEach
	void beforeAndAfter(TestInfo testInfo) {
		assertThat(allDisplayNames).contains(testInfo.getDisplayName());
	}

	@BeforeAll
	static void beforeAll(TestInfo testInfo) {
		Set<String> tags = testInfo.getTags();
		assertEquals(1, tags.size());
		assertTrue(tags.contains("class-tag"));
	}

	@BeforeAll
	@AfterAll
	static void beforeAndAfterAll(TestInfo testInfo) {
		assertEquals(TestInfoParameterResolverTests.class.getSimpleName(), testInfo.getDisplayName());
	}

	@SuppressWarnings("JUnitMalformedDeclaration")
	static class BlankDisplayNameTestCase {

		@Test
		@DisplayName("")
		void test(TestInfo testInfo) {
			assertEquals("test(TestInfo)", testInfo.getDisplayName());
		}
	}

}
