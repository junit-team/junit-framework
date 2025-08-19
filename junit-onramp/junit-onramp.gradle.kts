import junitbuild.java.UpdateJarAction

plugins {
	id("junitbuild.java-library-conventions")
	id("junitbuild.java-nullability-conventions")
}

description = "JUnit On-Ramp Module"

dependencies {
	api(platform(projects.junitBom))
	api(projects.junitJupiter)
	compileOnlyApi(projects.junitJupiterEngine)
	api(projects.junitPlatformLauncher)
	implementation(projects.junitPlatformConsole)
}

tasks {
	jar {
		manifest {
			attributes("Main-Class" to "org.junit.onramp.JUnit")
		}
		doLast(objects.newInstance(UpdateJarAction::class).apply {
			javaLauncher = project.javaToolchains.launcherFor(java.toolchain)
			args.addAll(
				"--file", archiveFile.get().asFile.absolutePath,
				"--main-class", "org.junit.onramp.JUnit",
			)
		})
	}
}

japicmp {
	enabled = false // no previous version, yet
}
