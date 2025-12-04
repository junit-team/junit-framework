import junitbuild.extensions.markerCoordinates
import org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_21

plugins {
	`kotlin-dsl`
}

dependencies {
	implementation("junitbuild.base:dsl-extensions")
	implementation(projects.buildParameters)
	implementation(projects.backwardCompatibility)
	implementation(libs.plugins.kotlin.markerCoordinates)
	implementation(libs.plugins.bnd.markerCoordinates)
	implementation(libs.plugins.commonCustomUserData.markerCoordinates)
	implementation(libs.plugins.develocity.markerCoordinates)
	implementation(libs.plugins.errorProne.markerCoordinates)
	implementation(libs.plugins.foojayResolver.markerCoordinates)
	implementation(libs.plugins.jmh.markerCoordinates)
	implementation(libs.plugins.nullaway.markerCoordinates)
	implementation(libs.plugins.shadow.markerCoordinates)
	implementation(libs.plugins.spotless.markerCoordinates)
}

tasks.compileJava {
	options.release = 21
}

kotlin {
	compilerOptions {
		jvmTarget = JVM_21
		freeCompilerArgs.add("-Xjdk-release=21")
	}
}
