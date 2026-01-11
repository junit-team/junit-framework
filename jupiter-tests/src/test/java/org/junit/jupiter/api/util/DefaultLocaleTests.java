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
import static org.junit.platform.testkit.engine.EventConditions.finishedSuccessfully;
import static org.junit.platform.testkit.engine.EventConditions.finishedWithFailure;
import static org.junit.platform.testkit.engine.TestExecutionResultConditions.instanceOf;
import static org.junit.platform.testkit.engine.TestExecutionResultConditions.message;

import java.util.Locale;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.extension.ExtensionConfigurationException;
import org.junit.jupiter.engine.AbstractJupiterTestEngineTests;
import org.junit.platform.testkit.engine.EngineExecutionResults;

@DisplayName("DefaultLocale extension")
class DefaultLocaleTests extends AbstractJupiterTestEngineTests {

	private static Locale TEST_DEFAULT_LOCALE;
	private static Locale DEFAULT_LOCALE_BEFORE_TEST;

	@BeforeAll
	static void globalSetUp() {
		DEFAULT_LOCALE_BEFORE_TEST = Locale.getDefault();
		TEST_DEFAULT_LOCALE = JupiterLocaleUtils.createLocale("custom");
		Locale.setDefault(TEST_DEFAULT_LOCALE);
	}

	@AfterAll
	static void globalTearDown() {
		Locale.setDefault(DEFAULT_LOCALE_BEFORE_TEST);
	}

	@Nested
	@DisplayName("applied on the method level")
	class MethodLevelTests {

		@Test
		@ReadsDefaultLocale
		@DisplayName("does nothing when annotation is not present")
		void testDefaultLocaleNoAnnotation() {
			assertThat(Locale.getDefault()).isEqualTo(TEST_DEFAULT_LOCALE);
		}

		@Test
		@DefaultLocale("zh-Hant-TW")
		@DisplayName("sets the default locale using a language tag")
		void setsLocaleViaLanguageTag() {
			assertThat(Locale.getDefault()).isEqualTo(Locale.forLanguageTag("zh-Hant-TW"));
		}

		@Test
		@DefaultLocale(language = "en")
		@DisplayName("sets the default locale using a language")
		void setsLanguage() {
			assertThat(Locale.getDefault()).isEqualTo(JupiterLocaleUtils.createLocale("en"));
		}

		@Test
		@DefaultLocale(language = "en", country = "EN")
		@DisplayName("sets the default locale using a language and a country")
		void setsLanguageAndCountry() {
			assertThat(Locale.getDefault()).isEqualTo(JupiterLocaleUtils.createLocale("en", "EN"));
		}

		/**
		 * A valid variant checked by {@link sun.util.locale.LanguageTag#isVariant} against BCP 47 (or more detailed RFC 5646) matches either {@code [0-9a-Z]{5-8}} or {@code [0-9][0-9a-Z]{3}}.
		 * It does NOT check if such a variant exists in real.
		 * <br>
		 * The Locale-Builder accepts valid variants, concatenated by minus or underscore (minus will be transformed by the builder).
		 * This means "en-EN" is a valid languageTag, but not a valid IETF BCP 47 variant subtag.
		 * <br>
		 * This is very confusing as the <a href="https://www.oracle.com/java/technologies/javase/jdk11-suported-locales.html">official page for supported locales</a> shows that japanese locales return {@code *} or {@code JP} as a variant.
		 * Even more confusing the enum values {@code Locale.JAPAN} and {@code Locale.JAPANESE} don't return a variant.
		 *
		 * @see <a href="https://www.rfc-editor.org/rfc/rfc5646.html">RFC 5646</a>
		 */
		@Test
		@DefaultLocale(language = "ja", country = "JP", variant = "japanese")
		@DisplayName("sets the default locale using a language, a country and a variant")
		void setsLanguageAndCountryAndVariant() {
			assertThat(Locale.getDefault()).isEqualTo(JupiterLocaleUtils.createLocale("ja", "JP", "japanese"));
		}

	}

	@Test
	@WritesDefaultLocale
	@DisplayName("applied on the class level, should execute tests with configured Locale")
	void shouldExecuteTestsWithConfiguredLocale() {
		EngineExecutionResults results = executeTestsForClass(ClassLevelTestCases.class);

		results.testEvents().assertThatEvents().haveAtMost(2, finishedSuccessfully());
	}

