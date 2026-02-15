package junitbuild.antora

import org.gradle.api.file.DirectoryProperty

interface AntoraConfiguration {
    val siteDir: DirectoryProperty
}
