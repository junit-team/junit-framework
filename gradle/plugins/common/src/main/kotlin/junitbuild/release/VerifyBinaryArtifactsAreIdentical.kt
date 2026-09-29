package junitbuild.release

import org.gradle.api.DefaultTask
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.provider.Property
import org.gradle.api.provider.ProviderFactory
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputDirectory
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.TaskAction
import org.gradle.api.tasks.options.Option
import java.io.File
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse.BodyHandlers
import javax.inject.Inject

abstract class VerifyBinaryArtifactsAreIdentical @Inject constructor(providers: ProviderFactory): DefaultTask() {

    @get:InputDirectory
    @get:PathSensitive(PathSensitivity.RELATIVE)
    abstract val localRepoDir: DirectoryProperty

    @get:Input
    abstract val remoteRepoUrl: Property<String>

    @get:Internal
    abstract val remoteRepoBearerToken: Property<String>

    init {
        // Depends on contents of remote repository
        outputs.upToDateWhen { false }
        remoteRepoBearerToken.convention(providers.environmentVariable("MAVEN_CENTRAL_USER_TOKEN"))
    }

    @Suppress("unused")
    @Option(
        option = "remote-repo-url",
        description = "The URL of the remote repository to compare the local repository against"
    )
    fun remoteRepo(url: String) {
        remoteRepoUrl.set(url)
    }

    @TaskAction
    fun execute() {
        val localRootDir = localRepoDir.get().asFile
        val baseUrl = remoteRepoUrl.get()
        val mismatches = mutableListOf<Mismatch>()
        var numChecks = 0
        HttpClient.newHttpClient().use { httpClient ->
            localRootDir.walk().forEach { file ->
                if (file.isFile && file.name.endsWith(".jar.sha512") && !file.name.endsWith("-javadoc.jar.sha512")) {
                    val localSha512 = file.readText()
                    val relativeFile = file.relativeTo(localRootDir)
                    val url = URI.create("${baseUrl}/${relativeFile.path}")
                    logger.info("Checking {}...", url)
                    val request = HttpRequest.newBuilder().GET()
                        .uri(url)
                        .header("Authorization", "Bearer ${remoteRepoBearerToken.get()}")
                        .build()
                    val response = httpClient.send(request, BodyHandlers.ofString())
                    val remoteSha512 = if (response.statusCode() == 200) response.body() else "status=${response.statusCode()}"
                    if (localSha512 != remoteSha512) {
                        mismatches.add(Mismatch(relativeFile, localSha512, remoteSha512))
                    }
                    numChecks++
                }
            }
        }
        require(numChecks > 0) {
            "No files found to compare"
        }
        require(mismatches.isEmpty()) {
            "The following files have different SHA-512 checksums in the local and remote repositories:\n\n" +
                    mismatches.joinToString("\n\n") {
                        """
                            ${it.file}
                            local:  ${it.localSha512}
                            remote: ${it.remoteSha512}
                        """.trimIndent()
                    }
        }
    }

    private data class Mismatch(val file: File, val localSha512: String, val remoteSha512: String)
}