	@DefaultLocale(language = "fr", country = "FR")
	static class ClassLevelTestCases {

		@Test
		@ReadsDefaultLocale
		void shouldExecuteWithClassLevelLocale() {
			assertThat(Locale.getDefault()).isEqualTo(JupiterLocaleUtils.createLocale("fr", "FR"));
		}

		@Test
		@DefaultLocale(language = "de", country = "DE")
		void shouldBeOverriddenWithMethodLevelLocale() {
			assertThat(Locale.getDefault()).isEqualTo(JupiterLocaleUtils.createLocale("de", "DE"));
		}

	}

	@Nested
	@DefaultLocale(language = "en")
	@DisplayName("with nested classes")
	class NestedDefaultLocaleTests {

		@Nested
		@DisplayName("without DefaultLocale annotation")
		class NestedClass {

			@Test
			@DisplayName("DefaultLocale should be set from enclosed class when it is not provided in nested")
			void shouldSetLocaleFromEnclosedClass() {
				assertThat(Locale.getDefault().getLanguage()).isEqualTo("en");
			}

		}

		@Nested
		@DefaultLocale(language = "de")
		@DisplayName("with DefaultLocale annotation")
		class AnnotatedNestedClass {

			@Test
			@DisplayName("DefaultLocale should be set from nested class when it is provided")
			void shouldSetLocaleFromNestedClass() {
				assertThat(Locale.getDefault().getLanguage()).isEqualTo("de");
			}

			@Test
			@DefaultLocale(language = "ch")
			@DisplayName("DefaultLocale should be set from method when it is provided")
			void shouldSetLocaleFromMethodOfNestedClass() {
				assertThat(Locale.getDefault().getLanguage()).isEqualTo("ch");
			}

		}

	}

	@Nested
	@DefaultLocale(language = "fi")
	@TestInstance(TestInstance.Lifecycle.PER_CLASS)
	@DisplayName("correctly sets/resets before/after each/all extension points")
	class ResettingDefaultLocaleTests {

		@Nested
		@DefaultLocale(language = "de")
		@TestInstance(TestInstance.Lifecycle.PER_CLASS)
		class ResettingDefaultLocaleNestedTests {

			@Test
			@DefaultLocale(language = "en")
			void setForTestMethod() {
				// only here to set the locale, so another test can verify whether it was reset;
				// still, better to assert the value was actually set
				assertThat(Locale.getDefault().getLanguage()).isEqualTo("en");
			}

			@AfterAll
			@ReadsDefaultLocale
			void resetAfterTestMethodExecution() {
				assertThat(Locale.getDefault().getLanguage()).isEqualTo("custom");
			}

		}

		@AfterAll
		@ReadsDefaultLocale
		void resetAfterTestMethodExecution() {
			assertThat(Locale.getDefault().getLanguage()).isEqualTo("custom");
		}

	}

	@DefaultLocale(language = "en")
	static class ClassLevelResetTestCase {

		@Test
		void setForTestMethod() {
			// only here to set the locale, so another test can verify whether it was reset;
			// still, better to assert the value was actually set
			assertThat(Locale.getDefault().getLanguage()).isEqualTo("en");
		}

	}

	@Nested
	@DisplayName("when configured incorrect")
	class ConfigurationFailureTests {

		@Nested
		@DisplayName("on the method level")
		class MethodLevel {

			@Test
			@DisplayName("should fail when nothing is configured")
			void shouldFailWhenNothingIsConfigured() {
				EngineExecutionResults results = executeTests(
					selectMethod(MethodLevelInitializationFailureTestCases.class, "shouldFailMissingConfiguration"));

				results.testEvents().assertThatEvents().haveAtMost(1,
					finishedWithFailure(instanceOf(ExtensionConfigurationException.class)));
			}

			@Test
			@DisplayName("should fail when variant is set but country is not")
			void shouldFailWhenVariantIsSetButCountryIsNot() {
				EngineExecutionResults results = executeTests(
					selectMethod(MethodLevelInitializationFailureTestCases.class, "shouldFailMissingCountry"));

				results.testEvents().assertThatEvents().haveAtMost(1,
					finishedWithFailure(instanceOf(ExtensionConfigurationException.class)));
			}

