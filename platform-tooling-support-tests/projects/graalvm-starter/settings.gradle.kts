pluginManagement {
	plugins {
		id("org.graalvm.buildtools.native") version "0.10.4"
	}
	repositories {
		mavenCentral()
		gradlePluginPortal()
	}
}

plugins {
	id("org.gradle.toolchains.foojay-resolver-convention") version "0.9.0"
}

rootProject.name = "graalvm-starter"
