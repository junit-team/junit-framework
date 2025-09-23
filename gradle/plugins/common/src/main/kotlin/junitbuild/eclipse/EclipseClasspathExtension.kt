package junitbuild.eclipse

import org.gradle.api.provider.Property

abstract class EclipseClasspathExtension {
    abstract val hideModularity: Property<Boolean>
}
