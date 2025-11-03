import junitbuild.modules.WriteModuleArtifactMapping

plugins {
	`java-library`
}

val moduleRuntimeScope = configurations.dependencyScope("moduleRuntimeScope")
val moduleRuntimeClasspath = configurations.resolvable("moduleRuntimeClasspath") {
	extendsFrom(moduleRuntimeScope.get())
	extendsFrom(configurations.compileOnlyApi.get())
}

dependencies {
	moduleRuntimeScope(project)
}

val moduleArtifactMapping by tasks.registering(WriteModuleArtifactMapping::class) {
	from(moduleRuntimeClasspath)
	propertiesFile = base.libsDirectory.file(base.archivesName.map { "${it}-${project.version}-module.properties" })
}

tasks.assemble {
	dependsOn(moduleArtifactMapping)
}