			@Test
			@DisplayName("should fail when languageTag and language is set")
			void shouldFailWhenLanguageTagAndLanguageIsSet() {
				EngineExecutionResults results = executeTests(
					selectMethod(MethodLevelInitializationFailureTestCases.class, "shouldFailLanguageTagAndLanguage"));

				results.testEvents().assertThatEvents().haveAtMost(1,
					finishedWithFailure(instanceOf(ExtensionConfigurationException.class)));
			}

			@Test
			@DisplayName("should fail when languageTag and country is set")
			void shouldFailWhenLanguageTagAndCountryIsSet() {
				EngineExecutionResults results = executeTests(
					selectMethod(MethodLevelInitializationFailureTestCases.class, "shouldFailLanguageTagAndCountry"));

				results.testEvents().assertThatEvents().haveAtMost(1,
					finishedWithFailure(instanceOf(ExtensionConfigurationException.class)));
			}

			@Test
			@DisplayName("should fail when languageTag and variant is set")
			void shouldFailWhenLanguageTagAndVariantIsSet() {
				EngineExecutionResults results = executeTests(
					selectMethod(MethodLevelInitializationFailureTestCases.class, "shouldFailLanguageTagAndVariant"));

				results.testEvents().assertThatEvents().haveAtMost(1,
					finishedWithFailure(instanceOf(ExtensionConfigurationException.class)));
			}

			@Test
			@DisplayName("should fail when invalid BCP 47 variant is set")
			void shouldFailIfNoValidBCP47VariantIsSet() {
				EngineExecutionResults results = executeTests(
					selectMethod(MethodLevelInitializationFailureTestCases.class, "shouldFailNoValidBCP47Variant"));

				results.testEvents().assertThatEvents().haveAtMost(1,
					finishedWithFailure(instanceOf(ExtensionConfigurationException.class)));
			}

		}

		@Nested
		@DisplayName("on the class level")
		class ClassLevel {

			@Test
			@DisplayName("should fail when variant is set but country is not")
			void shouldFailWhenVariantIsSetButCountryIsNot() {
				EngineExecutionResults results = executeTestsForClass(ClassLevelInitializationFailureTestCases.class);

				results.testEvents().assertThatEvents().haveAtMost(1,
					finishedWithFailure(instanceOf(ExtensionConfigurationException.class)));
			}

		}

	}

	static class MethodLevelInitializationFailureTestCases {

		@Test
		@DefaultLocale
		void shouldFailMissingConfiguration() {
		}

		@Test
		@DefaultLocale(language = "de", variant = "ch")
		void shouldFailMissingCountry() {
		}

		@Test
		@DefaultLocale(value = "Something", language = "de")
		void shouldFailLanguageTagAndLanguage() {
		}

		@Test
		@DefaultLocale(value = "Something", country = "DE")
		void shouldFailLanguageTagAndCountry() {
		}

		@Test
		@DefaultLocale(value = "Something", variant = "ch")
		void shouldFailLanguageTagAndVariant() {
		}

		@Test
		@DefaultLocale(variant = "en-GB")
		void shouldFailNoValidBCP47Variant() {
		}

	}

	@DefaultLocale(language = "de", variant = "ch")
	static class ClassLevelInitializationFailureTestCases {

		@Test
		void shouldFail() {
		}

	}

	@Nested
	@DisplayName("used with inheritance")
	class InheritanceTests extends InheritanceBaseTest {

		@Test
		@DisplayName("should inherit default locale annotation")
		void shouldInheritClearAndSetProperty() {
			assertThat(Locale.getDefault()).isEqualTo(JupiterLocaleUtils.createLocale("fr", "FR"));
		}

	}

	@DefaultLocale(language = "fr", country = "FR")
	static class InheritanceBaseTest {

	}

	@Nested
	@DisplayName("when used with a locale provider")
	class LocaleProviderTests {

		@Test
		@DisplayName("can get a basic locale from provider")
		@DefaultLocale(localeProvider = BasicLocaleProvider.class)
		void canUseProvider() {
			assertThat(Locale.getDefault()).isEqualTo(Locale.FRENCH);
		}

		@Test
		@ReadsDefaultLocale
		@DisplayName("throws a NullPointerException with custom message if provider returns null")
		void providerReturnsNull() {
			EngineExecutionResults results = executeTests(selectMethod(BadProviderTestCases.class, "returnsNull"));

			results.testEvents().assertThatEvents().haveAtMost(1,
				finishedWithFailure(instanceOf(NullPointerException.class),
					message(it -> it.contains("LocaleProvider instance returned with null"))));
		}

