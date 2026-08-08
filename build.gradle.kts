import junitbuild.extensions.modularProjects

plugins {
	id("junitbuild.base-conventions")
	id("junitbuild.checkstyle-nohttp")
	id("junitbuild.jacoco-aggregation-conventions")
}

description = "JUnit"
group = "org.junit"

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
		target("*.gradle.kts", "*/*.gradle.kts", "gradle/plugins/**/*.gradle.kts")
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
