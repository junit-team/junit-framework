plugins {
	java
	id("org.graalvm.buildtools.native")
}

val junitVersion: String by project

dependencies {
	testImplementation("org.junit.jupiter:junit-jupiter:$junitVersion")
	testImplementation("junit:junit:4.13.2")
	testImplementation("org.junit.platform:junit-platform-suite:$junitVersion")
	testRuntimeOnly("org.junit.vintage:junit-vintage-engine:$junitVersion")
	testRuntimeOnly("org.junit.platform:junit-platform-reporting:$junitVersion")
}

tasks.withType<JavaCompile>().configureEach {
	options.release = 21
}

tasks.test {
	useJUnitPlatform {
		includeEngines("junit-platform-suite")
	}

	val outputDir = reports.junitXml.outputLocation
	jvmArgumentProviders += CommandLineArgumentProvider {
		listOf(
			"-Djunit.platform.reporting.open.xml.enabled=true",
			"-Djunit.platform.reporting.output.dir=${outputDir.get().asFile.absolutePath}"
		)
	}
}

val initializeAtBuildTime = mapOf(
	// These need to be added to native-build-tools
	"5.14.1" to listOf(
		"org.junit.jupiter.engine.discovery.MethodSegmentResolver"
	),
)

graalvmNative {
	binaries {
		named("test") {
			buildArgs.add("-H:+ReportExceptionStackTraces")
			val classNames = initializeAtBuildTime.values.flatten()
			if (classNames.isNotEmpty()) {
				buildArgs.add("--initialize-at-build-time=${classNames.joinToString(",")}")
			}
		}
	}
}
