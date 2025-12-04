import junitbuild.extensions.dependencyFromLibs
import junitbuild.generator.GenerateJreRelatedSourceCode

plugins {
	java
}

val templates by sourceSets.creating
val templatesCompileOnly = configurations[templates.compileOnlyConfigurationName]

dependencies {
	templatesCompileOnly(dependencyFromLibs("jte"))
	templatesCompileOnly("junitbuild.base:code-generator-model")
}

val license: License by rootProject.extra
val rootTargetDir = layout.buildDirectory.dir("generated/sources/jte")
val generateCode by tasks.registering

sourceSets.named { it != templates.name }.configureEach {

	val sourceSetName = name

	val task = tasks.register(getTaskName("generateJreRelated", "SourceCode"), GenerateJreRelatedSourceCode::class) {
		templateDir.convention(layout.dir(provider {
			templates.resources.srcDirs.single().resolve(sourceSetName)
		}))
		targetDir.convention(rootTargetDir.map { it.dir(sourceSetName) })
		licenseHeaderFile.convention(license.headerFile)
	}

	java.srcDir(task.map { it.targetDir })

	generateCode {
		dependsOn(task)
	}
}
