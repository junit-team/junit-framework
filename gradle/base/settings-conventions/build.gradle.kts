import org.gradle.plugin.use.PluginDependency

plugins {
	`kotlin-dsl`
}

// Copy of junitbuild.extensions.markerCoordinates from dsl-extensions which cannot be
// used here because build scripts of this build cannot use its own subprojects
val Provider<PluginDependency>.markerCoordinates: Provider<String>
	get() = map { "${it.pluginId}:${it.pluginId}.gradle.plugin:${it.version}" }

dependencies {
	implementation(project(":build-parameters"))
	implementation(libs.plugins.commonCustomUserData.markerCoordinates)
	implementation(libs.plugins.develocity.markerCoordinates)
	implementation(libs.plugins.foojayResolver.markerCoordinates)
}
