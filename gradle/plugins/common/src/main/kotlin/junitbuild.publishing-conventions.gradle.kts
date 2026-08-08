import junitbuild.extensions.artifactGroup
import junitbuild.extensions.isSnapshot
import junitbuild.publishing.TEMP_MAVEN_REPO_ATTRIBUTE
import junitbuild.publishing.TEMP_MAVEN_REPO_ATTRIBUTE_VALUE
import junitbuild.release.VerifyBinaryArtifactsAreIdentical

plugins {
	`maven-publish`
	signing
	id("junitbuild.base-conventions")
	id("junitbuild.build-parameters")
	id("junitbuild.license")
}

group = buildParameters.publishing.group.getOrElse(artifactGroup)

val tempMavenRepoDir = layout.buildDirectory.dir("temp-maven-repo")

val clearTempMavenRepo = tasks.register<Delete>("clearTempMavenRepo") {
	delete(tempMavenRepoDir)
}

tasks.withType<PublishToMavenRepository>().named { it.endsWith("ToTempRepository") }.configureEach {
	dependsOn(clearTempMavenRepo)
}

configurations.consumable("tempMavenRepoElements") {
	attributes {
		attribute(TEMP_MAVEN_REPO_ATTRIBUTE, TEMP_MAVEN_REPO_ATTRIBUTE_VALUE)
		attribute(Category.CATEGORY_ATTRIBUTE, named(TEMP_MAVEN_REPO_ATTRIBUTE_VALUE))
	}
	outgoing.artifact(tempMavenRepoDir) {
		builtBy("publishAllPublicationsToTempRepository")
	}
}

// Verify that this project's freshly built artifacts are byte-for-byte identical
// to the ones already staged in the remote repository. Gated on the `java`
// plugin since jar-less projects (e.g. the BOM) publish no artifacts to compare.
pluginManager.withPlugin("java") {
	tasks.register<VerifyBinaryArtifactsAreIdentical>("verifyArtifactsInStagingRepositoryAreReproducible") {
		dependsOn("publishAllPublicationsToTempRepository")
		localRepoDir = tempMavenRepoDir
	}
}

val signArtifacts = buildParameters.publishing.signArtifacts.getOrElse(!(project.version.isSnapshot() || buildParameters.ci))

signing {
	useGpgCmd()
	sign(publishing.publications)
	isRequired = signArtifacts
}

tasks.withType<Sign>().configureEach {
	enabled = signArtifacts
}

publishing {
	repositories {
		maven {
			name = "temp"
			url = uri(tempMavenRepoDir)
		}
	}
	publications {
		create<MavenPublication>("maven") {
			version = buildParameters.jitpack.version
				.map { value ->
					val pattern = "(.+)-[0-9a-f]+-\\d+".toRegex()
					val matcher = requireNotNull(pattern.matchEntire(value)) {
						"Jitpack version does not match expected pattern: $pattern"
					}
					matcher.groupValues[1] + "-SNAPSHOT"
				}
				.getOrElse(project.version.toString())
			pom {
				name.set(provider {
					project.description ?: "${project.group}:${project.name}"
				})
				url = "https://junit.org/"
				scm {
					connection = "scm:git:git://github.com/junit-team/junit-framework.git"
					developerConnection = "scm:git:git://github.com/junit-team/junit-framework.git"
					url = "https://github.com/junit-team/junit-framework"
				}
				licenses {
					license {
						val license = project.the<License>()
						name = license.name
						url = license.url.toString()
					}
				}
				developers {
					developer {
						id = "bechte"
						name = "Stefan Bechtold"
						email = "stefan.bechtold@me.com"
					}
					developer {
						id = "jlink"
						name = "Johannes Link"
						email = "business@johanneslink.net"
					}
					developer {
						id = "marcphilipp"
						name = "Marc Philipp"
						email = "mail@marcphilipp.de"
					}
					developer {
						id = "mmerdes"
						name = "Matthias Merdes"
						email = "matthias.merdes@heidelpay.com"
					}
					developer {
						id = "sbrannen"
						name = "Sam Brannen"
						email = "sam@sambrannen.com"
					}
					developer {
						id = "sormuras"
						name = "Christian Stein"
						email = "sormuras@gmail.com"
					}
					developer {
						id = "juliette-derancourt"
						name = "Juliette de Rancourt"
						email = "derancourt.juliette@gmail.com"
					}
					developer {
						id = "mpkorstanje"
						name = "M.P. Korstanje"
						email = "mpkorstanje@junit.org"
					}
				}
			}
		}
	}
}
