import junitbuild.extensions.dependencyProject

plugins {
	id("junitbuild.base-conventions")
	id("junitbuild.build-metadata")
	id("junitbuild.checkstyle-nohttp")
	id("junitbuild.jacoco-aggregation-conventions")
	id("junitbuild.maven-central-publishing")
	id("junitbuild.temp-maven-repo")
}

description = "JUnit"
group = "org.junit"

val license by extra(License(
	name = "Eclipse Public License v2.0",
	url = uri("https://www.eclipse.org/legal/epl-v20.html"),
	headerFile = layout.projectDirectory.file("gradle/config/spotless/eclipse-public-license-2.0.java")
))

val platformProjects by extra(listOf(
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
).map { dependencyProject(it) })

val jupiterProjects by extra(listOf(
		projects.junitJupiter,
		projects.junitJupiterApi,
		projects.junitJupiterEngine,
		projects.junitJupiterMigrationsupport,
		projects.junitJupiterParams
).map { dependencyProject(it) })

val vintageProjects by extra(listOf(
	dependencyProject(projects.junitVintageEngine)
))

val mavenizedProjects by extra(listOf(dependencyProject(projects.junitStart)) + platformProjects + jupiterProjects + vintageProjects)
val modularProjects by extra(mavenizedProjects - setOf(dependencyProject(projects.junitPlatformConsoleStandalone)))

dependencies {
	modularProjects.forEach {
		jacocoAggregation(it)
	}
	jacocoAggregation(projects.documentation)
	jacocoAggregation(projects.jupiterTests)
	jacocoAggregation(projects.platformTests)
}
