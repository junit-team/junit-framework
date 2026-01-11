/*
 * Copyright 2015-2026 the original author or authors.
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v2.0 which
 * accompanies this distribution and is available at
 *
 * https://www.eclipse.org/legal/epl-v20.html
 */

package org.junit.jupiter.params.provider;

import static java.util.stream.Collectors.toSet;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.params.provider.EnumSource.Mode.EXCLUDE;
import static org.junit.jupiter.params.provider.EnumSource.Mode.INCLUDE;
import static org.junit.jupiter.params.provider.EnumSource.Mode.MATCH_ALL;
import static org.junit.jupiter.params.provider.EnumSource.Mode.MATCH_ANY;
import static org.junit.jupiter.params.provider.EnumSource.Mode.MATCH_NONE;
import static org.junit.jupiter.params.provider.EnumSourceTests.EnumWithThreeConstants.BAR;
import static org.junit.jupiter.params.provider.EnumSourceTests.EnumWithThreeConstants.BAZ;
import static org.junit.jupiter.params.provider.EnumSourceTests.EnumWithThreeConstants.FOO;

import java.util.EnumSet;
import java.util.Set;
import java.util.function.Function;

import org.junit.jupiter.api.Test;

/**
 * @since 5.0
 */
class EnumSourceTests {

	@Test
	void includeNamesWithAll() {
		assertAll("include names with all", //
			() -> assertTrue(INCLUDE.select(FOO, allOf(EnumWithThreeConstants::name))),
			() -> assertTrue(INCLUDE.select(BAR, allOf(EnumWithThreeConstants::name))),
			() -> assertTrue(INCLUDE.select(BAZ, allOf(EnumWithThreeConstants::name))) //
		);
	}

	@Test
	void includeNamesWithSingleton() {
		assertAll("include names with singleton", //
			() -> assertTrue(INCLUDE.select(FOO, Set.of(FOO.name()))),
			() -> assertTrue(INCLUDE.select(BAR, Set.of(BAR.name()))),
			() -> assertTrue(INCLUDE.select(BAZ, Set.of(BAZ.name()))) //
		);
		assertAll("include names with singleton complement", //
			() -> assertFalse(INCLUDE.select(BAR, Set.of(FOO.name()))),
			() -> assertFalse(INCLUDE.select(BAZ, Set.of(FOO.name()))) //
		);
	}

	@Test
	void excludeNames() {
		assertAll("exclude name with none excluded", //
			() -> assertTrue(EXCLUDE.select(FOO, Set.of())), //
			() -> assertTrue(EXCLUDE.select(BAR, Set.of())), //
			() -> assertTrue(EXCLUDE.select(BAZ, Set.of())) //
		);
		assertAll("exclude name with FOO excluded", //
			() -> assertFalse(EXCLUDE.select(FOO, Set.of(FOO.name()))),
			() -> assertTrue(EXCLUDE.select(BAR, Set.of(FOO.name()))),
			() -> assertTrue(EXCLUDE.select(BAZ, Set.of(FOO.name()))) //
		);
	}

	@Test
	void matchesAll() {
		assertAll("matches all", //
			() -> assertTrue(MATCH_ALL.select(FOO, Set.of("F.."))),
			() -> assertTrue(MATCH_ALL.select(BAR, Set.of("B.."))),
			() -> assertTrue(MATCH_ALL.select(BAZ, Set.of("B.."))) //
		);
		assertAll("matches all fails if not all match", //
			() -> assertFalse(MATCH_ALL.select(FOO, Set.of("F..", "."))),
			() -> assertFalse(MATCH_ALL.select(BAR, Set.of("B..", "."))),
			() -> assertFalse(MATCH_ALL.select(BAZ, Set.of("B..", "."))) //
		);
	}

	@Test
	void matchesAny() {
		assertAll("matches any", //
			() -> assertTrue(MATCH_ANY.select(FOO, Set.of("B..", "^F.*"))),
			() -> assertTrue(MATCH_ANY.select(BAR, Set.of("B", "B.", "B.."))),
			() -> assertTrue(MATCH_ANY.select(BAZ, Set.of("^.+[zZ]$"))));
	}

	@Test
	void matchesNone() {
		assertAll("matches none fails if any match", //
			() -> assertFalse(MATCH_NONE.select(FOO, Set.of("F.."))),
			() -> assertFalse(MATCH_NONE.select(FOO, Set.of("B..", "F.."))),
			() -> assertFalse(MATCH_NONE.select(BAZ, Set.of("B.", "F.", "^.+[zZ]$"))));

		assertAll("matches none", //
			() -> assertTrue(MATCH_NONE.select(FOO, Set.of())), //
			() -> assertTrue(MATCH_NONE.select(FOO, Set.of("F."))),
			() -> assertTrue(MATCH_NONE.select(FOO, Set.of("B.."))),
			() -> assertTrue(MATCH_NONE.select(BAZ, Set.of(".", "B.", "F."))));
	}

	enum EnumWithThreeConstants {
		FOO, BAR, BAZ

	}

	static Set<String> allOf(Function<EnumWithThreeConstants, String> mapper) {
		return EnumSet.allOf(EnumWithThreeConstants.class).stream().map(mapper).collect(toSet());
	}

}
