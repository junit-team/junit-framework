package junitbuild.publishing

import org.gradle.api.attributes.Attribute

val TEMP_MAVEN_REPO_ATTRIBUTE: Attribute<String> =
    Attribute.of("junitbuild.temp-maven-repo", String::class.java)

const val TEMP_MAVEN_REPO_ATTRIBUTE_VALUE = "temp-maven-repo"
