import org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_21

plugins {
    `kotlin-dsl`
}

tasks.compileJava {
    options.release = 21
}

kotlin {
    compilerOptions {
        jvmTarget = JVM_21
        freeCompilerArgs.add("-Xjdk-release=21")
    }
}
