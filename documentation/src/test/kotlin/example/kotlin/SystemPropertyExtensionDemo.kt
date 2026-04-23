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
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.ClassOrderer
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Order
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestClassOrder
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.util.ClearSystemProperty
import org.junit.jupiter.api.util.ReadsSystemProperty
import org.junit.jupiter.api.util.RestoreSystemProperties
import org.junit.jupiter.api.util.SetSystemProperty
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource

class SystemPropertyExtensionDemo {
    // tag::systemproperty_clear_simple[]
    @Test
    @ClearSystemProperty(key = "some property")
    fun testClearingProperty() {
        assertThat(System.getProperty("some property")).isNull()
    }
    // end::systemproperty_clear_simple[]

    // tag::systemproperty_set_simple[]
    @Test
    @SetSystemProperty(key = "some property", value = "new value")
    fun testSettingProperty() {
        assertThat(System.getProperty("some property")).isEqualTo("new value")
    }
    // end::systemproperty_set_simple[]

    // tag::systemproperty_using_set_and_clear[]
    @Test
    @ClearSystemProperty(key = "1st property")
    @ClearSystemProperty(key = "2nd property")
    @SetSystemProperty(key = "3rd property", value = "new value")
    fun testClearingAndSettingProperty() {
        assertThat(System.getProperty("1st property")).isNull()
        assertThat(System.getProperty("2nd property")).isNull()
        assertThat(System.getProperty("3rd property")).isEqualTo("new value")
    }
    // end::systemproperty_using_set_and_clear[]

    // @formatter:off
    @Nested
    // tag::systemproperty_using_at_class_level[]
    @ClearSystemProperty(key = "some property")
    // end::systemproperty_using_at_class_level[]
    inner
    // tag::systemproperty_using_at_class_level[]
    class MySystemPropertyTest {
        // end::systemproperty_using_at_class_level[]
        // @formatter:on
        // tag::systemproperty_using_at_class_level[]

        @Test
        @SetSystemProperty(key = "some property", value = "new value")
        fun clearedAtClasslevel() {
            assertThat(System.getProperty("some property")).isEqualTo("new value")
        }
    }
    // end::systemproperty_using_at_class_level[]

    // tag::systemproperty_restore_test[]
    @ParameterizedTest
    @ValueSource(strings = ["foo", "bar"])
    @RestoreSystemProperties
    fun parameterizedTest(value: String) {
        System.setProperty("some parameterized property", value)
        System.setProperty("some other dynamic property", "my code calculates somehow")
    }
    // end::systemproperty_restore_test[]

    @Nested
    @TestClassOrder(ClassOrderer.OrderAnnotation::class)
    inner class SystemPropertyRestoreExample {
        // @formatter:off
        @Nested
        @Order(1)
        // tag::systemproperty_class_restore_setup[]
        @TestInstance(TestInstance.Lifecycle.PER_CLASS)
        @RestoreSystemProperties
        // end::systemproperty_class_restore_setup[]
        inner
        // tag::systemproperty_class_restore_setup[]
        class MySystemPropertyRestoreTest {
            // end::systemproperty_class_restore_setup[]
            // @formatter:on
            // tag::systemproperty_class_restore_setup[]
            @BeforeAll
            fun beforeAll() {
                System.setProperty("A", "A value")
            }

            @BeforeEach
            fun beforeEach() {
                System.setProperty("B", "B value")
            }

            @Test
            fun isolatedTest1() {
                System.setProperty("C", "C value")
            }

            @Test
            fun isolatedTest2() {
                assertThat(System.getProperty("A")).isEqualTo("A value")
                assertThat(System.getProperty("B")).isEqualTo("B value")

                // Class-level @RestoreSystemProperties restores "C" to original state
                assertThat(System.getProperty("C")).isNull()
            }
        }
        // end::systemproperty_class_restore_setup[]

        // @formatter:off
        // tag::systemproperty_class_restore_isolated_class[]
        @Nested
        @Order(2)
        @ReadsSystemProperty
        // end::systemproperty_class_restore_isolated_class[]
        inner
        // tag::systemproperty_class_restore_isolated_class[]
        class SomeOtherTestClass {
            // end::systemproperty_class_restore_isolated_class[]
            // @formatter:on
            // tag::systemproperty_class_restore_isolated_class[]
            @Test
            fun isolatedTest() {
                assertThat(System.getProperty("A")).isNull()
                assertThat(System.getProperty("B")).isNull()
                assertThat(System.getProperty("C")).isNull()
            }
        }
        // end::systemproperty_class_restore_isolated_class[]
    }

    // tag::systemproperty_method_combine_all_test[]
    @ParameterizedTest
    @ValueSource(ints = [100, 500, 1000])
    @RestoreSystemProperties
    @SetSystemProperty(key = "DISABLE_CACHE", value = "TRUE")
    @ClearSystemProperty(key = "COPYWRITE_OVERLAY_TEXT")
    fun imageGenerationTest(imageSize: Int) {
        System.setProperty("IMAGE_SIZE", "$imageSize") // Requires restore

        // Test your image generation utility with the current system properties
    }
    // end::systemproperty_method_combine_all_test[]
}
