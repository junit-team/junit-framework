package junitbuild.compatibility.japicmp

import me.champeau.gradle.japicmp.report.PostProcessViolationsRule
import me.champeau.gradle.japicmp.report.Severity.accepted
import me.champeau.gradle.japicmp.report.ViolationCheckContextWithViolations

class AcceptedViolationsPostProcessRule(params: Map<String, String>) : PostProcessViolationsRule {

    val acceptedViolations = params["acceptedIncompatibilities"]!!.split(',').filter { !it.isEmpty() }.toSet()

    override fun execute(context: ViolationCheckContextWithViolations) {

        val actualViolations = context.violations.asSequence()
            .flatMap { it.value.asSequence() }
            .filter { it.severity == accepted }
            .map { it.member }
            .flatMap {
                sequenceOf(
                    it.fullyQualifiedClassName,
                    it.fullyQualifiedName
                )
            }
            .toSet()

        val diff = acceptedViolations - actualViolations

        require(diff.isEmpty()) {
            "The following elements are listed as 'accepted' but are not actually violations:\n- ${diff.joinToString("\n- ")}"
        }
    }

}
