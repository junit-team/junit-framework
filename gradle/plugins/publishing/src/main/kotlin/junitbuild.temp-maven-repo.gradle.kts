import junitbuild.extensions.capitalized
import junitbuild.release.VerifyBinaryArtifactsAreIdentical
import org.gradle.internal.extensions.core.extra

val tempRepoName = "temp"
val tempRepoDir = layout.buildDirectory.dir("repo").get().asFile
extra["tempRepoName"] = tempRepoName
extra["tempRepoDir"] = tempRepoDir

val clearTempRepoDir = tasks.register("clearTempRepoDir") {
	val dir = tempRepoDir
	doFirst {
		dir.deleteRecursively()
	}
}

val publishAllSubprojectsToTempRepository = tasks.register("publishAllSubprojectsToTempRepository")

tasks.register<VerifyBinaryArtifactsAreIdentical>("verifyArtifactsInStagingRepositoryAreReproducible") {
	dependsOn(publishAllSubprojectsToTempRepository)
	localRepoDir.set(tempRepoDir)
}

subprojects {
	pluginManager.withPlugin("maven-publish") {
		configure<PublishingExtension> {
			repositories {
				maven {
					name = tempRepoName
					url = uri(tempRepoDir)
				}
			}
		}
		val publishingTasks = tasks.withType<PublishToMavenRepository>()
			.named { it.endsWith("To${tempRepoName.capitalized()}Repository") }
		publishingTasks.configureEach {
			dependsOn(clearTempRepoDir)
		}
		publishAllSubprojectsToTempRepository {
			dependsOn(publishingTasks)
		}
	}
}
