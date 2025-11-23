import org.antora.gradle.AntoraExtension

plugins {
	id("org.antora")
	id("io.spring.antora.generate-antora-yml")
}

repositories {
	// Redefined here because the Node.js plugin adds a repo
	mavenCentral()
}

tasks.register("generateAntoraResources") {
	dependsOn("generateAntoraYml")
}

val generateAntoraPlaybook by tasks.registering(Copy::class) {

	val gitRepoRoot = providers.exec {
		commandLine("git", "worktree", "list", "--porcelain", "-z")
	}.standardOutput.asText.map { it.substringBefore('\u0000').substringAfter(' ') }

	val gitBranchName = providers.exec {
		commandLine("git", "rev-parse", "--abbrev-ref", "HEAD")
	}.standardOutput.asText.map { it.trim() }

	from(layout.projectDirectory.file("antora-playbook.yml").asFile)
	filter { line ->
		var result = line
		if (line.contains("@GIT_REPO_ROOT@")) {
			result = result.replace("@GIT_REPO_ROOT@", gitRepoRoot.get())
		}
		if (line.contains("@GIT_BRANCH_NAME@")) {
			result = result.replace("@GIT_BRANCH_NAME@", gitBranchName.get())
		}
		return@filter result
	}
	into(layout.buildDirectory.dir("antora"))
}

the<AntoraExtension>().apply {
	setOptions(mapOf("clean" to true, "stacktrace" to true, "fetch" to true))
	playbook = generateAntoraPlaybook.map { it.rootSpec.destinationDir.resolve("antora-playbook.yml") }
}
