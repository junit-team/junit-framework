import com.github.gradle.node.npm.task.NpxTask

plugins {
	id("com.github.node-gradle.node")
	id("io.spring.antora.generate-antora-yml")
	id("junitbuild.build-parameters")
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
	into(layout.buildDirectory.dir("antora-playbook"))
}

node {
	download = buildParameters.antora.downloadNode
	version = providers.fileContents(layout.projectDirectory.file(".tool-versions")).asText.map {
		it.substringAfter("nodejs").trim()
	}
}

tasks.npmInstall {
	args.addAll("--no-audit", "--no-package-lock", "--no-fund")
}

tasks.register<NpxTask>("antora") {
	dependsOn(tasks.npmInstall)
	description = "Runs Antora to generate a documentation site described by the playbook file."

	command = "antora"
	args.addAll("--clean", "--stacktrace", "--fetch", "--log-format=pretty", "--log-level=all")

	args.add("--to-dir")
	val outputDir = layout.buildDirectory.dir("antora-site")
	args.add(outputDir.map { it.asFile.toRelativeString(layout.projectDirectory.asFile) })
	outputs.dir(outputDir)

	outputs.upToDateWhen { false } // not all inputs are tracked

	val playbook = generateAntoraPlaybook.map { it.rootSpec.destinationDir.resolve("antora-playbook.yml") }
	args.add(playbook.map { it.toRelativeString(layout.projectDirectory.asFile) })
	inputs.file(playbook)

	execOverrides {
		environment["IS_TTY"] = true
	}
}
