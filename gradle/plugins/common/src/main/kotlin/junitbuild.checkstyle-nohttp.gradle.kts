import junitbuild.extensions.dependencyFromLibs
import junitbuild.extensions.requiredVersionFromLibs

plugins {
	id("junitbuild.checkstyle-conventions")
}

dependencies {
	checkstyle(dependencyFromLibs("nohttp-checkstyle"))
}

configurations.checkstyle {
	resolutionStrategy {
		eachDependency {
			// Workaround for CVE-2024-12798 and CVE-2024-12801
			if (requested.group == "ch.qos.logback") {
				useVersion(requiredVersionFromLibs("logback"))
			}
		}
	}
}

tasks.register<Checkstyle>("checkstyleNohttp") {
	group = "verification"
	description = "Checks for illegal uses of http://"
	classpath = files(configurations.checkstyle)
	config = resources.text.fromFile(checkstyle.configDirectory.file("checkstyleNohttp.xml"))
	source = fileTree(layout.projectDirectory) {
		exclude(".git/**", "**/.gradle/**")
		exclude(".idea/**", ".eclipse/**")
		exclude("**/*.class")
		exclude("**/*.hprof")
		exclude("**/*.jar")
		exclude("**/*.jpg", "**/*.png")
		exclude("**/*.jks")
		exclude("**/build/**")
		exclude("**/.kotlin")
		exclude("**/node_modules/**")
	}
}
