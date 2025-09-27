import junitbuild.eclipse.EclipseConventionsExtension
import org.gradle.plugins.ide.eclipse.model.Classpath
import org.gradle.plugins.ide.eclipse.model.Library
import org.gradle.plugins.ide.eclipse.model.ProjectDependency
import org.gradle.plugins.ide.eclipse.model.SourceFolder

plugins {
	eclipse
}

val extension = extensions.create<EclipseConventionsExtension>("eclipseConventions").apply {
	hideModularity.convention(true)
}

eclipse {
	jdt {
		sourceCompatibility = JavaVersion.VERSION_21
		targetCompatibility = JavaVersion.VERSION_21
		file {
			// Set properties for org.eclipse.jdt.core.prefs
			withProperties {
				// Configure Eclipse projects with -release compiler flag.
				setProperty("org.eclipse.jdt.core.compiler.release", "enabled")
				// Configure Eclipse projects with -parameters compiler flag.
				setProperty("org.eclipse.jdt.core.compiler.codegen.methodParameters", "generate")
			}
		}
	}
	classpath.file.whenMerged {
		this as Classpath
		// Remove classpath entries for non-existent libraries added by various
		// plugins, such as "junit-jupiter-api/build/classes/kotlin/testFixtures".
		entries.removeIf { it is Library && !file(it.path).exists() }
		// Remove classpath entries for the code generator model used by the
		// Java Template Engine (JTE) which is used to generate the JRE enum and
		// dependent tests.
		entries.removeIf { it is ProjectDependency && it.path.equals("/code-generator-model") }
		// Remove classpath entries for anything used by the Gradle Wrapper.
		entries.removeIf { it is Library && it.path.contains("gradle/wrapper") }
		if (extension.hideModularity.get()) {
			entries.filterIsInstance<SourceFolder>().forEach {
				it.excludes.add("**/module-info.java")
			}
			entries.filterIsInstance<ProjectDependency>().forEach {
				it.entryAttributes.remove("module")
			}
			entries.filterIsInstance<Library>().forEach {
				it.entryAttributes.remove("module")
			}
		}
	}
}
