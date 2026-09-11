package junitbuild.metadata

import buildparameters.BuildParametersExtension
import org.gradle.api.Project
import org.gradle.api.provider.Property
import org.gradle.api.provider.Provider
import org.gradle.api.provider.ProviderFactory
import org.gradle.api.services.BuildService
import org.gradle.api.services.BuildServiceParameters
import org.gradle.kotlin.dsl.registerIfAbsent
import org.gradle.kotlin.dsl.the
import java.time.Instant
import java.time.ZoneOffset.UTC
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeFormatterBuilder
import java.time.temporal.ChronoUnit.SECONDS
import javax.inject.Inject

abstract class BuildMetadata @Inject constructor(private val providers: ProviderFactory) :
    BuildService<BuildMetadata.Params> {

    interface Params : BuildServiceParameters {
        // See the 'sourceDateEpoch' build parameter; may be a number of seconds since the epoch
        // or a formatted date/time. Absent unless overridden, in which case the timestamp is `now`.
        val sourceDateEpoch: Property<String>
    }

    private val dateFormatter: DateTimeFormatter = DateTimeFormatter.ISO_LOCAL_DATE.withZone(UTC)
    private val timeFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("HH:mm:ss.SSSZ").withZone(UTC)

    val buildTimestamp: Instant by lazy {
        parameters.sourceDateEpoch.orNull?.let { value ->
            value.toLongOrNull()
                ?.let { Instant.ofEpochSecond(it) }
                ?: DateTimeFormatterBuilder()
                    .append(dateFormatter)
                    .appendLiteral(' ')
                    .append(timeFormatter)
                    .toFormatter()
                    .parse(value, Instant::from)
                    .truncatedTo(SECONDS)
        } ?: Instant.now()
    }

    val buildDate: String by lazy { dateFormatter.format(buildTimestamp) }
    val buildTime: String by lazy { timeFormatter.format(buildTimestamp) }

    val buildRevision: String by lazy {
        providers.exec {
            commandLine("git", "rev-parse", "--verify", "HEAD")
        }.standardOutput.asText.get().trim()
    }
}

val Project.buildMetadata: Provider<BuildMetadata>
    get() {
        val sourceDateEpoch = the<BuildParametersExtension>().sourceDateEpoch
        return gradle.sharedServices.registerIfAbsent("buildMetadata", BuildMetadata::class) {
            parameters.sourceDateEpoch.set(sourceDateEpoch)
        }
    }
