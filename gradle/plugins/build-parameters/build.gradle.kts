plugins {
	alias(libs.plugins.buildParameters)
}

group = "junitbuild"

tasks.compileJava {
	options.release = 21
}

buildParameters {
	pluginId("junitbuild.build-parameters")
	bool("ci") {
		description = "Whether or not this build is running in a CI environment"
		defaultValue = false
		fromEnvironment()
	}
	group("javaToolchain") {
		description = "Parameters controlling the Java toolchain used for compiling code and running tests"
		integer("version") {
			description = "JDK version"
		}
		string("implementation") {
			description = "JDK implementation (for example, 'j9')"
		}
	}
	group("documentation") {
		description = "Parameters controlling how the documentation is built"
		bool("replaceCurrentDocs") {
			description = "The documentation that is being deployed will replace what's currently deployed as 'current'"
			defaultValue = false
		}
	}
	group("junit") {
		group("develocity") {
			description = "Parameters controlling Develocity features"
			group("buildCache") {
				string("server") {
					description =
						"Remote build cache server address (protocol and hostname), e.g. https://eu-build-cache-ge.junit.org"
				}
			}
			group("predictiveTestSelection") {
				bool("enabled") {
					description = "Whether or not to use Predictive Test Selection for selecting tests to execute"
					defaultValue = true
				}
				bool("selectRemainingTests") {
					// see https://docs.gradle.com/develocity/predictive-test-selection/#gradle-selection-mode
					description = "Whether or not to use PTS' 'remaining tests' selection mode"
					defaultValue = false
				}
			}
			group("testDistribution") {
				bool("enabled") {
					description = "Whether or not to use Test Distribution for executing tests"
					defaultValue = false
					fromEnvironment()
				}
				integer("maxLocalExecutors") {
					description = "How many local executors to use for executing tests"
					defaultValue = 1
				}
				integer("maxRemoteExecutors") {
					description = "How many remote executors to request for executing tests"
				}
			}
		}
	}
	group("testing") {
		description = "Testing related parameters"
		bool("dryRun") {
			description = "Enables dry run mode for tests"
			defaultValue = false
		}
		bool("enableJaCoCo") {
			description = "Enables JaCoCo test coverage reporting"
			defaultValue = true
		}
		bool("enableJFR") {
			description = "Enables Java Flight Recorder functionality"
			defaultValue = false
		}
		integer("retries") {
			description = "Configures the number of times failing test are retried"
		}
		bool("hideOpenTestReportHtmlGeneratorOutput") {
			description = "Whether or not to hide the output of the OpenTestReportHtmlGenerator"
			defaultValue = true
		}
	}
	group("publishing") {
		bool("signArtifacts") {
			description = "Sign artifacts before publishing them to Maven repos"
		}
		string("group") {
			description = "Group ID for published Maven artifacts"
		}
	}
	group("jitpack") {
		string("version") {
			description = "The version computed by Jitpack"
		}
	}
	group("manifest") {
		string("buildTimestamp") {
			description = "Overrides the value of the 'Build-Date' and 'Build-Time' jar manifest entries. Can be set as a String (e.g. '2023-11-05 17:49:13.996+0100') or as seconds since the epoch."
			fromEnvironment("SOURCE_DATE_EPOCH") // see https://reproducible-builds.org/docs/source-date-epoch/
		}
		string("builtBy") {
			description = "Overrides the value of the 'Built-By' jar manifest entry"
		}
		string("createdBy") {
			description = "Overrides the value of the 'Created-By' jar manifest entry"
		}
	}
}
