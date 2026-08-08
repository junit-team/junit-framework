import junitbuild.extensions.mavenizedProjects
import junitbuild.publishing.TEMP_MAVEN_REPO_ATTRIBUTE
import junitbuild.publishing.TEMP_MAVEN_REPO_ATTRIBUTE_VALUE
import junitbuild.release.VerifyBinaryArtifactsAreIdentical

val tempMavenRepo = configurations.dependencyScope("tempMavenRepo")
val allTempMavenRepos = configurations.resolvable("tempMavenRepoClasspath") {
	extendsFrom(tempMavenRepo.get())
	attributes {
		attribute(TEMP_MAVEN_REPO_ATTRIBUTE, TEMP_MAVEN_REPO_ATTRIBUTE_VALUE)
	}
}

dependencies {
	tempMavenRepo(project(":junit-bom"))
	mavenizedProjects.forEach { tempMavenRepo(it) }
}

val mergedRepoDir = layout.buildDirectory.dir("repo")

val mergeTempRepositories = tasks.register<Sync>("mergeTempRepositories") {
	from(allTempMavenRepos)
	into(mergedRepoDir)
	duplicatesStrategy = DuplicatesStrategy.FAIL
}

tasks.register<VerifyBinaryArtifactsAreIdentical>("verifyArtifactsInStagingRepositoryAreReproducible") {
	dependsOn(mergeTempRepositories)
	localRepoDir = mergedRepoDir
}
