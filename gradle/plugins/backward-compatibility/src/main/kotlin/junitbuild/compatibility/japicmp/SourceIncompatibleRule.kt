package junitbuild.compatibility.japicmp

import japicmp.model.JApiCompatibility
import me.champeau.gradle.japicmp.report.Violation
import me.champeau.gradle.japicmp.report.stdrules.AbstractRecordingSeenMembers

class SourceIncompatibleRule : AbstractRecordingSeenMembers() {

    override fun maybeAddViolation(member: JApiCompatibility): Violation? = when {
        !member.isSourceCompatible -> Violation.error(member, "Is not source compatible")
        else -> null
    }
}
