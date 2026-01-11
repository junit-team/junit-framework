/*
 * Copyright 2015-2026 the original author or authors.
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v2.0 which
 * accompanies this distribution and is available at
 *
 * https://www.eclipse.org/legal/epl-v20.html
 */

package org.junit.jupiter.api.util;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.platform.engine.discovery.DiscoverySelectors.selectMethod;
import static org.junit.platform.testkit.engine.EventConditions.finishedWithFailure;
import static org.junit.platform.testkit.engine.TestExecutionResultConditions.instanceOf;
import static org.junit.platform.testkit.engine.TestExecutionResultConditions.message;

import java.util.TimeZone;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.extension.ExtensionConfigurationException;
import org.junit.jupiter.engine.AbstractJupiterTestEngineTests;
import org.junit.platform.testkit.engine.EngineExecutionResults;

@DisplayName("DefaultTimeZone extension")
class DefaultTimeZoneTests extends AbstractJupiterTestEngineTests {

	private static TimeZone TEST_DEFAULT_TIMEZONE;
	private static TimeZone DEFAULT_TIMEZONE_BEFORE_TEST;

	@BeforeAll
	static void globalSetUp() {
		// we set UTC as test time zone unless it is already
		// the system's time zone; in that case we use UTC+12
		DEFAULT_TIMEZONE_BEFORE_TEST = TimeZone.getDefault();
		TimeZone utc = TimeZone.getTimeZone("UTC");
		TimeZone utcPlusTwelve = TimeZone.getTimeZone("GMT+12:00");
		if (DEFAULT_TIMEZONE_BEFORE_TEST.equals(utc))
			TimeZone.setDefault(utcPlusTwelve);
		else
			TimeZone.setDefault(utc);
		TEST_DEFAULT_TIMEZONE = TimeZone.getDefault();
	}

	@AfterAll
	static void globalTearDown() {
		TimeZone.setDefault(DEFAULT_TIMEZONE_BEFORE_TEST);
	}

	@Nested
	@DisplayName("when applied on the method level")
	class MethodLevelTests {

		@Test
		@ReadsDefaultTimeZone
		@DisplayName("does nothing when annotation is not present")
		void doesNothingWhenAnnotationNotPresent() {
			assertThat(TimeZone.getDefault()).isEqualTo(TEST_DEFAULT_TIMEZONE);
		}

		@Test
		@DefaultTimeZone("GMT")
		@DisplayName("does not throw when explicitly set to GMT")
		void doesNotThrowForExplicitGmt() {
			assertThat(TimeZone.getDefault()).isEqualTo(TimeZone.getTimeZone("GMT"));
		}

		@Test
		@DefaultTimeZone("CET")
		@DisplayName("sets the default time zone using an abbreviation")
		void setsTimeZoneFromAbbreviation() {
			assertThat(TimeZone.getDefault()).isEqualTo(TimeZone.getTimeZone("CET"));
		}

		@Test
		@DefaultTimeZone("America/Los_Angeles")
		@DisplayName("sets the default time zone using a full name")
		void setsTimeZoneFromFullName() {
			assertThat(TimeZone.getDefault()).isEqualTo(TimeZone.getTimeZone("America/Los_Angeles"));
		}

	}

	@Nested
	@DefaultTimeZone("GMT-8:00")
	@DisplayName("when applied on the class level")
	class ClassLevelTestCases {

		@Test
		@ReadsDefaultTimeZone
		@DisplayName("sets the default time zone")
		void shouldExecuteWithClassLevelTimeZone() {
			assertThat(TimeZone.getDefault()).isEqualTo(TimeZone.getTimeZone("GMT-8:00"));
		}

		@Test
		@DefaultTimeZone("GMT-12:00")
		@DisplayName("gets overridden by annotation on the method level")
		void shouldBeOverriddenWithMethodLevelTimeZone() {
			assertThat(TimeZone.getDefault()).isEqualTo(TimeZone.getTimeZone("GMT-12:00"));
		}

	}

	@Nested
	@DefaultTimeZone("GMT")
	@DisplayName("when explicitly set to GMT on the class level")
	class ExplicitGmtClassLevelTestCases {

