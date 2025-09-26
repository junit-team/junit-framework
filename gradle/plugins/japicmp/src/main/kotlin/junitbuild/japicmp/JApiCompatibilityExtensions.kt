package junitbuild.japicmp

import japicmp.model.JApiBehavior
import japicmp.model.JApiClass
import japicmp.model.JApiCompatibility
import japicmp.model.JApiField

internal val JApiCompatibility.fullyQualifiedName: String
    get() = when (this) {
        is JApiClass -> fullyQualifiedClassName
        is JApiBehavior -> "${fullyQualifiedClassName}#${name}"
        is JApiField -> "${fullyQualifiedClassName}#${name}"
        else -> throw IllegalArgumentException("Could not determine fully-qualified name for $this")
    }

internal val JApiCompatibility.fullyQualifiedClassName: String
    get() = when (this) {
        is JApiClass -> fullyQualifiedName
        is JApiBehavior -> getjApiClass().fullyQualifiedName
        is JApiField -> getjApiClass().fullyQualifiedName
        else -> throw IllegalArgumentException("Could not determine fully-qualified class name for $this")
    }

internal fun JApiCompatibility.isBreakingSuperClassChange(): Boolean {
    if (this !is JApiClass || superclass.isBinaryCompatible || superclass.compatibilityChanges.isEmpty()) {
        return false
    }
    // breaking change would otherwise be reported
    return oldClass.isPresent && newClass.isPresent

}
