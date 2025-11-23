import junitbuild.extensions.dependencyFromLibs
import junitbuild.extensions.requiredVersionFromLibs

plugins {
	id("junitbuild.checkstyle-conventions")
}

dependencies {
	checkstyle(dependencyFromLibs("nohttp-checkstyle"))
	constraints {
		checkstyle("com.puppycrawl.tools:checkstyle") {
			version {
				require(requiredVersionFromLibs("checkstyle"))
			}
		}
		checkstyle("ch.qos.logback:logback-classic") {
			version {
				require("1.5.18")
			}
			because("Workaround for CVE-2024-12798 and CVE-2024-12801")
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
		exclude(".idea/**", "**/.settings/**", "**/.classpath", "**/.project")
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
