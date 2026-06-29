plugins {
	java
}

val junitVersion = providers.gradleProperty("junitVersion").orNull

repositories {
	maven { url = uri(file(System.getProperty("maven.repo"))) }
	mavenCentral()
}

dependencies {
	testImplementation("org.junit.jupiter:junit-jupiter-api:$junitVersion") {
		exclude(group = "org.junit.jupiter", module = "junit-jupiter-engine")
	}
	testRuntimeOnly("org.junit.platform:junit-platform-launcher:$junitVersion")}

java {
	toolchain {
		languageVersion = JavaLanguageVersion.of(17)
	}
}

tasks.test {
	useJUnitPlatform()
}
