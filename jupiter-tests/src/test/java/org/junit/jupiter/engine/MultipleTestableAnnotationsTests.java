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
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.platform.commons.util.CollectionUtils.getOnlyElement;

import org.junit.jupiter.api.RepeatedTest;
import org.junit.jupiter.api.RepetitionInfo;
import org.junit.jupiter.api.Test;
import org.junit.platform.engine.DiscoveryIssue.Severity;
import org.junit.platform.engine.support.descriptor.MethodSource;

/**
 * Integration tests that verify the correct behavior for methods annotated
 * with multiple testable annotations simultaneously.
 *
 * @since 5.0
 */
class MultipleTestableAnnotationsTests extends AbstractJupiterTestEngineTests {

	@Test
	void testAndRepeatedTest() throws Exception {
		var results = discoverTestsForClass(TestCase.class);

		var discoveryIssue = getOnlyElement(results.getDiscoveryIssues());

		assertThat(discoveryIssue.severity()) //
				.isEqualTo(Severity.WARNING);
		assertThat(discoveryIssue.message()) //
				.matches("Possible configuration error: method .+ resulted in multiple TestDescriptors .+");
		assertThat(discoveryIssue.source()) //
				.contains(
					MethodSource.from(TestCase.class.getDeclaredMethod("testAndRepeatedTest", RepetitionInfo.class)));
	}

	@SuppressWarnings("JUnitMalformedDeclaration")
	static class TestCase {

		@Test
		@RepeatedTest(1)
		void testAndRepeatedTest(RepetitionInfo repetitionInfo) {
			assertNotNull(repetitionInfo);
		}

	}

}
