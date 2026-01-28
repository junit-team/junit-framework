plugins {
	id("junitbuild.jacoco-conventions")
	`jacoco-report-aggregation`
}

val jacocoRootReport by reporting.reports.creating(JacocoCoverageReport::class) {
	testSuiteName = "test"
}

val classesView = configurations["aggregateCodeCoverageReportResults"].incoming.artifactView {
	withVariantReselection() // Required to ensure the transformed classes are selected
	componentFilter { it is ProjectComponentIdentifier }
	attributes {
		attribute(LibraryElements.LIBRARY_ELEMENTS_ATTRIBUTE, named(LibraryElements.CLASSES))
	}
}

tasks {
	val reportTask = named<JacocoReport>(jacocoRootReport.reportTask.name)
	val jacocoRootCoverageVerification by registering(JacocoCoverageVerification::class) {
		enabled = !buildParameters.junit.develocity.predictiveTestSelection.enabled
		executionData.from(reportTask.map { it.executionData })
		classDirectories.from(reportTask.map { it.classDirectories })
		sourceDirectories.from(reportTask.map { it.sourceDirectories })
		violationRules {
			rule {
				limit {
					// In order to detect problems with coverage aggregation, we require a minimum coverage percentage
					minimum = "0.90".toBigDecimal()
				}
			}
		}
	}
	reportTask {
		// Override to restore behavior of pre-8.2.1 Gradle (see https://github.com/gradle/gradle/issues/25618)
		classDirectories.setFrom(classesView.files)
		finalizedBy(jacocoRootCoverageVerification)
	}
}
