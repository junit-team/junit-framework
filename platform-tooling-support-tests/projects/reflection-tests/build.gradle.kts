plugins {
	java
}

val junitVersion = providers.gradleProperty("junitVersion").orNull

repositories {
	maven { url = uri(file(System.getProperty("maven.repo"))) }
	mavenCentral()
}

dependencies {
	testImplementation("org.junit.jupiter:junit-jupiter:$junitVersion")
	testImplementation("org.junit.jupiter:junit-jupiter-engine:$junitVersion")
	testRuntimeOnly("org.junit.platform:junit-platform-launcher:$junitVersion")
}

java {
	toolchain {
		languageVersion = JavaLanguageVersion.of(17)
	}
}

tasks.test {
	useJUnitPlatform()

	testLogging {
		events("failed", "standardOut")
	}

	reports {
		html.required = true
	}
}
