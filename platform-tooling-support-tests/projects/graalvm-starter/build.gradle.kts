plugins {
	java
	id("org.graalvm.buildtools.native")
}

val jupiterVersion: String by project
val platformVersion: String by project
val vintageVersion: String by project

repositories {
	maven { url = uri(file(System.getProperty("maven.repo"))) }
	mavenCentral()
}

dependencies {
	testImplementation("org.junit.jupiter:junit-jupiter:$jupiterVersion")
	testImplementation("junit:junit:4.13.2")
	testImplementation("org.junit.platform:junit-platform-suite:$platformVersion")
	testRuntimeOnly("org.junit.vintage:junit-vintage-engine:$vintageVersion")
	testRuntimeOnly("org.junit.platform:junit-platform-reporting:$platformVersion")
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

// These will be part of the next version of native-build-tools
// see https://github.com/graalvm/native-build-tools/pull/693
val initializeAtBuildTime = listOf(
	"org.junit.jupiter.engine.descriptor.ClassBasedTestDescriptor\$ClassInfo",
	"org.junit.jupiter.engine.descriptor.ClassBasedTestDescriptor\$LifecycleMethods",
	"org.junit.jupiter.engine.descriptor.ClassTemplateInvocationTestDescriptor",
	"org.junit.jupiter.engine.descriptor.ClassTemplateTestDescriptor",
	"org.junit.jupiter.engine.descriptor.DynamicDescendantFilter\$Mode",
	"org.junit.jupiter.engine.descriptor.ExclusiveResourceCollector\$1",
	"org.junit.jupiter.engine.descriptor.MethodBasedTestDescriptor\$MethodInfo",
	"org.junit.jupiter.engine.discovery.ClassSelectorResolver\$DummyClassTemplateInvocationContext",
	"org.junit.platform.launcher.core.DiscoveryIssueNotifier",
	"org.junit.platform.launcher.core.HierarchicalOutputDirectoryProvider",
	"org.junit.platform.launcher.core.LauncherDiscoveryResult\$EngineResultInfo",
	"org.junit.platform.suite.engine.SuiteTestDescriptor\$LifecycleMethods",
)

graalvmNative {
	binaries {
		named("test") {
			buildArgs.add("--strict-image-heap")
			buildArgs.add("-H:+ReportExceptionStackTraces")
			buildArgs.add("--initialize-at-build-time=${initializeAtBuildTime.joinToString(",")}")
		}
	}
}
