/*
 * Copyright 2015-2025 the original author or authors.
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v2.0 which
 * accompanies this distribution and is available at
 *
 * https://www.eclipse.org/legal/epl-v20.html
 */

package org.junit.platform.launcher;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.platform.engine.discovery.DiscoverySelectors.selectClass;
import static org.junit.platform.launcher.TagFilter.includeTags;
import static org.junit.platform.launcher.TagIntegrationTests.TaggedTestCase.doubleTaggedWasExecuted;
import static org.junit.platform.launcher.TagIntegrationTests.TaggedTestCase.tag1WasExecuted;
import static org.junit.platform.launcher.TagIntegrationTests.TaggedTestCase.tag2WasExecuted;
import static org.junit.platform.launcher.TagIntegrationTests.TaggedTestCase.unTaggedWasExecuted;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.junit.platform.testkit.engine.EngineTestKit;

class TagIntegrationTests {

	@BeforeEach
	void init() {
		tag1WasExecuted = false;
		tag2WasExecuted = false;
		unTaggedWasExecuted = false;
		doubleTaggedWasExecuted = false;
	}

	@Test
	void includingWrongTagExecutesNothing() {
		executeTaggedTestCase(includeTags("whatever"));

		assertFalse(tag1WasExecuted);
		assertFalse(tag2WasExecuted);
		assertFalse(doubleTaggedWasExecuted);
		assertFalse(unTaggedWasExecuted);
	}

	@Test
	void includingSuitableTagExecutesTaggedTestOnly() {
		executeTaggedTestCase(includeTags("tag1"));

		assertTrue(tag1WasExecuted);
		assertFalse(tag2WasExecuted);
		assertTrue(doubleTaggedWasExecuted);
		assertFalse(unTaggedWasExecuted);
	}

	@ParameterizedTest
	@ValueSource(strings = { "any()", "!none()" })
	void includingTheAnyKeywordExecutesAllTaggedTests(String tagExpression) {
		executeTaggedTestCase(includeTags(tagExpression));

		assertTrue(tag1WasExecuted);
		assertTrue(tag2WasExecuted);
		assertTrue(doubleTaggedWasExecuted);
		assertFalse(unTaggedWasExecuted);
	}

	@ParameterizedTest
	@ValueSource(strings = { "none()", "!any()" })
	void includingTheNoneKeywordExecutesAllUntaggedTests(String tagExpression) {
		executeTaggedTestCase(includeTags(tagExpression));

		assertFalse(tag1WasExecuted);
		assertFalse(tag2WasExecuted);
		assertFalse(doubleTaggedWasExecuted);
		assertTrue(unTaggedWasExecuted);
	}

	private void executeTaggedTestCase(PostDiscoveryFilter filter) {
		EngineTestKit.engine("junit-jupiter") //
				.selectors(selectClass(TaggedTestCase.class)) //
				.filters(filter) //
				.execute();
	}

	static class TaggedTestCase {

		static boolean tag1WasExecuted = false;
		static boolean tag2WasExecuted = false;
		static boolean unTaggedWasExecuted = false;
		static boolean doubleTaggedWasExecuted = false;

		@Test
		@Tag("tag1")
		void tagged1() {
			tag1WasExecuted = true;
		}

		@Test
		@Tag("tag2")
		void tagged2() {
			tag2WasExecuted = true;
		}

		@Test
		@Tag("tag1")
		@Tag("tag2")
		void doubleTagged() {
			doubleTaggedWasExecuted = true;
		}

		@Test
		void unTagged() {
			unTaggedWasExecuted = true;
		}

	}

}
