import junitbuild.extensions.markerCoordinates
import org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_21

plugins {
	`kotlin-dsl`
}

dependencies {
	implementation(projects.buildParameters)
	implementation(libs.plugins.node.markerCoordinates)
	constraints {
		implementation("com.fasterxml.jackson.core:jackson-core") {
			version {
				require("2.15.0")
			}
			because("Workaround for CVE-2025-52999")
		}
	}
	implementation(libs.plugins.spring.antora.markerCoordinates)
	constraints {
		implementation("org.yaml:snakeyaml") {
			version {
				require("2.0")
			}
			because("Workaround for CVE-2022-1471")
		}
	}
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
