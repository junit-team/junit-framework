/*
 * Copyright 2015-2026 the original author or authors.
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v2.0 which
 * accompanies this distribution and is available at
 *
 * https://www.eclipse.org/legal/epl-v20.html
 */

package org.junit.jupiter.api.condition;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;
import static org.junit.jupiter.api.condition.AbstractOsBasedExecutionCondition.osNameMatches;
import static org.junit.platform.commons.test.PreconditionAssertions.assertPreconditionViolationFor;
import static org.junit.platform.testkit.engine.EventConditions.container;
import static org.junit.platform.testkit.engine.EventConditions.event;
import static org.junit.platform.testkit.engine.EventConditions.skippedWithReason;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Locale;

import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ConditionEvaluationResult;
import org.junit.jupiter.api.util.DefaultLocale;
import org.junit.jupiter.engine.AbstractJupiterTestEngineTests;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * Tests for the operating system {@code name} attribute of
 * {@link EnabledOnOs @EnabledOnOs} and {@link DisabledOnOs @DisabledOnOs}.
 *
 * <p>Most annotations in these tests are created at runtime so that they can
 * be configured with the name and architecture of the operating system that
 * executes the tests.
 *
 * @since 6.2
 */
class OsNameConditionTests extends AbstractJupiterTestEngineTests {

	private static final String CURRENT_OS_NAME = System.getProperty("os.name");
	private static final String CURRENT_ARCHITECTURE = System.getProperty("os.arch");

	// Longer than the current values, so they cannot be contained in them.
	private static final String NON_MATCHING_OS_NAME = CURRENT_OS_NAME + " (another operating system)";
	private static final String NON_MATCHING_ARCHITECTURE = CURRENT_ARCHITECTURE + "-another-architecture";

	private static final OS[] NO_OPERATING_SYSTEMS = {};
	private static final String[] NO_ARCHITECTURES = {};

	@Nested
	class NameMatching {

		@ParameterizedTest(name = "\"{0}\" contains \"{1}\": {2}")
		@CsvSource(delimiter = '|', textBlock = """
				AmongOS                | AmongOS          | true
				AmongOS 2.0            | AmongOS          | true
				Vendor AmongOS Edition | amongos          | true
				AmongOS                | aMoNgOs          | true
				Vendor OS              | Vendor OS        | true
				Linux                  | AmongOS          | false
				AmongOS                | AmongOS Extended | false
				AmongOS                | .*               | false
				""")
		void nameIsMatchedAsCaseInsensitiveLiteralSubstring(String osName, String name, boolean expected) {
			assertEquals(expected, osNameMatches(osName, name));
		}

		@ParameterizedTest
		@ValueSource(strings = { "", " ", "\t" })
		void blankNameNeverMatches(String name) {
			assertFalse(osNameMatches("AmongOS", name));
		}

		@ParameterizedTest
		@NullAndEmptySource
		@ValueSource(strings = { " ", "\t" })
		void unknownOsNameNeverMatches(@Nullable String osName) {
			assertFalse(osNameMatches(osName, "AmongOS"));
		}

		@Test
		@DefaultLocale("tr-TR")
		void matchingDoesNotDependOnDefaultLocale() {
			// In the Turkish locale, an upper-case I is converted to a dotless lower-case ı.
			assertNotEquals("linux", "LINUX".toLowerCase());

			assertTrue(osNameMatches("LINUX", "linux"));
			assertTrue(osNameMatches("Windows 11", "WINDOWS"));
		}

	}

	@Nested
	class EnabledOnOsWithName {

		@Test
		void enabledIfNameMatchesCurrentOsName() {
			var result = evaluate(enabledOnOs(NO_OPERATING_SYSTEMS, CURRENT_OS_NAME, NO_ARCHITECTURES, ""));

			assertFalse(result.isDisabled());
			assertThat(result.getReason()).contains("Enabled on operating system: " + CURRENT_OS_NAME);
		}

		@Test
		void disabledIfNameDoesNotMatchCurrentOsName() {
			var result = evaluate(enabledOnOs(NO_OPERATING_SYSTEMS, NON_MATCHING_OS_NAME, NO_ARCHITECTURES, ""));

			assertTrue(result.isDisabled());
			assertThat(result.getReason()).contains("Disabled on operating system: " + CURRENT_OS_NAME);
		}

		@Test
		void nameIsMatchedIgnoringCase() {
			for (String name : caseVariantsOfCurrentOsName()) {
				var result = evaluate(enabledOnOs(NO_OPERATING_SYSTEMS, name, NO_ARCHITECTURES, ""));

				assertFalse(result.isDisabled(), () -> "Should be enabled for name: " + name);
			}
		}

