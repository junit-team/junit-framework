import de.undercouch.gradle.tasks.download.Download
import junitbuild.extensions.dependencyFromLibs

plugins {
    id("de.undercouch.download")
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

repositories {
    mavenCentral()
    maven(url = "https://jitpack.io")
}

val outputDir = layout.buildDirectory.dir("roseau")

val downloadPreviousReleaseJar by tasks.registering(Download::class) {
    src("https://repo1.maven.org/maven2/${project.group.toString().replace(".", "/")}/${project.name}/${previousVersion}/${project.name}-${previousVersion}.jar")
    dest(outputDir)
    overwrite(false)
}

val roseauDiff by tasks.registering(JavaExec::class) {
    dependsOn(downloadPreviousReleaseJar, tasks.jar)
    javaLauncher = project.javaToolchains.launcherFor {
        languageVersion = JavaLanguageVersion.of(21) // version required by roseau
    }
    mainClass = "io.github.alien.roseau.cli.RoseauCLI"
    classpath = files(roseauClasspath)
    argumentProviders.add(CommandLineArgumentProvider {
        listOf(
            "--classpath", configurations.compileClasspath.get().files.joinToString(":") { file -> file.absolutePath },
//            "--extractor", "SPOON",
            "--v1", outputDir.get().file("${project.name}-${previousVersion}.jar").asFile.absolutePath,
            "--v2", tasks.jar.get().archiveFile.get().asFile.absolutePath,
            "--verbose",
            "--diff",
            "--report", outputDir.get().file("breaking-changes.csv").asFile.absolutePath,
        )
    })
}
