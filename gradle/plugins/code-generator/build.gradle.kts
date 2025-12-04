import org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_21

plugins {
	`kotlin-dsl`
}

dependencies {
	implementation("junitbuild.base:code-generator-model")
	implementation("junitbuild.base:dsl-extensions")
	implementation(projects.common)
	implementation(libs.jackson.dataformat.yaml)
	implementation(libs.jackson.module.kotlin)
	implementation(libs.jte)
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
