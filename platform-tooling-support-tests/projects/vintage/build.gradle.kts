import org.gradle.api.tasks.testing.logging.TestLogEvent

plugins {
	java
}

repositories {
	maven { url = uri(file(System.getProperty("maven.repo"))) }
	mavenCentral()
}

val junitVersion = providers.gradleProperty("junitVersion").orNull

dependencies {
	val junit4Version = System.getProperty("junit4Version", "4.12")
	testImplementation("junit:junit:$junit4Version")

	testImplementation("org.junit.vintage:junit-vintage-engine:$junitVersion") {
		exclude(group = "junit")
		because("we want to override it to test against different versions")
	}

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
		events = setOf(TestLogEvent.STARTED, TestLogEvent.PASSED, TestLogEvent.FAILED)
		afterSuite(KotlinClosure2<TestDescriptor, TestResult, Any>({ _, result ->
			result.exception?.printStackTrace(System.out)
		}))
	}
}
