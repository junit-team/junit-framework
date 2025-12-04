package junitbuild.eclipse

import org.gradle.api.provider.Property

abstract class EclipseConventionsExtension {
    abstract val hideModularity: Property<Boolean>
}
