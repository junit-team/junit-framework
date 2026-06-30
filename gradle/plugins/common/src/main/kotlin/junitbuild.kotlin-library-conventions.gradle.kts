
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.dsl.KotlinVersion.KOTLIN_2_1
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
	id("junitbuild.java-library-conventions")
	kotlin("jvm")
}

tasks.named("kotlinSourcesJar") {
	enabled = false
}

val javaLibraryExtension = project.the<JavaLibraryExtension>()

tasks.withType<KotlinCompile>().configureEach {
	compilerOptions {
		jvmTarget = javaLibraryExtension.mainJavaVersion.map { JvmTarget.fromTarget(it.toString()) }

		apiVersion = KOTLIN_2_1 // remove suppression below when upgrading
		languageVersion = apiVersion

		allWarningsAsErrors.convention(true)
		javaParameters = true

		freeCompilerArgs.add("-opt-in=kotlin.RequiresOptIn")
		freeCompilerArgs.add(jvmTarget.map { "-Xjdk-release=${JavaVersion.toVersion(it.target).majorVersion}" })
		// Silence deprecation warning for language/API version which is fixed to 2.1 for backward compatibility
		freeCompilerArgs.add("-Xsuppress-version-warnings")
	}
}

tasks.named<KotlinCompile>("compileTestKotlin") {
	compilerOptions.jvmTarget = javaLibraryExtension.testJavaVersion.map { JvmTarget.fromTarget(it.toString()) }
}

configurations.named { it == "kotlinBouncyCastleConfiguration" }.configureEach {
	resolutionStrategy {
		eachDependency {
			if (requested.group == "org.bouncycastle") {
				useVersion("1.84")
				because("Workaround for CVE-2026-3505 et al (used by kotlin plugin)")
			}
		}
	}
}
