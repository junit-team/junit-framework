package junitbuild.japicmp

import japicmp.model.JApiClass
import japicmp.model.JApiCompatibility
import me.champeau.gradle.japicmp.report.Violation
import me.champeau.gradle.japicmp.report.stdrules.AbstractRecordingSeenMembers

class UnacceptedSuperClassChangeRule(params: Map<String, String>): AbstractRecordingSeenMembers() {

    val severitySource = SeveritySource(params)

    override fun maybeAddViolation(element: JApiCompatibility): Violation? {
        if (element !is JApiClass || element.superclass.isBinaryCompatible || element.superclass.compatibilityChanges.isEmpty()) {
            return null
        }

        val oldClass = element.oldClass
        val newClass = element.newClass
        if (!oldClass.isPresent || !newClass.isPresent) {
            // breaking change would be reported
            return null
        }

        return Violation.notBinaryCompatible(element, severitySource.determineSeverity(element))
    }
}
