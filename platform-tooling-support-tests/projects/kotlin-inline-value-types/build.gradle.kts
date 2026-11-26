plugins {
	kotlin("jvm") version "2.4.21"
}

val junitVersion = providers.gradleProperty("junitVersion").orNull

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
