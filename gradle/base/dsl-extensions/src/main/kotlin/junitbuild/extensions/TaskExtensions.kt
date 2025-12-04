package junitbuild.extensions

import org.gradle.api.Task
import org.gradle.api.file.ArchiveOperations
import org.gradle.internal.os.OperatingSystem
import org.gradle.kotlin.dsl.newInstance
import javax.inject.Inject

fun Task.trackOperationSystemAsInput() =
    inputs.property("os", OperatingSystem.current().familyName)

fun <T> Task.withArchiveOperations(action: (ArchiveOperations) -> T): T =
    archiveOperations.run { action(this) }

private val Task.archiveOperations: ArchiveOperations
    get() = project.objects.newInstance(DummyObject::class).archiveOperations

private abstract class DummyObject {
    @get:Inject
    abstract val archiveOperations: ArchiveOperations
}
