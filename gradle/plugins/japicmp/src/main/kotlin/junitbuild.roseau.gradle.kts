import de.undercouch.gradle.tasks.download.Download
import downloadPreviousReleaseJar
import junitbuild.extensions.dependencyFromLibs
import junitbuild.japicmp.JApiCmpExtension

plugins {
	id("junitbuild.japicmp")
	`java-library`
}

// TODO make configurable
val previousVersion = if (group == "org.junit.platform") "1.13.3" else "5.13.3"

val roseauDependencies = configurations.dependencyScope("roseau")
val roseauClasspath = configurations.resolvable("roseauClasspath") {
	extendsFrom(roseauDependencies.get())
}

dependencies {
	roseauDependencies(dependencyFromLibs("roseau-cli"))
}

val outputDir = layout.buildDirectory.dir("roseau")

val downloadPreviousReleaseJar = tasks.named<Download>("downloadPreviousReleaseJar")

val roseauDiff by tasks.registering(JavaExec::class) {
	dependsOn(downloadPreviousReleaseJar, tasks.jar)
	onlyIf { project.the<JApiCmpExtension>().enabled.get() }
	javaLauncher = project.javaToolchains.launcherFor {
		languageVersion = JavaLanguageVersion.of(21) // version required by roseau
	}
	mainClass = "io.github.alien.roseau.cli.RoseauCLI"
	classpath = files(roseauClasspath)
	argumentProviders.add(CommandLineArgumentProvider {
		listOf(
			"--classpath", configurations.compileClasspath.get().files.joinToString(":") { file -> file.absolutePath },
//            "--extractor", "SPOON",
			"--v1", downloadPreviousReleaseJar.get().outputFiles.single().absolutePath,
			"--v2", tasks.jar.get().archiveFile.get().asFile.absolutePath,
			"--verbose",
			"--diff",
			"--report", outputDir.get().file("breaking-changes.csv").asFile.absolutePath,
		)
	})
	doFirst {
		outputDir.get().asFile.mkdirs()
	}
}
