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

import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.migrationsupport.rules.FailAfterAllHelper.fail;

import java.util.ArrayList;
import java.util.List;

import org.junit.Rule;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.rules.ExternalResource;

@SuppressWarnings("removal")
@ExtendWith(ExternalResourceSupport.class)
public class ExternalResourceSupportForMixedMethodAndFieldRulesTests {

	private static List<String> initEvents = new ArrayList<>();
	private static List<String> beforeEvents = new ArrayList<>();
	private static List<String> afterEvents = new ArrayList<>();

	@BeforeAll
	static void clear() {
		initEvents.clear();
		beforeEvents.clear();
		afterEvents.clear();
	}

	@Rule
	public ExternalResource fieldRule1 = new MyExternalResource("fieldRule1");

	@Rule
	public ExternalResource fieldRule2 = new MyExternalResource("fieldRule2");

	@SuppressWarnings("JUnitMalformedDeclaration")
	@Rule
	ExternalResource methodRule1() {
		return new MyExternalResource("methodRule1");
	}

	@SuppressWarnings("JUnitMalformedDeclaration")
	@Rule
	ExternalResource methodRule2() {
		return new MyExternalResource("methodRule2");
	}

	@Test
	void constructorsAndBeforeEachMethodsOfAllRulesWereExecuted() {
		assertThat(initEvents).hasSize(4);
		// the order of fields and methods is not stable, but fields are initialized before methods are called
		assertThat(initEvents.subList(0, 2)).allMatch(item -> item.startsWith("fieldRule"));
		assertThat(initEvents.subList(2, 4)).allMatch(item -> item.startsWith("methodRule"));
		// beforeEach methods of rules from fields are run before those from methods but in reverse order of instantiation
		assertEquals(asList(initEvents.get(1), initEvents.get(0), initEvents.get(3), initEvents.get(2)), beforeEvents);
	}

	@AfterAll
	static void afterMethodsOfAllRulesWereExecuted() {
		// beforeEach methods of rules from methods are run before those from fields but in reverse order
		if (!asList(initEvents.get(2), initEvents.get(3), initEvents.get(0), initEvents.get(1)).equals(afterEvents)) {
			fail();
		}
	}

	static class MyExternalResource extends ExternalResource {

		private final String name;

		MyExternalResource(String name) {
			this.name = name;
			initEvents.add(name);
		}

		@Override
		protected void before() {
			beforeEvents.add(name);
		}

		@Override
		protected void after() {
			afterEvents.add(name);
		}
	}
}
