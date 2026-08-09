import junitbuild.extensions.requiredVersionFromLibs

plugins {
	base
	checkstyle
}

dependencies {
	constraints {
		checkstyle("org.apache.commons:commons-lang3") {
			version {
				require("3.18.0")
			}
			because("Workaround for CVE-2025-48924")
		}
		checkstyle("org.codehaus.plexus:plexus-utils") {
			version {
				require("3.6.1")
			}
			because("Workaround for CVE-2025-67030")
		}
	}
}

checkstyle {
	toolVersion = requiredVersionFromLibs("checkstyle")
	configDirectory = layout.settingsDirectory.dir("gradle/config/checkstyle")
}

tasks.check {
	dependsOn(tasks.withType<Checkstyle>())
}
