plugins {
	java
	id("org.graalvm.buildtools.native")
}

val junitVersion = providers.gradleProperty("junitVersion").orNull
val jdkVersion = providers.gradleProperty("jdkVersion").orNull!!.toInt()

dependencies {
	testImplementation("org.junit.jupiter:junit-jupiter:$junitVersion")
	testImplementation("junit:junit:4.13.2")
	testImplementation("org.junit.platform:junit-platform-suite:$junitVersion")
	testRuntimeOnly("org.junit.vintage:junit-vintage-engine:$junitVersion")
	testRuntimeOnly("org.junit.platform:junit-platform-reporting:$junitVersion")
}

tasks.withType<JavaCompile>().configureEach {
	options.release = jdkVersion
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

val initializeAtBuildTime = mapOf<String, List<String>>(
	// These need to be added to native-build-tools
	"6.2" to listOf(),
)

graalvmNative {
	metadataRepository {
		enabled = false
	}
	binaries {
		named("test") {
			buildArgs.add("-H:+ReportExceptionStackTraces")
			if (jdkVersion <= 21) { // no longer necessary on higher versions, see https://github.com/graalvm/native-build-tools/pull/693
				val classNames = initializeAtBuildTime.values.flatten()
				if (classNames.isNotEmpty()) {
					buildArgs.add("--initialize-at-build-time=${classNames.joinToString(",")}")
				}
			}
		}
	}
}
