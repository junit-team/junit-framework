package junitbuild.compatibility

import org.gradle.api.provider.Property
import org.gradle.api.provider.SetProperty

abstract class BackwardCompatibilityChecksExtension {

    abstract val enabled: Property<Boolean>

    abstract val previousVersion: Property<String>

    abstract val acceptedIncompatibilities: SetProperty<String>

}
