package junitbuild.japicmp

import japicmp.model.JApiCompatibility
import me.champeau.gradle.japicmp.report.Violation
import me.champeau.gradle.japicmp.report.stdrules.AbstractRecordingSeenMembers

// Required due to https://github.com/melix/japicmp-gradle-plugin/issues/56
class BreakingSuperClassChangeRule : AbstractRecordingSeenMembers() {

    override fun maybeAddViolation(element: JApiCompatibility): Violation? {
        return when {
            element.isBreakingSuperClassChange() -> Violation.error(element, "Is not binary compatible due to super class change")
            else -> null
        }
    }
}
