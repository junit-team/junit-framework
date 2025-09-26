package junitbuild.japicmp

import japicmp.model.JApiClass
import japicmp.model.JApiImplementedInterface
import me.champeau.gradle.japicmp.report.Severity.accepted
import me.champeau.gradle.japicmp.report.Violation
import me.champeau.gradle.japicmp.report.ViolationTransformer
import java.util.*

class AcceptedViolationSuppressor(params: Map<String, String>) : ViolationTransformer {

    val acceptedIncompatibilities = params["acceptedIncompatibilities"]!!.split(',').filter { !it.isEmpty() }.toSet()

    override fun transform(type: String?, violation: Violation): Optional<Violation> = when {
        violation.isRedundant() -> Optional.empty()
        violation.isAccepted() -> Optional.of(violation.withSeverity(accepted))
        else -> Optional.of(violation)
    }

    private fun Violation.isRedundant(): Boolean =
        // The changes about the interface's methods will be reported already
        member is JApiImplementedInterface
                // A member of the class breaks binary or source compatibility and will be reported
                || (member is JApiClass && member.compatibilityChanges.none { !it.isBinaryCompatible || !it.isSourceCompatible } && !member.isBreakingSuperClassChange())

    private fun Violation.isAccepted(): Boolean =
        acceptedIncompatibilities.contains(member.fullyQualifiedName)
                || acceptedIncompatibilities.contains(member.fullyQualifiedClassName)

}
