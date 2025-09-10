import junitbuild.extensions.markerCoordinates
import org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_21

plugins {
	`kotlin-dsl`
}

dependencies {
	implementation("junitbuild.base:dsl-extensions")
	implementation(libs.plugins.download.markerCoordinates)
	implementation(libs.plugins.japicmp.markerCoordinates)
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
