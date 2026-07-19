import java.time.Duration
import kotlin.time.Duration.Companion.minutes
import kotlin.time.toJavaDuration

plugins {
	id("junitbuild.build-parameters")
	id("com.gradleup.nmcp.settings")
}

nmcpSettings {
	centralPortal {
		username = providers.gradleProperty("mavenCentralUsername")
		password = providers.gradleProperty("mavenCentralPassword")
		publishingType = "USER_MANAGED"
		validationTimeout = 10.minutes.toJavaDuration()
		publishingTimeout = buildParameters.publishing.timeout.map(Duration::parse)
	}
}