		@ParameterizedTest(name = "name matches: {0}, architecture matches: {1}")
		@CsvSource({ "true, true", "true, false", "false, true", "false, false" })
		void nameAndArchitectureMustBothMatch(boolean nameMatches, boolean architectureMatches) {
			var result = evaluate(
				enabledOnOs(NO_OPERATING_SYSTEMS, osName(nameMatches), architectures(architectureMatches), ""));

			boolean expectedEnabled = nameMatches && architectureMatches;
			assertEquals(!expectedEnabled, result.isDisabled());
			assertThat(result.getReason()).contains("%s on operating system: %s (%s)".formatted(
				expectedEnabled ? "Enabled" : "Disabled", CURRENT_OS_NAME, CURRENT_ARCHITECTURE));
		}

		@ParameterizedTest
		@ValueSource(strings = { "", " ", "\t" })
		void blankNameAloneIsRejected(String name) {
			assertPreconditionViolationFor(
				() -> evaluate(enabledOnOs(NO_OPERATING_SYSTEMS, name, NO_ARCHITECTURES, ""))) //
						.withMessage("You must declare at least one OS or architecture in @EnabledOnOs");
		}

		@ParameterizedTest
		@ValueSource(strings = { "", " ", "\t" })
		void blankNameWithArchitectureBehavesLikeArchitectureOnly(String name) {
			var result = evaluate(enabledOnOs(NO_OPERATING_SYSTEMS, name, architectures(true), ""));

			assertFalse(result.isDisabled());
			assertThat(result.getReason()).contains("Enabled on architecture: " + CURRENT_ARCHITECTURE);
		}

		@Test
		void customDisabledReasonIsAppendedOnce() {
			var result = evaluate(
				enabledOnOs(NO_OPERATING_SYSTEMS, NON_MATCHING_OS_NAME, NO_ARCHITECTURES, "Custom reason"));

			assertTrue(result.isDisabled());
			assertThat(result.getReason()).contains(
				"Disabled on operating system: " + CURRENT_OS_NAME + " ==> Custom reason");
		}

		@ParameterizedTest
		@EnumSource(OS.class)
		void osValuesAndNameMustNotBeCombined(OS os) {
			for (String name : List.of(CURRENT_OS_NAME, NON_MATCHING_OS_NAME)) {
				assertPreconditionViolationFor(() -> evaluate(enabledOnOs(new OS[] { os }, name, NO_ARCHITECTURES, ""))) //
						.withMessage(
							"@EnabledOnOs must be configured with either OS enum constants or an OS name, but not both");
			}
		}

	}

	@Nested
	class DisabledOnOsWithName {

		@Test
		void disabledIfNameMatchesCurrentOsName() {
			var result = evaluate(disabledOnOs(NO_OPERATING_SYSTEMS, CURRENT_OS_NAME, NO_ARCHITECTURES, ""));

			assertTrue(result.isDisabled());
			assertThat(result.getReason()).contains("Disabled on operating system: " + CURRENT_OS_NAME);
		}

		@Test
		void enabledIfNameDoesNotMatchCurrentOsName() {
			var result = evaluate(disabledOnOs(NO_OPERATING_SYSTEMS, NON_MATCHING_OS_NAME, NO_ARCHITECTURES, ""));

			assertFalse(result.isDisabled());
			assertThat(result.getReason()).contains("Enabled on operating system: " + CURRENT_OS_NAME);
		}

		@Test
		void nameIsMatchedIgnoringCase() {
			for (String name : caseVariantsOfCurrentOsName()) {
				var result = evaluate(disabledOnOs(NO_OPERATING_SYSTEMS, name, NO_ARCHITECTURES, ""));

				assertTrue(result.isDisabled(), () -> "Should be disabled for name: " + name);
			}
		}

		@ParameterizedTest(name = "name matches: {0}, architecture matches: {1}")
		@CsvSource({ "true, true", "true, false", "false, true", "false, false" })
		void nameAndArchitectureMustBothMatch(boolean nameMatches, boolean architectureMatches) {
			var result = evaluate(
				disabledOnOs(NO_OPERATING_SYSTEMS, osName(nameMatches), architectures(architectureMatches), ""));

			boolean expectedDisabled = nameMatches && architectureMatches;
			assertEquals(expectedDisabled, result.isDisabled());
			assertThat(result.getReason()).contains("%s on operating system: %s (%s)".formatted(
				expectedDisabled ? "Disabled" : "Enabled", CURRENT_OS_NAME, CURRENT_ARCHITECTURE));
		}

