import org.gradle.api.tasks.PathSensitivity.RELATIVE
import org.gradle.plugins.ide.eclipse.model.Classpath
import org.gradle.plugins.ide.eclipse.model.SourceFolder

plugins {
	id("junitbuild.code-generator")
	id("junitbuild.kotlin-library-conventions")
	id("junitbuild.junit4-compatibility")
	id("junitbuild.testing-conventions")
	groovy
}

dependencies {
	testImplementation(projects.junitJupiter)
	testImplementation(projects.junitJupiterMigrationsupport)
	testImplementation(projects.junitPlatformLauncher)
	testImplementation(projects.junitPlatformSuiteEngine)
	testImplementation(projects.junitPlatformTestkit)
	testImplementation(testFixtures(projects.junitPlatformCommons))
	testImplementation(kotlin("stdlib"))
	testImplementation(libs.jimfs)
	testImplementation(libs.junit4)
	testImplementation(libs.kotlinx.coroutines)
	testImplementation(libs.groovy)
	testImplementation(libs.memoryfilesystem)
	testImplementation(testFixtures(projects.junitJupiterApi))
	testImplementation(testFixtures(projects.junitJupiterEngine))
	testImplementation(testFixtures(projects.junitPlatformLauncher))
	testImplementation(testFixtures(projects.junitPlatformReporting))

	testRuntimeOnly(kotlin("reflect"))
}

tasks {
	test {
		inputs.dir("src/test/resources").withPathSensitivity(RELATIVE)
		systemProperty("developmentVersion", version)
	}
	test_4_12 {
		filter {
			includeTestsMatching("org.junit.jupiter.migrationsupport.*")
		}
	}
}

eclipse {
	classpath.file.whenMerged {
		this as Classpath
		entries.filterIsInstance<SourceFolder>().forEach {
			if (it.path == "src/test/java") {
				// Exclude test classes that depend on compiled Kotlin code.
				it.excludes.add("**/AtypicalJvmMethodNameTests.java")
				it.excludes.add("**/TestInstanceLifecycleKotlinTests.java")
			}
		}
	}
	project {
		// Remove Groovy Nature, since we don't require a Groovy plugin for Eclipse
		// in order for developers to work with the code base.
		natures.removeAll { it == "org.eclipse.jdt.groovy.core.groovyNature" }
	}
}
