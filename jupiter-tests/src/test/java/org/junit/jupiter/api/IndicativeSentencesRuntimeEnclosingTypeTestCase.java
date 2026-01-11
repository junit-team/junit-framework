/*
 * Copyright 2015-2026 the original author or authors.
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v2.0 which
 * accompanies this distribution and is available at
 *
 * https://www.eclipse.org/legal/epl-v20.html
 */

package org.junit.jupiter.api;

import org.junit.jupiter.api.DisplayNameGenerator.ReplaceUnderscores;

/**
 * @since 5.12
 */
@DisplayName("Base Scenario")
@IndicativeSentencesGeneration(separator = " -> ", generator = ReplaceUnderscores.class)
abstract class IndicativeSentencesRuntimeEnclosingTypeTestCase {

	@Nested
	class Level_1 {

		@Nested
		class Level_2 {

			@Test
			void this_is_a_test() {
			}
		}
	}
}
