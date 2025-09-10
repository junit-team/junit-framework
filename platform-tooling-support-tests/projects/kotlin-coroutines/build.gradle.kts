plugins {
	kotlin("jvm") version "2.2.20"
}

val junitVersion: String by project

repositories {
	maven { url = uri(file(System.getProperty("maven.repo"))) }
	mavenCentral()
}

dependencies {
	testImplementation("org.junit.jupiter:junit-jupiter:$junitVersion")
	testRuntimeOnly("org.junit.platform:junit-platform-launcher")

	if (!project.hasProperty("withoutKotlinReflect")) {
		testImplementation(kotlin("reflect"))
	}

	if (!project.hasProperty("withoutKotlinxCoroutines")) {
		testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.10.2")
	}
}

java {
	toolchain {
		languageVersion = JavaLanguageVersion.of(17)
	}
}

tasks.test {
	useJUnitPlatform()
	testLogging {
		exceptionFormat = org.gradle.api.tasks.testing.logging.TestExceptionFormat.FULL
	}
	systemProperty("junit.platform.stacktrace.pruning.enabled", "false")
}
