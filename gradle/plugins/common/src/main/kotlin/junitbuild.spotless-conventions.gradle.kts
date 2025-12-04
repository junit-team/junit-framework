import com.diffplug.spotless.LineEnding
import junitbuild.extensions.requiredVersionFromLibs

plugins {
	id("com.diffplug.spotless")
}

val license: License by rootProject.extra

spotless {

	format("misc") {
		target("*.gradle.kts", "gradle/plugins/**/*.gradle.kts", "*.gitignore")
		targetExclude("gradle/plugins/**/build/**")
		leadingSpacesToTabs()
		trimTrailingWhitespace()
		endWithNewline()
	}

	format("documentation") {
		target("*.adoc", "*.md", "src/**/*.adoc", "src/**/*.md")
		trimTrailingWhitespace()
		endWithNewline()
	}

	pluginManager.withPlugin("java") {

		val configDir = rootProject.layout.projectDirectory.dir("gradle/config/eclipse")
		val importOrderConfigFile = configDir.file("junit-eclipse.importorder")
		val javaFormatterConfigFile = configDir.file("junit-eclipse-formatter-settings.xml")

		java {
			targetExclude("**/module-info.java")
			licenseHeaderFile(license.headerFile, "(package|import) ")
			importOrderFile(importOrderConfigFile)
			val fullVersion = requiredVersionFromLibs("eclipse")
			val majorMinorVersion = "([0-9]+\\.[0-9]+).*".toRegex().matchEntire(fullVersion)!!.let { it.groups[1]!!.value }
			eclipse(majorMinorVersion).configFile(javaFormatterConfigFile)
			trimTrailingWhitespace()
			endWithNewline()
			removeUnusedImports()
		}

		format("moduleDescriptor") {
			target(fileTree(layout.projectDirectory.dir("src/main/java")) {
				include("module-info.java")
			})
			licenseHeaderFile(license.headerFile, "^$")
			trimTrailingWhitespace()
			endWithNewline()
		}
	}

	pluginManager.withPlugin("org.jetbrains.kotlin.jvm") {
		kotlin {
			targetExclude("**/src/test/resources/**")
			ktlint(requiredVersionFromLibs("ktlint"))
			licenseHeaderFile(license.headerFile)
			trimTrailingWhitespace()
			endWithNewline()
			toggleOffOn("formatter:off", "formatter:on")
		}
	}

	pluginManager.withPlugin("groovy") {
		groovy {
			licenseHeaderFile(license.headerFile)
			trimTrailingWhitespace()
			endWithNewline()
		}
	}

	// Explicitly configure line endings to avoid Spotless to search for .gitattributes file
	// see https://github.com/gradle/gradle/issues/25469#issuecomment-3444231151
	lineEndings = LineEnding.UNIX
}

tasks {
	named("spotlessDocumentation") {
		outputs.doNotCacheIf("negative avoidance savings") { true }
	}
	named("spotlessMisc") {
		outputs.doNotCacheIf("negative avoidance savings") { true }
	}
}
