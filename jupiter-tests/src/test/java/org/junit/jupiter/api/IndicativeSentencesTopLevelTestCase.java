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
 * This test case declares {@link IndicativeSentencesGeneration} on a top-level
 * test class that contains a nested test class.
 *
 * @see IndicativeSentencesNestedTestCase
 * @since 5.8
 */
@IndicativeSentencesGeneration(separator = " -> ", generator = ReplaceUnderscores.class)
class IndicativeSentencesTopLevelTestCase {

	@Nested
	class A_year_is_a_leap_year {

		@Test
		void if_it_is_divisible_by_4_but_not_by_100() {
		}
	}

}