		@Test
		@DisplayName("does not throw and sets to GMT ")
		void explicitGmt() {
			assertThat(TimeZone.getDefault()).isEqualTo(TimeZone.getTimeZone("GMT"));
		}

	}

	@Nested
	@DefaultTimeZone("GMT-8:00")
	@DisplayName("with nested classes")
	class NestedTests {

		@Nested
		@DisplayName("without DefaultTimeZone annotation")
		class NestedClass {

			@Test
			@ReadsDefaultTimeZone
			@DisplayName("DefaultTimeZone should be set from enclosed class when it is not provided in nested")
			public void shouldSetTimeZoneFromEnclosedClass() {
				assertThat(TimeZone.getDefault()).isEqualTo(TimeZone.getTimeZone("GMT-8:00"));
			}

		}

		@Nested
		@DefaultTimeZone("GMT-12:00")
		@DisplayName("with DefaultTimeZone annotation")
		class AnnotatedNestedClass {

			@Test
			@ReadsDefaultTimeZone
			@DisplayName("DefaultTimeZone should be set from nested class when it is provided")
			public void shouldSetTimeZoneFromNestedClass() {
				assertThat(TimeZone.getDefault()).isEqualTo(TimeZone.getTimeZone("GMT-12:00"));
			}

			@Test
			@DefaultTimeZone("GMT-6:00")
			@DisplayName("DefaultTimeZone should be set from method when it is provided")
			public void shouldSetTimeZoneFromMethodOfNestedClass() {
				assertThat(TimeZone.getDefault()).isEqualTo(TimeZone.getTimeZone("GMT-6:00"));
			}

		}

	}

	@Nested
	@DefaultTimeZone("GMT-12:00")
	@TestInstance(TestInstance.Lifecycle.PER_CLASS)
	class ResettingDefaultTimeZoneTests {

		@Nested
		@DefaultTimeZone("GMT-3:00")
		@TestInstance(TestInstance.Lifecycle.PER_CLASS)
		class ResettingDefaultTimeZoneNestedTests {

			@Test
			@DefaultTimeZone("GMT+6:00")
			void setForTestMethod() {
				// only here to set the time zone, so another test can verify whether it was reset;
				// still, better to assert the value was actually set
				assertThat(TimeZone.getDefault()).isEqualTo(TimeZone.getTimeZone("GMT+6:00"));
			}

			@AfterAll
			@ReadsDefaultTimeZone
			void resetAfterTestMethodExecution() {
				assertThat(TimeZone.getDefault()).isEqualTo(TEST_DEFAULT_TIMEZONE);
			}

		}

		@AfterAll
		@ReadsDefaultTimeZone
		void resetAfterTestMethodExecution() {
			assertThat(TimeZone.getDefault()).isEqualTo(TEST_DEFAULT_TIMEZONE);
		}

	}

	@Nested
	@DisplayName("when misconfigured")
	class ConfigurationTests {

		@Test
		@ReadsDefaultTimeZone
		@DisplayName("on method level, throws exception")
		void throwsWhenConfigurationIsBad() {
			EngineExecutionResults results = executeTests(
				selectMethod(BadMethodLevelConfigurationTestCases.class, "badConfiguration"));
			results.testEvents().assertThatEvents().haveAtMost(1,
				finishedWithFailure(instanceOf(ExtensionConfigurationException.class),
					message(it -> it.contains("@DefaultTimeZone not configured correctly."))));
		}

		@Test
		@ReadsDefaultTimeZone
		@DisplayName("on class level, throws exception")
		void shouldThrowWithBadConfiguration() {
			EngineExecutionResults results = executeTestsForClass(BadClassLevelConfigurationTestCases.class);

			results.testEvents().assertThatEvents().haveAtMost(1,
				finishedWithFailure(instanceOf(ExtensionConfigurationException.class),
					message(it -> it.contains("@DefaultTimeZone not configured correctly."))));
		}

		@AfterEach
		void verifyMisconfigurationSisNotChangeTimeZone() {
			assertThat(TimeZone.getDefault()).isEqualTo(TEST_DEFAULT_TIMEZONE);
		}

	}

	static class BadMethodLevelConfigurationTestCases {

		@Test
		@DefaultTimeZone("Gibberish")
		void badConfiguration() {
		}

	}

