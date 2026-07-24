rootProject.name = "base"

dependencyResolutionManagement {
    versionCatalogs {
        create("libs") {
            from(files("../libs.versions.toml"))
        }
    }
    repositories {
        gradlePluginPortal()
    }
}

include("build-parameters")
include("code-generator-model")
include("dsl-extensions")
