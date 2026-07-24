package junitbuild.javadoc

import junitbuild.extensions.javaModuleName
import org.gradle.api.Project
import org.gradle.api.artifacts.Configuration
import org.gradle.api.artifacts.ProjectDependency
import org.gradle.api.tasks.TaskProvider
import org.gradle.api.tasks.javadoc.Javadoc
import org.gradle.external.javadoc.StandardJavadocDocletOptions
import org.gradle.kotlin.dsl.dependencies

class JavadocConventionsExtension(val project: Project, val dependencyScope: Configuration, val task: TaskProvider<Javadoc>) {

    fun addExtraModuleReferences(vararg projectDependencies: ProjectDependency) {
        project.dependencies {
            projectDependencies.forEach { dependencyScope(it) }
        }
        task.configure {
            options {
                (this as StandardJavadocDocletOptions).apply {
                    val referencedModuleNames = projectDependencies.joinToString(",") { it.javaModuleName }
                    addStringOption("-add-modules", referencedModuleNames)
                    addStringOption("-add-reads", "${project.javaModuleName}=$referencedModuleNames")
                }
            }
        }
    }

}
