import com.gradle.develocity.agent.gradle.internal.test.PredictiveTestSelectionConfigurationInternal
import com.gradle.develocity.agent.gradle.test.PredictiveTestSelectionMode
import junitbuild.extensions.bundleFromLibs
import junitbuild.extensions.dependencyFromLibs
import junitbuild.extensions.trackOperationSystemAsInput
import org.gradle.api.tasks.PathSensitivity.RELATIVE
import org.gradle.api.tasks.testing.logging.TestExceptionFormat.FULL
import org.gradle.api.tasks.testing.logging.TestLogEvent.FAILED
import org.gradle.internal.os.OperatingSystem
import java.io.IOException
import java.io.OutputStream
import java.nio.file.FileVisitResult
import java.nio.file.Files
import java.nio.file.NoSuchFileException
import java.nio.file.Path
import java.nio.file.SimpleFileVisitor
import java.nio.file.attribute.BasicFileAttributes

plugins {
	`java-library`
	id("junitbuild.build-parameters")
}

var javaAgent = configurations.dependencyScope("javaAgent")
var javaAgentClasspath = configurations.resolvable("javaAgentClasspath") {
	extendsFrom(javaAgent.get())
}

var openTestReportingCli = configurations.dependencyScope("openTestReportingCli")
var openTestReportingCliClasspath = configurations.resolvable("openTestReportingCliClasspath") {
	extendsFrom(openTestReportingCli.get())
	attributes {
		// Avoid using the shadowed variant of junit-platform-reporting
		attribute(Bundling.BUNDLING_ATTRIBUTE, objects.named(Bundling::class, Bundling.EXTERNAL))
	}
}

val generateOpenTestHtmlReport by tasks.registering(JavaExec::class) {
	mustRunAfter(tasks.withType<Test>())
	mainModule.set("org.opentest4j.reporting.cli")
	modularity.inferModulePath = true
	args("html-report")
	classpath(openTestReportingCliClasspath)
	argumentProviders += objects.newInstance(HtmlReportParameters::class).apply {
		eventXmlFiles.from(tasks.withType<Test>().map {
			objects.fileTree()
				.from(it.reports.junitXml.outputLocation)
				.include("junit-*/open-test-report.xml")
		})
		outputLocation = layout.buildDirectory.file("reports/open-test-report.html")
	}
	if (buildParameters.testing.hideOpenTestReportHtmlGeneratorOutput) {
		standardOutput = object : OutputStream() {
			override fun write(b: Int) {
				// discard output
			}
		}
	}
	outputs.cacheIf { true }
}

abstract class HtmlReportParameters : CommandLineArgumentProvider {

	@get:InputFiles
	@get:PathSensitive(RELATIVE)
	@get:SkipWhenEmpty
	abstract val eventXmlFiles: ConfigurableFileCollection

	@get:OutputFile
	abstract val outputLocation: RegularFileProperty

	override fun asArguments() = listOf("--output", outputLocation.get().asFile.absolutePath) +
			eventXmlFiles.map { it.absolutePath }.toList()
}

