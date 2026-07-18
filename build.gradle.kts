plugins {
	id("junitbuild.base-conventions")
	id("junitbuild.build-metadata")
	id("junitbuild.checkstyle-nohttp")
	id("junitbuild.jacoco-aggregation-conventions")
	id("junitbuild.temp-maven-repo")
}

description = "JUnit"
group = "org.junit"

extra["license"] = License(
	name = "Eclipse Public License v2.0",
	url = uri("https://www.eclipse.org/legal/epl-v20.html"),
	headerFile = layout.settingsDirectory.file("gradle/config/spotless/eclipse-public-license-2.0.java")
)

val mavenizedProjects = listOf(
	projects.junitStart,
	projects.junitPlatformCommons,
	projects.junitPlatformConsole,
	projects.junitPlatformConsoleStandalone,
	projects.junitPlatformEngine,
	projects.junitPlatformLauncher,
	projects.junitPlatformReporting,
	projects.junitPlatformSuite,
	projects.junitPlatformSuiteApi,
	projects.junitPlatformSuiteEngine,
	projects.junitPlatformTestkit,
	projects.junitJupiter,
	projects.junitJupiterApi,
	projects.junitJupiterEngine,
	projects.junitJupiterMigrationsupport,
	projects.junitJupiterParams,
	projects.junitVintageEngine
)
	.also { extra["mavenizedProjects"] = it }

val modularProjects = mavenizedProjects
	.filter { it.path != projects.junitPlatformConsoleStandalone.path }
	.also { extra["modularProjects"] = it }

dependencies {
	modularProjects.forEach {
		jacocoAggregation(it)
	}
	jacocoAggregation(projects.documentation)
	jacocoAggregation(projects.jupiterTests)
	jacocoAggregation(projects.platformTests)
}

spotless {
	format("misc") {
		target("*.gradle.kts", "*/*.gradle.kts", "gradle/plugins/**/*.gradle.kts", "**/.gitignore")
		targetExclude("gradle/plugins/**/build/**")
		leadingSpacesToTabs()
		trimTrailingWhitespace()
		endWithNewline()
	}
	yaml {
		target(
			"*.yml", "*.yaml",
			".github/**/*.yml", ".github/**/*.yaml",
			"gradle/**/*.yml", "gradle/**/*.yaml"
		)
	}
}
