import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17
import org.jetbrains.kotlin.gradle.dsl.KotlinVersion.KOTLIN_2_1

plugins {
	kotlin("jvm") version "2.2.20"
}

repositories {
	maven { url = uri(file(System.getProperty("maven.repo"))) }
	mavenCentral()
}

val junitVersion: String by project

dependencies {
	testImplementation(kotlin("stdlib"))
	testImplementation("org.junit.jupiter:junit-jupiter:$junitVersion")
	testRuntimeOnly("org.junit.platform:junit-platform-launcher:$junitVersion")
}

java {
	toolchain {
		languageVersion = JavaLanguageVersion.of(17)
	}
}

tasks.withType<KotlinCompile>().configureEach {
	compilerOptions {
		jvmTarget = JVM_17
		apiVersion = KOTLIN_2_1
		languageVersion = KOTLIN_2_1
		freeCompilerArgs.addAll("-Xskip-prerelease-check")
	}
}

tasks.test {
	useJUnitPlatform()
	testLogging {
		events("passed", "skipped", "failed")
	}
}