tasks.withType<Test>().configureEach {
	useJUnitPlatform {
		includeEngines("junit-jupiter")
	}
	include("**/*Test.class", "**/*Tests.class")
	testLogging {
		events = setOf(FAILED)
		exceptionFormat = FULL
	}
	develocity {
		testRetry {
			maxRetries.convention(buildParameters.testing.retries.orElse(if (buildParameters.ci) 2 else 0))
		}
		testDistribution {
			enabled.convention(buildParameters.junit.develocity.testDistribution.enabled && (!buildParameters.ci || !System.getenv("DEVELOCITY_ACCESS_KEY").isNullOrBlank()))
			maxLocalExecutors.convention(buildParameters.junit.develocity.testDistribution.maxLocalExecutors)
			maxRemoteExecutors.convention(buildParameters.junit.develocity.testDistribution.maxRemoteExecutors)
			if (buildParameters.ci) {
				when {
					OperatingSystem.current().isLinux -> requirements.add("os=linux")
					OperatingSystem.current().isWindows -> requirements.add("os=windows")
					OperatingSystem.current().isMacOsX -> requirements.add("os=macos")
				}
			}
		}
		predictiveTestSelection {
			enabled.convention(buildParameters.junit.develocity.predictiveTestSelection.enabled)

			if (buildParameters.junit.develocity.predictiveTestSelection.selectRemainingTests) {
				mode.convention(PredictiveTestSelectionMode.REMAINING_TESTS)
			}

			// Ensure PTS works when publishing Build Scans to scans.gradle.com
			this as PredictiveTestSelectionConfigurationInternal
			server = uri("https://ge.junit.org")

			mergeCodeCoverage = true
		}
	}
	systemProperty("java.util.logging.manager", "org.apache.logging.log4j.jul.LogManager")
	// https://github.com/gradle/gradle/issues/30554
	systemProperty("log4j2.julLoggerAdapter", "org.apache.logging.log4j.jul.CoreLoggerAdapter")
	// Avoid overhead (see https://logging.apache.org/log4j/2.x/manual/jmx.html#enabling-jmx)
	systemProperty("log4j2.disableJmx", "true")
	// https://github.com/raphw/byte-buddy/issues/1803
	systemProperty("net.bytebuddy.safe", true)
	// Required until ASM officially supports the JDK 14
	systemProperty("net.bytebuddy.experimental", true)
	if (buildParameters.testing.enableJFR) {
		jvmArgs(
			"-XX:+UnlockDiagnosticVMOptions",
			"-XX:+DebugNonSafepoints",
			"-XX:StartFlightRecording=filename=${reports.junitXml.outputLocation.get()},dumponexit=true,settings=profile.jfc",
			"-XX:FlightRecorderOptions=stackdepth=1024"
		)
	}
	systemProperty("junit.platform.execution.dryRun.enabled", buildParameters.testing.dryRun)

	// Track OS as input so that tests are executed on all configured operating systems on CI
	trackOperationSystemAsInput()

	// Avoid passing unnecessary environment variables to the JVM (from GitHub Actions)
	if (buildParameters.ci) {
		environment.remove("RUNNER_TEMP")
		environment.remove("GITHUB_ACTION")
	}

	jvmArgumentProviders += CommandLineArgumentProvider {
		listOf(
			"-Djunit.platform.reporting.open.xml.enabled=true",
			"-Djunit.platform.reporting.open.xml.git.enabled=true",
			"-Djunit.platform.reporting.output.dir=${reports.junitXml.outputLocation.get().asFile.absolutePath}/junit-{uniqueNumber}",
		)
	}
	systemProperty("junit.platform.output.capture.stdout", "true")
	systemProperty("junit.platform.output.capture.stderr", "true")
	systemProperty("junit.platform.discovery.issue.severity.critical", "info")

	jvmArgumentProviders += objects.newInstance(JavaAgentArgumentProvider::class).apply {
		classpath.from(javaAgentClasspath)
	}
	jvmArgs("-Xshare:off") // https://github.com/mockito/mockito/issues/3111

	doFirst {
		reports.junitXml.outputLocation.asFile.get()
			.listFiles { _, name -> name.startsWith("junit-") }
			?.forEach { dir ->
				Files.walkFileTree(dir.toPath(), object : SimpleFileVisitor<Path>() {
					override fun visitFile(file: Path, attrs: BasicFileAttributes): FileVisitResult {
						return deleteIfExistsAndContinue(file)
					}

					override fun postVisitDirectory(dir: Path, ex: IOException?): FileVisitResult {
						if (ex is NoSuchFileException) {
							return FileVisitResult.CONTINUE
						}
						if (ex != null) {
							throw ex
						}
						return deleteIfExistsAndContinue(dir)
					}

					private fun deleteIfExistsAndContinue(dir: Path): FileVisitResult {
						Files.deleteIfExists(dir)
						return FileVisitResult.CONTINUE
					}

					override fun visitFileFailed(file: Path, ex: IOException): FileVisitResult {
						if (ex is NoSuchFileException) {
							return FileVisitResult.CONTINUE
						}
						throw ex
					}
				})
			}
	}

	finalizedBy(generateOpenTestHtmlReport)
}

dependencies {
	testImplementation(platform(dependencyFromLibs("mockito-bom")))
	testImplementation(dependencyFromLibs("assertj"))
	testImplementation(dependencyFromLibs("mockito-junit-jupiter"))
	testImplementation(dependencyFromLibs("testingAnnotations"))
	testImplementation(project(":junit-jupiter"))

	testRuntimeOnly(project(":junit-platform-launcher"))
	testRuntimeOnly(project(":junit-platform-reporting"))

	testRuntimeOnly(bundleFromLibs("log4j"))
	testRuntimeOnly(dependencyFromLibs("openTestReporting-events")) {
		because("it's required to run tests via IntelliJ which does not consumed the shadowed jar of junit-platform-reporting")
	}

	openTestReportingCli(dependencyFromLibs("openTestReporting-cli"))
	openTestReportingCli(project(":junit-platform-reporting"))

	javaAgent(platform(dependencyFromLibs("mockito-bom")))
	javaAgent(dependencyFromLibs("mockito-core")) {
		isTransitive = false
	}
}

abstract class JavaAgentArgumentProvider : CommandLineArgumentProvider {

	@get:Classpath
	abstract val classpath: ConfigurableFileCollection

	override fun asArguments() = listOf("-javaagent:${classpath.singleFile.absolutePath}")

}
