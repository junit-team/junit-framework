package junitbuild.compatibility

import org.gradle.api.provider.Property

abstract class BackwardCompatibilityChecksExtension {

    abstract val enabled: Property<Boolean>

    abstract val previousVersion: Property<String>

}
