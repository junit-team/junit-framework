import nmcp.NmcpAggregationExtension
import kotlin.time.Duration.Companion.minutes
import kotlin.time.toJavaDuration

plugins {
	id("com.gradleup.nmcp.settings")
}

nmcpSettings {
	centralPortal {
		username = providers.gradleProperty("mavenCentralUsername")
		password = providers.gradleProperty("mavenCentralPassword")
		publishingType = "USER_MANAGED"
		validationTimeout = 10.minutes.toJavaDuration()
		publishingTimeout = 30.minutes.toJavaDuration()
	}
}

gradle.lifecycle.afterProject {
	if (project == rootProject) {
		the<NmcpAggregationExtension>().apply {
			publishAllChecksums = true
		}
		tasks.named<Zip>("nmcpZipAggregation") {
			eachFile {
				if (name.contains(".asc.")) {
					exclude()
				}
			}
		}
	}
}
