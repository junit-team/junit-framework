import junitbuild.extensions.dependencyProject

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

val platformProjects = listOf(
		projects.junitPlatformCommons,
		projects.junitPlatformConsole,
		projects.junitPlatformConsoleStandalone,
		projects.junitPlatformEngine,
		projects.junitPlatformLauncher,
		projects.junitPlatformReporting,
		projects.junitPlatformSuite,
		projects.junitPlatformSuiteApi,
		projects.junitPlatformSuiteEngine,
		projects.junitPlatformTestkit
)
	.map { dependencyProject(it) }
	.also { extra["platformProjects"] = it }

val jupiterProjects = listOf(
		projects.junitJupiter,
		projects.junitJupiterApi,
		projects.junitJupiterEngine,
		projects.junitJupiterMigrationsupport,
		projects.junitJupiterParams
)
	.map { dependencyProject(it) }
	.also { extra["jupiterProjects"] = it }

val vintageProjects = listOf(
	projects.junitVintageEngine
)
	.map { dependencyProject(it) }
	.also { extra["vintageProjects"] = it }

val mavenizedProjects = (listOf(dependencyProject(projects.junitStart)) + platformProjects + jupiterProjects + vintageProjects)
	.also { extra["mavenizedProjects"] = it }
val modularProjects = (mavenizedProjects - setOf(dependencyProject(projects.junitPlatformConsoleStandalone)))
	.also { extra["modularProjects"] = it }

dependencies {
	modularProjects.forEach {
		jacocoAggregation(it)
	}
	jacocoAggregation(projects.documentation)
	jacocoAggregation(projects.jupiterTests)
	jacocoAggregation(projects.platformTests)
}
