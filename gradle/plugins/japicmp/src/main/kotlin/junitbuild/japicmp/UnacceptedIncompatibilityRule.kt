package junitbuild.japicmp

import japicmp.model.JApiBehavior
import japicmp.model.JApiClass
import japicmp.model.JApiCompatibility
import japicmp.model.JApiField
import japicmp.model.JApiImplementedInterface
import me.champeau.gradle.japicmp.report.Violation
import me.champeau.gradle.japicmp.report.stdrules.AbstractRecordingSeenMembers

class UnacceptedIncompatibilityRule(params: Map<String, String>): AbstractRecordingSeenMembers() {

    val severitySource = SeveritySource(params)

    override fun maybeAddViolation(element: JApiCompatibility): Violation? {
        if (element is JApiClass && element.compatibilityChanges.isEmpty()) {
            // A member of the class breaks binary compatibility.
            // That will be handled when the member is passed to `maybeViolation`.
            return null
        }
        if (element is JApiImplementedInterface) {
            // The changes about the interface's methods will be reported already
            return null
        }
        if (isInNewClass(element)) {
            return null
        }
        return if (!element.isBinaryCompatible) {
            Violation.notBinaryCompatible(element, severitySource.determineSeverity(element))
        } else if (!element.isSourceCompatible) {
            Violation.any(element, severitySource.determineSeverity(element), "Is not source compatible")
        } else null
    }

    private fun isInNewClass(element: JApiCompatibility): Boolean {
        return when (element) {
            is JApiClass -> element.oldClass.isEmpty
            is JApiBehavior -> isInNewClass(element.getjApiClass())
            is JApiField -> isInNewClass(element.getjApiClass())
            else -> false
        }
    }
}
