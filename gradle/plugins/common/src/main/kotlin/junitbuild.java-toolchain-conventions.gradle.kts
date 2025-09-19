import org.jetbrains.kotlin.gradle.dsl.KotlinJvmProjectExtension

plugins {
	id("junitbuild.build-parameters")
}

project.pluginManager.withPlugin("java") {
	val defaultLanguageVersion = JavaLanguageVersion.of(25)
	val javaLanguageVersion = buildParameters.javaToolchain.version.map { JavaLanguageVersion.of(it) }.getOrElse(defaultLanguageVersion)
	val jvmImplementation = buildParameters.javaToolchain.implementation.map {
		when(it) {
			"j9" -> JvmImplementation.J9
			else -> throw InvalidUserDataException("Unsupported JDK implementation: $it")
		}
	}.getOrElse(JvmImplementation.VENDOR_SPECIFIC)

	val extension = the<JavaPluginExtension>()
	val javaToolchainService = the<JavaToolchainService>()

	extension.toolchain {
		languageVersion = javaLanguageVersion
		implementation = jvmImplementation
	}

	pluginManager.withPlugin("org.jetbrains.kotlin.jvm") {
		configure<KotlinJvmProjectExtension> {
			jvmToolchain {
				languageVersion = javaLanguageVersion
			}
		}
	}

	tasks.withType<JavaExec>().configureEach {
		javaLauncher = javaToolchainService.launcherFor(extension.toolchain)
		if (javaLanguageVersion != defaultLanguageVersion) {
			// Track exact version of Java to detect changes in behavior between EA builds sooner
			inputs.property("javaRuntimeVersion", javaLauncher.get().metadata.javaRuntimeVersion)
		}
	}

	tasks.withType<Test>().configureEach {
		if (javaLanguageVersion != defaultLanguageVersion) {
			// Track exact version of Java to detect changes in behavior between EA builds sooner
			inputs.property("javaRuntimeVersion", javaLauncher.get().metadata.javaRuntimeVersion)
		}
	}

	tasks.withType<JavaCompile>().configureEach {
		outputs.cacheIf { javaLanguageVersion == defaultLanguageVersion }
		doFirst {
			if (options.release.orNull == 8 && javaLanguageVersion.asInt() >= 20) {
				options.compilerArgs.add(
					"-Xlint:-options" // see https://github.com/junit-team/junit-framework/issues/3029
				)
			}
		}
	}

	tasks.withType<GroovyCompile>().configureEach {
		javaLauncher.set(javaToolchainService.launcherFor {
			// Groovy does not yet support JDK 19, see https://issues.apache.org/jira/browse/GROOVY-10569
			languageVersion = defaultLanguageVersion
		})
	}
}
