import junitbuild.extensions.isSnapshot

plugins {
	`maven-publish`
	signing
	id("junitbuild.base-conventions")
	id("junitbuild.build-parameters")
}

@Suppress("UNCHECKED_CAST")
val jupiterProjects = rootProject.extra["jupiterProjects"] as List<Project>

@Suppress("UNCHECKED_CAST")
val platformProjects = rootProject.extra["platformProjects"] as List<Project>

@Suppress("UNCHECKED_CAST")
val vintageProjects = rootProject.extra["vintageProjects"] as List<Project>

group = buildParameters.publishing.group
	.getOrElse(when (project) {
		in jupiterProjects -> "org.junit.jupiter"
		in platformProjects -> "org.junit.platform"
		in vintageProjects -> "org.junit.vintage"
		else -> "org.junit"
	})

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
	publications {
		create<MavenPublication>("maven") {
			version = buildParameters.jitpack.version
				.map { value -> "(.+)-[0-9a-f]+-\\d+".toRegex().matchEntire(value)!!.groupValues[1] + "-SNAPSHOT" }
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
						val license = rootProject.extra["license"] as License
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