		@Test
		@ReadsDefaultLocale
		@DisplayName("throws an ExtensionConfigurationException if any other option is present")
		void mutuallyExclusiveWithValue() {
			EngineExecutionResults results = executeTests(
				selectMethod(BadProviderTestCases.class, "mutuallyExclusiveWithValue"));

			results.testEvents().assertThatEvents().haveAtMost(1,
				finishedWithFailure(instanceOf(ExtensionConfigurationException.class), message(it -> it.contains(
					"can only be used with a provider if value, language, country and variant are not set."))));
		}

		@Test
		@ReadsDefaultLocale
		@DisplayName("throws an ExtensionConfigurationException if any other option is present")
		void mutuallyExclusiveWithLanguage() {
			EngineExecutionResults results = executeTests(
				selectMethod(BadProviderTestCases.class, "mutuallyExclusiveWithLanguage"));

			results.testEvents().assertThatEvents().haveAtMost(1,
				finishedWithFailure(instanceOf(ExtensionConfigurationException.class),
					message(it -> it.contains("can only be used with language tag if provider is not set."))));
		}

		@Test
		@ReadsDefaultLocale
		@DisplayName("throws an ExtensionConfigurationException if any other option is present")
		void mutuallyExclusiveWithCountry() {
			EngineExecutionResults results = executeTests(
				selectMethod(BadProviderTestCases.class, "mutuallyExclusiveWithCountry"));

			results.testEvents().assertThatEvents().haveAtMost(1,
				finishedWithFailure(instanceOf(ExtensionConfigurationException.class), message(it -> it.contains(
					"can only be used with a provider if value, language, country and variant are not set."))));
		}

		@Test
		@ReadsDefaultLocale
		@DisplayName("throws an ExtensionConfigurationException if any other option is present")
		void mutuallyExclusiveWithVariant() {
			EngineExecutionResults results = executeTests(
				selectMethod(BadProviderTestCases.class, "mutuallyExclusiveWithVariant"));

			results.testEvents().assertThatEvents().haveAtMost(1,
				finishedWithFailure(instanceOf(ExtensionConfigurationException.class), message(it -> it.contains(
					"can only be used with a provider if value, language, country and variant are not set."))));
		}

		@Test
		@ReadsDefaultLocale
		@DisplayName("throws an ExtensionConfigurationException if localeProvider can't be constructed")
		void badConstructor() {
			EngineExecutionResults results = executeTests(selectMethod(BadProviderTestCases.class, "badConstructor"));

			results.testEvents().assertThatEvents().haveAtMost(1,
				finishedWithFailure(instanceOf(ExtensionConfigurationException.class),
					message(it -> it.contains("could not be constructed because of an exception"))));
		}

	}

	static class BadProviderTestCases {

		@Test
		@DefaultLocale(value = "en", localeProvider = BasicLocaleProvider.class)
		void mutuallyExclusiveWithValue() {
			// can't have both a value and a provider
		}

		@Test
		@DefaultLocale(language = "en", localeProvider = BasicLocaleProvider.class)
		void mutuallyExclusiveWithLanguage() {
			// can't have both a language property and a provider
		}

		@Test
		@DefaultLocale(country = "EN", localeProvider = BasicLocaleProvider.class)
		void mutuallyExclusiveWithCountry() {
			// can't have both a country property and a provider
		}

		@Test
		@DefaultLocale(variant = "japanese", localeProvider = BasicLocaleProvider.class)
		void mutuallyExclusiveWithVariant() {
			// can't have both a variant property and a provider
		}

		@Test
		@DefaultLocale(localeProvider = ReturnsNullLocaleProvider.class)
		void returnsNull() {
			// provider should not return 'null'
		}

		@Test
		@DefaultLocale(localeProvider = BadConstructorLocaleProvider.class)
		void badConstructor() {
			// provider has to have a no-args constructor
		}

	}

	static class BasicLocaleProvider implements LocaleProvider {

		@Override
		public Locale get() {
			return Locale.FRENCH;
		}

	}

	static class ReturnsNullLocaleProvider implements LocaleProvider {

		@Override
		@SuppressWarnings("NullAway")
		public Locale get() {
			return null;
		}

	}

	static class BadConstructorLocaleProvider implements LocaleProvider {

		private final String language;

		BadConstructorLocaleProvider(String language) {
			this.language = language;
		}

		@Override
		public Locale get() {
			return Locale.forLanguageTag(language);
		}

	}

}
