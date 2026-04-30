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
