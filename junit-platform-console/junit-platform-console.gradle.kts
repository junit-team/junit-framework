import junitbuild.extensions.javaModuleName
import junitbuild.java.UpdateJarAction
import net.ltgt.gradle.errorprone.errorprone
import net.ltgt.gradle.nullaway.nullaway

plugins {
	id("junitbuild.java-library-conventions")
	id("junitbuild.java-nullability-conventions")
	id("junitbuild.shadow-conventions")
}

description = "JUnit Platform Console"

dependencies {
	api(platform(projects.junitBom))
	api(projects.junitPlatformReporting)

	compileOnlyApi(libs.apiguardian)
	compileOnlyApi(libs.jspecify)

	shadowed(libs.picocli)

	osgiVerification(projects.junitJupiterEngine)
	osgiVerification(projects.junitPlatformLauncher)
	osgiVerification(libs.openTestReporting.tooling.spi)
}

tasks {
	compileJava {
		options.compilerArgs.addAll(listOf(
			"--add-modules", "info.picocli",
			"--add-reads", "${javaModuleName}=info.picocli"
		))
		options.errorprone.nullaway {
			excludedFieldAnnotations.addAll(
				"picocli.CommandLine.ArgGroup",
				"picocli.CommandLine.Mixin",
				"picocli.CommandLine.Spec",
			)
		}
	}
	javadoc {
		(options as StandardJavadocDocletOptions).apply {
			addStringOption("-add-modules", "info.picocli")
			addStringOption("-add-reads", "${javaModuleName}=info.picocli")
		}
	}
	shadowJar {
		exclude("META-INF/**/module-info.class")
		relocate("picocli", "org.junit.platform.console.shadow.picocli")
		from(projectDir) {
			include("LICENSE-picocli.md")
			into("META-INF")
		}
		doLast(objects.newInstance(UpdateJarAction::class).apply {
			javaLauncher = project.javaToolchains.launcherFor(java.toolchain)
			args.addAll(
				"--file", archiveFile.get().asFile.absolutePath,
				"--main-class", "org.junit.platform.console.ConsoleLauncher",
			)
		})
		bundle {
			// Ignore warning for package that is only exported as "INTERNAL"
			bnd("""
				-fixupmessages.picocli.export: "Export org.junit.platform.console.options";is:=ignore
			""")
		}
	}
	codeCoverageClassesJar {
		exclude("org/junit/platform/console/options/ConsoleUtils.class")
	}
	jar {
		manifest {
			attributes("Main-Class" to "org.junit.platform.console.ConsoleLauncher")
		}
	}
}
