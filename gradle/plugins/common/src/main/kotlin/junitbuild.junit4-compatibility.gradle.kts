import junitbuild.extensions.dependencyFromLibs

plugins {
	`java-library`
}

val junit_4_12 = configurations.dependencyScope("junit_4_12")
val junit_4_12_classpath = configurations.resolvable("junit_4_12_classpath") {
	extendsFrom(configurations.testRuntimeClasspath.get())
	extendsFrom(junit_4_12.get())
}

dependencies {
	constraints {
		junit_4_12("junit:junit") {
			version {
				strictly("4.12")
			}
		}
	}
	pluginManager.withPlugin("junitbuild.osgi-conventions") {
		"osgiVerification"(dependencyFromLibs("junit4-bundle"))
	}
}

tasks {
	val test_4_12 = register("test_4_12", Test::class) {
		val test = testing.suites.named<JvmTestSuite>("test")
		testClassesDirs = files(test.map { it.sources.output.classesDirs })
		classpath = files(sourceSets.main.map { it.output }) + files(test.map { it.sources.output }) + junit_4_12_classpath.get()
	}
	check {
		dependsOn(test_4_12)
	}
}
