package no.marec.gradle

import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.util.*

class BuildProperties {
   val buildTime: ZonedDateTime = ZonedDateTime.now().truncatedTo(ChronoUnit.SECONDS)
   val buildTimestamp: String = DateTimeFormatter.ofPattern("yyyyMMdd-HHmm").format(buildTime)
   val buildYear: String = buildTime.year.toString()
   val buildDate: String = DateTimeFormatter.ofPattern("MMMM d, yyyy", Locale.ENGLISH).format(buildTime)

   val gitCommit: String = ProcessBuilder("git", "rev-parse", "HEAD").start().inputReader().readText().trim()
}
