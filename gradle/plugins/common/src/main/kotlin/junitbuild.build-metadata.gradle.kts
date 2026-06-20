import java.time.Instant
import java.time.OffsetDateTime
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeFormatterBuilder

plugins {
	id("junitbuild.build-parameters")
}

val dateFormatter = DateTimeFormatter.ISO_LOCAL_DATE
val timeFormatter = DateTimeFormatter.ofPattern("HH:mm:ss.SSSZ")

val buildTimeAndDate = buildParameters.manifest.buildTimestamp
	.map {
		it.toLongOrNull()
			?.let { s -> Instant.ofEpochSecond(s).atOffset(ZoneOffset.UTC) }
			?: DateTimeFormatterBuilder()
				.append(dateFormatter)
				.appendLiteral(' ')
				.append(timeFormatter)
				.toFormatter()
				.parse(it)
	}
	.orNull
	?: OffsetDateTime.now()

extra["buildDate"] = dateFormatter.format(buildTimeAndDate)
extra["buildTime"] = timeFormatter.format(buildTimeAndDate)
extra["buildRevision"] = providers.exec {
	commandLine("git", "rev-parse", "--verify", "HEAD")
}.standardOutput.asText.get().trim()
