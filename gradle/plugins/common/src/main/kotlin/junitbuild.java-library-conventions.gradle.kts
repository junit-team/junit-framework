import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar
import junitbuild.extensions.isSnapshot

plugins {
	`java-library`
	id("junitbuild.base-conventions")
	id("junitbuild.build-parameters")
	id("junitbuild.checkstyle-conventions")
	id("junitbuild.eclipse-conventions")
	id("junitbuild.jacoco-java-conventions")
}

val mavenizedProjects: List<Project> by rootProject.extra
val buildDate: String by rootProject.extra
val buildTime: String by rootProject.extra
val buildRevision: Any by rootProject.extra

val extension = extensions.create<JavaLibraryExtension>("javaLibrary")

java {
	modularity.inferModulePath = true
}

if (project in mavenizedProjects) {

	apply(plugin = "junitbuild.javadoc-conventions")
	apply(plugin = "junitbuild.publishing-conventions")
	apply(plugin = "junitbuild.osgi-conventions")
	apply(plugin = "junitbuild.backward-compatibility")

	java {
		withSourcesJar()
	}

	tasks.named<Jar>("sourcesJar").configure {
		duplicatesStrategy = DuplicatesStrategy.EXCLUDE
	}

	pluginManager.withPlugin("java-test-fixtures") {
		val javaComponent = components["java"] as AdhocComponentWithVariants
		javaComponent.withVariantsFromConfiguration(configurations["testFixturesApiElements"]) { skip() }
		javaComponent.withVariantsFromConfiguration(configurations["testFixturesRuntimeElements"]) { skip() }
	}

	configure<PublishingExtension> {
		publications {
			named<MavenPublication>("maven") {
				from(components["java"])
				if (!buildParameters.jitpack.version.isPresent) {
					versionMapping {
						allVariants {
							fromResolutionResult()
						}
					}
				}
				pom {
					description = provider { "Module \"${project.name}\" of JUnit" }
				}
			}
		}
	}

	if (!project.version.isSnapshot()) {
		configurations {
			compileClasspath {
				resolutionStrategy.failOnChangingVersions()
			}
			runtimeClasspath {
				resolutionStrategy.failOnChangingVersions()
			}
		}
	}

} else {
	tasks {
		jar {
			enabled = false
		}
		javadoc {
			enabled = false
		}
	}
}

tasks.withType<AbstractArchiveTask>().configureEach {
	isPreserveFileTimestamps = false
	isReproducibleFileOrder = true
	dirPermissions {
		unix("rwxr-xr-x")
	}
	filePermissions {
		unix("rw-r--r--")
	}
}

normalization {
	runtimeClasspath {
		metaInf {
			// Ignore inconsequential JAR manifest attributes such as timestamps and the commit checksum.
			// This is used when checking whether runtime classpaths, e.g. of test tasks, have changed and
			// improves cacheability of such tasks.
			ignoreAttribute("Built-By")
			ignoreAttribute("Build-Date")
			ignoreAttribute("Build-Time")
			ignoreAttribute("Build-Revision")
			ignoreAttribute("Created-By")
		}
	}
}

tasks.withType<Jar>().configureEach {
	from(rootDir) {
		include("LICENSE.md")
		into("META-INF")
	}
	from(rootDir) {
		include("NOTICE.md")
		rename {
			"LICENSE-notice.md"
		}
		into("META-INF")
	}
}

tasks.jar {
	manifest {
		attributes(
				"Created-By" to (buildParameters.manifest.createdBy.orNull
					?: "${System.getProperty("java.version")} (${System.getProperty("java.vendor")} ${System.getProperty("java.vm.version")})"),
				"Built-By" to buildParameters.manifest.builtBy.orElse("JUnit Team"),
				"Build-Date" to buildDate,
				"Build-Time" to buildTime,
				"Build-Revision" to buildRevision,
				"Specification-Title" to project.name,
				"Specification-Version" to (project.version as String).substringBefore('-'),
				"Specification-Vendor" to "junit.org",
				"Implementation-Title" to project.name,
				"Implementation-Version" to project.version,
				"Implementation-Vendor" to "junit.org"
		)
	}
}

tasks.withType<ShadowJar>().configureEach {
	outputs.doNotCacheIf("Shadow jar contains a Manifest with Build-Time") { true }
}

tasks.withType<JavaCompile>().configureEach {
	options.encoding = "UTF-8"
	options.compilerArgs.addAll(listOf(
		"-Xlint:all", // Enables all recommended warnings.
		"-Werror", // Terminates compilation when warnings occur.
		"-parameters", // Generates metadata for reflection on method parameters.
	))
}

tasks.compileJava {
	options.compilerArgs.addAll(listOf(
		"--module-version", "${project.version}"
	))
}

configurations {
	apiElements {
		attributes {
			attributeProvider(TargetJvmVersion.TARGET_JVM_VERSION_ATTRIBUTE, extension.mainJavaVersion.map { it.majorVersion.toInt() })
		}
	}
	runtimeElements {
		attributes {
			attributeProvider(TargetJvmVersion.TARGET_JVM_VERSION_ATTRIBUTE, extension.mainJavaVersion.map { it.majorVersion.toInt() })
		}
	}
}

tasks {
	compileJava {
		options.release = extension.mainJavaVersion.map { it.majorVersion.toInt() }
	}
	compileTestJava {
		options.release = extension.testJavaVersion.map { it.majorVersion.toInt() }
	}
}

afterEvaluate {
	pluginManager.withPlugin("groovy") {
		tasks.named<GroovyCompile>("compileGroovy").configure {
			// Groovy compiler does not support the --release flag.
			sourceCompatibility = extension.mainJavaVersion.get().majorVersion
			targetCompatibility = extension.mainJavaVersion.get().majorVersion
		}
		tasks.withType<GroovyCompile>().named { it.startsWith("compileTest") }.configureEach {
			// Groovy compiler does not support the --release flag.
			sourceCompatibility = extension.testJavaVersion.get().majorVersion
			targetCompatibility = extension.testJavaVersion.get().majorVersion
		}
	}
}

tasks {
	checkstyleMain {
		config = resources.text.fromFile(checkstyle.configDirectory.file("checkstyleMain.xml"))
	}
	checkstyleTest {
		config = resources.text.fromFile(checkstyle.configDirectory.file("checkstyleTest.xml"))
	}
}

pluginManager.withPlugin("java-test-fixtures") {
	tasks.named<Checkstyle>("checkstyleTestFixtures") {
		config = resources.text.fromFile(checkstyle.configDirectory.file("checkstyleTest.xml"))
	}
	tasks.named<JavaCompile>("compileTestFixturesJava") {
		options.release = extension.testJavaVersion.map { it.majorVersion.toInt() }
	}
}