		@ParameterizedTest
		@ValueSource(strings = { "", " ", "\t" })
		void blankNameAloneIsRejected(String name) {
			assertPreconditionViolationFor(
				() -> evaluate(disabledOnOs(NO_OPERATING_SYSTEMS, name, NO_ARCHITECTURES, ""))) //
						.withMessage("You must declare at least one OS or architecture in @DisabledOnOs");
		}

		@ParameterizedTest
		@ValueSource(strings = { "", " ", "\t" })
		void blankNameWithArchitectureBehavesLikeArchitectureOnly(String name) {
			var result = evaluate(disabledOnOs(NO_OPERATING_SYSTEMS, name, architectures(true), ""));

			assertTrue(result.isDisabled());
			assertThat(result.getReason()).contains("Disabled on architecture: " + CURRENT_ARCHITECTURE);
		}

		@Test
		void customDisabledReasonIsAppendedOnce() {
			var result = evaluate(
				disabledOnOs(NO_OPERATING_SYSTEMS, CURRENT_OS_NAME, NO_ARCHITECTURES, "Custom reason"));

			assertTrue(result.isDisabled());
			assertThat(result.getReason()).contains(
				"Disabled on operating system: " + CURRENT_OS_NAME + " ==> Custom reason");
		}

		@ParameterizedTest
		@EnumSource(OS.class)
		void osValuesAndNameMustNotBeCombined(OS os) {
			for (String name : List.of(CURRENT_OS_NAME, NON_MATCHING_OS_NAME)) {
				assertPreconditionViolationFor(
					() -> evaluate(disabledOnOs(new OS[] { os }, name, NO_ARCHITECTURES, ""))) //
							.withMessage(
								"@DisabledOnOs must be configured with either OS enum constants or an OS name, but not both");
			}
		}

	}

	@Nested
	class ClassLevelDeclarations {

		@Test
		void enabledOnOsWithNonMatchingNameSkipsTestClass() {
			var testClass = EnabledOnAmongOsTestCase.class;

			var results = executeTestsForClass(testClass);

			results.containerEvents().assertThatEvents().haveExactly(1,
				event(container(testClass), skippedWithReason("Disabled on operating system: " + CURRENT_OS_NAME)));
			results.testEvents().assertStatistics(stats -> stats.started(0));
		}

		@Test
		void disabledOnOsWithNonMatchingNameExecutesTestClass() {
			var results = executeTestsForClass(DisabledOnAmongOsTestCase.class);

			results.containerEvents().assertStatistics(stats -> stats.skipped(0));
			results.testEvents().assertStatistics(stats -> stats.started(1).succeeded(1));
		}

	}

	// -------------------------------------------------------------------------

	private static ConditionEvaluationResult evaluate(EnabledOnOs annotation) {
		return new EnabledOnOsCondition().evaluateExecutionCondition(annotation);
	}

	private static ConditionEvaluationResult evaluate(DisabledOnOs annotation) {
		return new DisabledOnOsCondition().evaluateExecutionCondition(annotation);
	}

	private static EnabledOnOs enabledOnOs(OS[] operatingSystems, String name, String[] architectures,
			String disabledReason) {

		EnabledOnOs annotation = mock();
		when(annotation.value()).thenReturn(operatingSystems);
		when(annotation.name()).thenReturn(name);
		when(annotation.architectures()).thenReturn(architectures);
		when(annotation.disabledReason()).thenReturn(disabledReason);
		return annotation;
	}

	private static DisabledOnOs disabledOnOs(OS[] operatingSystems, String name, String[] architectures,
			String disabledReason) {

		DisabledOnOs annotation = mock();
		when(annotation.value()).thenReturn(operatingSystems);
		when(annotation.name()).thenReturn(name);
		when(annotation.architectures()).thenReturn(architectures);
		when(annotation.disabledReason()).thenReturn(disabledReason);
		return annotation;
	}

	private static String osName(boolean matching) {
		return matching ? CURRENT_OS_NAME : NON_MATCHING_OS_NAME;
	}

	private static String[] architectures(boolean matching) {
		return new String[] { matching ? CURRENT_ARCHITECTURE : NON_MATCHING_ARCHITECTURE };
	}

	private static List<String> caseVariantsOfCurrentOsName() {
		return List.of(CURRENT_OS_NAME.toUpperCase(Locale.ENGLISH), CURRENT_OS_NAME.toLowerCase(Locale.ENGLISH));
	}

	@EnabledOnOs(name = "AmongOS")
	static class EnabledOnAmongOsTestCase {

		@Test
		void test() {
			fail("should be skipped");
		}

	}

	@DisabledOnOs(name = "AmongOS")
	static class DisabledOnAmongOsTestCase {

		@Test
		void test() {
		}

	}

}