	@DefaultTimeZone("Gibberish")
	static class BadClassLevelConfigurationTestCases {

		@Test
		void badConfiguration() {
		}

	}

	@Nested
	@DisplayName("used with inheritance")
	class InheritanceTests extends InheritanceBaseTest {

		@Test
		@DisplayName("should inherit default time zone annotation")
		void shouldInheritClearAndSetProperty() {
			assertThat(TimeZone.getDefault()).isEqualTo(TimeZone.getTimeZone("GMT-8:00"));
		}

	}

	@DefaultTimeZone("GMT-8:00")
	static class InheritanceBaseTest {

	}

	@Nested
	@DisplayName("used with TimeZoneProvider")
	class ProviderTests {

		@Test
		@DisplayName("can get a basic time zone")
		@DefaultTimeZone(timeZoneProvider = BasicTimeZoneProvider.class)
		void canGetBasicTimeZone() {
			assertThat(TimeZone.getDefault()).isEqualTo(TimeZone.getTimeZone("Europe/Prague"));
		}

		@Test
		@DisplayName("defaults to GMT if the provider returns null")
		@DefaultTimeZone(timeZoneProvider = NullProvider.class)
		void defaultToGmt() {
			assertThat(TimeZone.getDefault()).isEqualTo(TimeZone.getTimeZone("GMT"));
		}

		@Test
		@ReadsDefaultTimeZone
		@DisplayName("throws ExtensionConfigurationException if the provider is not the only option")
		void throwsForMutuallyExclusiveOptions() {
			EngineExecutionResults results = executeTests(
				selectMethod(BadTimeZoneProviderTestCases.class, "notExclusive"));

			results.testEvents().assertThatEvents().haveAtMost(1,
				finishedWithFailure(instanceOf(ExtensionConfigurationException.class),
					message(it -> it.contains("Either a valid time zone id or a TimeZoneProvider must be provided"))));
		}

		@Test
		@ReadsDefaultTimeZone
		@DisplayName("throws ExtensionConfigurationException if properties are empty")
		void throwsForEmptyOptions() {
			EngineExecutionResults results = executeTests(selectMethod(BadTimeZoneProviderTestCases.class, "empty"));

			results.testEvents().assertThatEvents().haveAtMost(1,
				finishedWithFailure(instanceOf(ExtensionConfigurationException.class),
					message(it -> it.contains("Either a valid time zone id or a TimeZoneProvider must be provided"))));
		}

		@Test
		@ReadsDefaultTimeZone
		@DisplayName("throws ExtensionConfigurationException if the provider does not have a suitable constructor")
		void throwsForBadConstructor() {
			EngineExecutionResults results = executeTests(
				selectMethod(BadTimeZoneProviderTestCases.class, "noConstructor"));

			results.testEvents().assertThatEvents().haveAtMost(1,
				finishedWithFailure(instanceOf(ExtensionConfigurationException.class),
					message(it -> it.contains("Could not instantiate TimeZoneProvider because of exception"))));
		}

	}

	static class BadTimeZoneProviderTestCases {

		@Test
		@DefaultTimeZone(value = "GMT", timeZoneProvider = BasicTimeZoneProvider.class)
		void notExclusive() {
			// can't have both a time zone value and a provider
		}

		@Test
		@DefaultTimeZone
		void empty() {
			// must have a provider or a time zone
		}

		@Test
		@DefaultTimeZone(timeZoneProvider = ComplicatedProvider.class)
		void noConstructor() {
			// provider has to have a no-args constructor
		}

	}

	static class BasicTimeZoneProvider implements TimeZoneProvider {

		@Override
		public TimeZone get() {
			return TimeZone.getTimeZone("Europe/Prague");
		}

	}

	static class NullProvider implements TimeZoneProvider {

		@Override
		@SuppressWarnings("NullAway")
		public TimeZone get() {
			return null;
		}

	}

	static class ComplicatedProvider implements TimeZoneProvider {

		private final String timeZoneString;

		ComplicatedProvider(String timeZoneString) {
			this.timeZoneString = timeZoneString;
		}

		@Override
		public TimeZone get() {
			return TimeZone.getTimeZone(timeZoneString);
		}

	}

}
