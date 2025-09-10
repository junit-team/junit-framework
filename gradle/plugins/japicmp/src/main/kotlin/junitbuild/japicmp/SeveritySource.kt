package junitbuild.japicmp

import japicmp.model.JApiBehavior
import japicmp.model.JApiClass
import japicmp.model.JApiCompatibility
import japicmp.model.JApiField
import me.champeau.gradle.japicmp.report.Severity

class SeveritySource(params: Map<String, String>) {

    val acceptedIncompatibilities = params["acceptedIncompatibilities"]!!.split(',')

    fun determineSeverity(element: JApiCompatibility): Severity {
        return if (isAcceptedIncompatibility(element)) Severity.accepted else Severity.error
    }

    private fun isAcceptedIncompatibility(element: JApiCompatibility): Boolean =
        acceptedIncompatibilities.contains(getFullyQualifiedName(element))
                || acceptedIncompatibilities.contains(getFullyQualifiedClassName(element))

    private fun getFullyQualifiedName(element: JApiCompatibility): String {
        if (element is JApiClass) {
            return getFullyQualifiedClassName(element)
        }
        if (element is JApiBehavior) {
            return "${getFullyQualifiedClassName(element)}#${element.name}"
        }
        if (element is JApiField) {
            return "${getFullyQualifiedClassName(element)}#${element.name}"
        }
        throw IllegalArgumentException("Could not determine fully-qualified name for $element")
    }

    private fun getFullyQualifiedClassName(element: JApiCompatibility): String {
        if (element is JApiClass) {
            return element.fullyQualifiedName
        }
        if (element is JApiBehavior) {
            return element.getjApiClass().fullyQualifiedName
        }
        if (element is JApiField) {
            return element.getjApiClass().fullyQualifiedName
        }
        throw IllegalArgumentException("Could not determine fully-qualified class name for $element")
    }
}
