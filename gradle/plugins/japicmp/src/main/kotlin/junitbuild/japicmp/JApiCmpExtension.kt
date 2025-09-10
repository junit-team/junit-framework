package junitbuild.japicmp

import org.gradle.api.provider.Property
import org.gradle.api.provider.SetProperty

abstract class JApiCmpExtension {

    abstract val previousVersion: Property<String>

    abstract val acceptedIncompatibilities: SetProperty<String>

}
