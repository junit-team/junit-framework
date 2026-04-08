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

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.condition.DisabledForJreRange
import org.junit.jupiter.api.condition.DisabledIf
import org.junit.jupiter.api.condition.DisabledIfEnvironmentVariable
import org.junit.jupiter.api.condition.DisabledIfSystemProperty
import org.junit.jupiter.api.condition.DisabledInNativeImage
import org.junit.jupiter.api.condition.DisabledOnJre
import org.junit.jupiter.api.condition.DisabledOnOs
import org.junit.jupiter.api.condition.EnabledForJreRange
import org.junit.jupiter.api.condition.EnabledIf
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable
import org.junit.jupiter.api.condition.EnabledIfSystemProperty
import org.junit.jupiter.api.condition.EnabledInNativeImage
import org.junit.jupiter.api.condition.EnabledOnJre
import org.junit.jupiter.api.condition.EnabledOnOs
import org.junit.jupiter.api.condition.JRE.JAVA_17
import org.junit.jupiter.api.condition.JRE.JAVA_18
import org.junit.jupiter.api.condition.JRE.JAVA_19
import org.junit.jupiter.api.condition.JRE.JAVA_21
import org.junit.jupiter.api.condition.JRE.JAVA_25
import org.junit.jupiter.api.condition.OS.LINUX
import org.junit.jupiter.api.condition.OS.MAC
import org.junit.jupiter.api.condition.OS.WINDOWS

class ConditionalTestExecutionDemo {
    // tag::user_guide_os[]
    @Test
    @EnabledOnOs(MAC)
    fun onlyOnMacOs() {
        // ...
    }

    @TestOnMac
    fun testOnMac() {
        // ...
    }

    @Test
    @EnabledOnOs(LINUX, MAC)
    fun onLinuxOrMac() {
        // ...
    }

    @Test
    @DisabledOnOs(WINDOWS)
    fun notOnWindows() {
        // ...
    }

    @Target(AnnotationTarget.FUNCTION)
    @Retention(AnnotationRetention.RUNTIME)
    @Test
    @EnabledOnOs(MAC)
    annotation class TestOnMac
    // end::user_guide_os[]

    // tag::user_guide_architecture[]
    @Test
    @EnabledOnOs(architectures = ["aarch64"])
    fun onAarch64() {
        // ...
    }

    @Test
    @DisabledOnOs(architectures = ["x86_64"])
    fun notOnX86_64() {
        // ...
    }

    @Test
    @EnabledOnOs(value = [MAC], architectures = ["aarch64"])
    fun onNewMacs() {
        // ...
    }

    @Test
    @DisabledOnOs(value = [MAC], architectures = ["aarch64"])
    fun notOnNewMacs() {
        // ...
    }
    // end::user_guide_architecture[]

    // tag::user_guide_jre[]
    @Test
    @EnabledOnJre(JAVA_17)
    fun onlyOnJava17() {
        // ...
    }

    @Test
    @EnabledOnJre(JAVA_17, JAVA_21)
    fun onJava17And21() {
        // ...
    }

    @Test
    @EnabledForJreRange(min = JAVA_21, max = JAVA_25)
    fun fromJava21To25() {
        // ...
    }

    @Test
    @EnabledForJreRange(min = JAVA_21)
    fun onJava21ndHigher() {
        // ...
    }

    @Test
    @EnabledForJreRange(max = JAVA_18)
    fun fromJava17To18() {
        // ...
    }

    @Test
    @DisabledOnJre(JAVA_19)
    fun notOnJava19() {
        // ...
    }

    @Test
    @DisabledForJreRange(min = JAVA_17, max = JAVA_17)
    fun notFromJava17To19() {
        // ...
    }

    @Test
    @DisabledForJreRange(min = JAVA_19)
    fun notOnJava19AndHigher() {
        // ...
    }

    @Test
    @DisabledForJreRange(max = JAVA_18)
    fun notFromJava17To18() {
        // ...
    }
    // end::user_guide_jre[]

    // tag::user_guide_jre_arbitrary_versions[]
    @Test
    @EnabledOnJre(versions = [26])
    fun onlyOnJava26() {
        // ...
    }

    // Can also be expressed as follows.
    // @EnabledOnJre(value = [JAVA_25], versions = [26])
    @Test
    @EnabledOnJre(versions = [25, 26])
    fun onJava25And26() {
        // ...
    }

    @Test
    @EnabledForJreRange(minVersion = 26)
    fun onJava26AndHigher() {
        // ...
    }

    // Can also be expressed as follows.
    // @EnabledForJreRange(min = JAVA_25, maxVersion = 27)
    @Test
    @EnabledForJreRange(minVersion = 25, maxVersion = 27)
    fun fromJava25To27() {
        // ...
    }

    @Test
    @DisabledOnJre(versions = [26])
    fun notOnJava26() {
        // ...
    }

    // Can also be expressed as follows.
    // @DisabledOnJre(value = [JAVA_25], versions = [26])
    @Test
    @DisabledOnJre(versions = [25, 26])
    fun notOnJava25And26() {
        // ...
    }

    @Test
    @DisabledForJreRange(minVersion = 26)
    fun notOnJava26AndHigher() {
        // ...
    }

    // Can also be expressed as follows.
    // @DisabledForJreRange(min = JAVA_25, maxVersion = 27)
    @Test
    @DisabledForJreRange(minVersion = 25, maxVersion = 27)
    fun notFromJava25To27() {
        // ...
    }
    // end::user_guide_jre_arbitrary_versions[]

    // tag::user_guide_native[]
    @Test
    @EnabledInNativeImage
    fun onlyWithinNativeImage() {
        // ...
    }

    @Test
    @DisabledInNativeImage
    fun neverWithinNativeImage() {
        // ...
    }
    // end::user_guide_native[]

    // tag::user_guide_system_property[]
    @Test
    @EnabledIfSystemProperty(named = "os.arch", matches = ".*64.*")
    fun onlyOn64BitArchitectures() {
        // ...
    }

    @Test
    @DisabledIfSystemProperty(named = "ci-server", matches = "true")
    fun notOnCiServer() {
        // ...
    }
    // end::user_guide_system_property[]

    // tag::user_guide_environment_variable[]
    @Test
    @EnabledIfEnvironmentVariable(named = "ENV", matches = "staging-server")
    fun onlyOnStagingServer() {
        // ...
    }

    @Test
    @DisabledIfEnvironmentVariable(named = "ENV", matches = ".*development.*")
    fun notOnDeveloperWorkstation() {
        // ...
    }
    // end::user_guide_environment_variable[]

    // tag::user_guide_custom[]
    @Test
    @EnabledIf("customCondition")
    fun enabled() {
        // ...
    }

    @Test
    @DisabledIf("customCondition")
    fun disabled() {
        // ...
    }

    fun customCondition(): Boolean = true
    // end::user_guide_custom[]
}
