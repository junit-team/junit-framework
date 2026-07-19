import java.time.Instant
import java.time.ZoneOffset.UTC
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeFormatterBuilder
import java.time.temporal.ChronoUnit.SECONDS

plugins {
	id("junitbuild.build-parameters")
}

val dateFormatter = DateTimeFormatter.ISO_LOCAL_DATE.withZone(UTC)
val timeFormatter = DateTimeFormatter.ofPattern("HH:mm:ss.SSSZ").withZone(UTC)

val buildTimestamp = buildParameters.sourceDateEpoch
	.map { value ->
		value.toLongOrNull()
			?.let { longValue -> Instant.ofEpochSecond(longValue) }
			?: DateTimeFormatterBuilder()
				.append(dateFormatter)
				.appendLiteral(' ')
				.append(timeFormatter)
				.toFormatter()
				.parse(value, Instant::from)
				.truncatedTo(SECONDS)
	}
	.getOrElse(Instant.now())

extra["buildTimestamp"] = buildTimestamp
extra["buildDate"] = dateFormatter.format(buildTimestamp)
extra["buildTime"] = timeFormatter.format(buildTimestamp)
extra["buildRevision"] = providers.exec {
	commandLine("git", "rev-parse", "--verify", "HEAD")
}.standardOutput.asText.get().trim()
