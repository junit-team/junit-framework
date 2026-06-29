import junitbuild.extensions.dependencyFromLibs
import junitbuild.generator.GenerateJreRelatedSourceCode
import java.time.Year

plugins {
	java
}

val templates = sourceSets.create("templates")
val templatesCompileOnly = configurations[templates.compileOnlyConfigurationName]

dependencies {
	templatesCompileOnly(dependencyFromLibs("jte"))
	templatesCompileOnly("junitbuild.base:code-generator-model")
}

val license = rootProject.extra["license"] as License
val rootTargetDir = layout.buildDirectory.dir("generated/sources/jte")

val generateCode = tasks.register("generateCode") {
	dependsOn(tasks.withType<GenerateJreRelatedSourceCode>())
	group = LifecycleBasePlugin.BUILD_GROUP
	description = "Generates JRE-related source code."
}

tasks.withType<GenerateJreRelatedSourceCode>().configureEach {
	licenseHeaderFile.convention(license.headerFile)
	licenseHeaderYear.convention(Year.now())
	additionalTemplateParameters.convention(emptyMap())
}

sourceSets.named { it != templates.name }.configureEach {

	val sourceSetName = name

	val task = tasks.register(getTaskName("generateJreRelated", "SourceCode"), GenerateJreRelatedSourceCode::class) {
		templateDir.convention(layout.dir(provider {
			templates.resources.srcDirs.single().resolve(sourceSetName)
		}))
		targetDir.convention(rootTargetDir.map { it.dir(sourceSetName) })
	}

	java.srcDir(task.map { it.targetDir })
}
