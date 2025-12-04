plugins {
	java
}

val junitVersion: String by project

repositories {
	maven { url = uri(file(System.getProperty("maven.repo"))) }
	mavenCentral()
}

dependencies {
	testImplementation("org.junit.jupiter:junit-jupiter:$junitVersion")
	testRuntimeOnly("org.junit.platform:junit-platform-reporting:$junitVersion")
}

java {
	toolchain {
		languageVersion = JavaLanguageVersion.of(System.getProperty("java.toolchain.version"))
	}
}

tasks.test {
	useJUnitPlatform()

	testLogging {
		events("passed", "skipped", "failed", "standardOut")
		exceptionFormat = org.gradle.api.tasks.testing.logging.TestExceptionFormat.FULL
	}

	reports {
		html.required = true
	}

	val outputDir = reports.junitXml.outputLocation
	jvmArgumentProviders += CommandLineArgumentProvider {
		listOf(
			"-Djunit.platform.reporting.open.xml.enabled=true",
			"-Djunit.platform.reporting.output.dir=${outputDir.get().asFile.absolutePath}"
		)
	}
}
