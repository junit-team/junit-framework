/*
 * Copyright 2015-2026 the original author or authors.
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v2.0 which
 * accompanies this distribution and is available at
 *
 * https://www.eclipse.org/legal/epl-v20.html
 */

package example.kotlin

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.util.DefaultLocale
import org.junit.jupiter.api.util.DefaultTimeZone
import org.junit.jupiter.api.util.LocaleProvider
import org.junit.jupiter.api.util.TimeZoneProvider
import java.time.ZoneOffset
import java.util.Locale
import java.util.TimeZone

class DefaultLocaleTimezoneExtensionDemo {
    // tag::default_locale_language[]
    @Test
    @DefaultLocale("zh-Hant-TW")
    fun test_with_language() {
        assertThat(Locale.getDefault()).isEqualTo(Locale.forLanguageTag("zh-Hant-TW"))
    }
    // end::default_locale_language[]

    // tag::default_locale_language_alternatives[]
    @Test
    @DefaultLocale(language = "en")
    fun test_with_language_only() {
        assertThat(Locale.getDefault()).isEqualTo(Locale.Builder().setLanguage("en").build())
    }

    @Test
    @DefaultLocale(language = "en", country = "EN")
    fun test_with_language_and_country() {
        assertThat(Locale.getDefault()).isEqualTo(
            Locale
                .Builder()
                .setLanguage("en")
                .setRegion("EN")
                .build()
        )
    }

    @Test
    @DefaultLocale(language = "ja", country = "JP", variant = "japanese")
    fun test_with_language_and_country_and_vairant() {
        assertThat(Locale.getDefault()).isEqualTo(
            Locale
                .Builder()
                .setLanguage("ja")
                .setRegion("JP")
                .setVariant("japanese")
                .build()
        )
    }
    // end::default_locale_language_alternatives[]

    // @formatter:off
    @Nested
    // tag::default_locale_class_level[]
    @DefaultLocale(language = "fr")
    // end::default_locale_class_level[]
    inner
    // tag::default_locale_class_level[]
    class MyLocaleTests {
        // end::default_locale_class_level[]
        // @formatter:on
        // tag::default_locale_class_level[]
        @Test
        fun test_with_class_level_configuration() {
            assertThat(Locale.getDefault()).isEqualTo(Locale.Builder().setLanguage("fr").build())
        }

        @Test
        @DefaultLocale(language = "en")
        fun test_with_method_level_configuration() {
            assertThat(Locale.getDefault()).isEqualTo(Locale.Builder().setLanguage("en").build())
        }
    }
    // end::default_locale_class_level[]

    // tag::default_locale_with_provider[]
    @Test
    @DefaultLocale(localeProvider = EnglishProvider::class)
    fun test_with_locale_provider() {
        assertThat(Locale.getDefault()).isEqualTo(Locale.Builder().setLanguage("en").build())
    }

    class EnglishProvider : LocaleProvider {
        override fun get(): Locale = Locale.ENGLISH
    }
    // end::default_locale_with_provider[]

    // tag::default_timezone_zone[]
    @Test
    @DefaultTimeZone("CET")
    fun test_with_short_zone_id() {
        assertThat(TimeZone.getDefault()).isEqualTo(TimeZone.getTimeZone("CET"))
    }

    @Test
    @DefaultTimeZone("Africa/Juba")
    fun test_with_long_zone_id() {
        assertThat(TimeZone.getDefault()).isEqualTo(TimeZone.getTimeZone("Africa/Juba"))
    }
    // end::default_timezone_zone[]

    // @formatter:off
    @Nested
    // tag::default_timezone_class_level[]
    @DefaultTimeZone("CET")
    // end::default_timezone_class_level[]
    inner
    // tag::default_timezone_class_level[]
    class MyTimeZoneTests {
        // end::default_timezone_class_level[]
        // @formatter:on
        // tag::default_timezone_class_level[]

        @Test
        fun test_with_class_level_configuration() {
            assertThat(TimeZone.getDefault()).isEqualTo(TimeZone.getTimeZone("CET"))
        }

        @Test
        @DefaultTimeZone("Africa/Juba")
        fun test_with_method_level_configuration() {
            assertThat(TimeZone.getDefault()).isEqualTo(TimeZone.getTimeZone("Africa/Juba"))
        }
    }
    // end::default_timezone_class_level[]

    // tag::default_time_zone_with_provider[]
    @Test
    @DefaultTimeZone(timeZoneProvider = UtcTimeZoneProvider::class)
    fun test_with_time_zone_provider() {
        assertThat(TimeZone.getDefault()).isEqualTo(TimeZone.getTimeZone("UTC"))
    }

    class UtcTimeZoneProvider : TimeZoneProvider {
        override fun get(): TimeZone = TimeZone.getTimeZone(ZoneOffset.UTC)
    }
    // end::default_time_zone_with_provider[]
}
