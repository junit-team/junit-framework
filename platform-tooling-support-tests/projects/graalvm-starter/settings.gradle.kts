pluginManagement {
	plugins {
		// TODO Check if classes can be removed from `initializeAtBuildTime` in build.gradle.kts when upgrading
		id("org.graalvm.buildtools.native") version "0.11.2"
	}
	repositories {
		mavenCentral()
		gradlePluginPortal()
		maven(url = "https://raw.githubusercontent.com/graalvm/native-build-tools/snapshots") {
			mavenContent {
				snapshotsOnly()
			}
		}
	}
}

plugins {
	id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}

dependencyResolutionManagement {
	repositories {
		repositories {
			maven { url = uri(file(System.getProperty("maven.repo"))) }
			mavenCentral()
			maven(url = "https://raw.githubusercontent.com/graalvm/native-build-tools/snapshots") {
				mavenContent {
					snapshotsOnly()
				}
			}
		}
	}
}

rootProject.name = "graalvm-starter"
