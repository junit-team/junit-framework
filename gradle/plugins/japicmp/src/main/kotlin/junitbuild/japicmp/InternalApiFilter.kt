package junitbuild.japicmp

import japicmp.filter.BehaviorFilter
import japicmp.filter.ClassFilter
import japicmp.filter.FieldFilter
import javassist.ClassPool
import javassist.CtBehavior
import javassist.CtClass
import javassist.CtField
import javassist.NotFoundException
import javassist.bytecode.AnnotationsAttribute
import javassist.bytecode.AttributeInfo
import javassist.bytecode.annotation.EnumMemberValue

class InternalApiFilter : ClassFilter, BehaviorFilter, FieldFilter {

    override fun matches(clazz: CtClass): Boolean =
        isInternal(clazz)
                || isInternal(getEnclosingClass(clazz.name, clazz.classPool))

    override fun matches(behavior: CtBehavior) =
        isInternal(behavior.methodInfo.getAttribute(AnnotationsAttribute.visibleTag))
                || isInternal(behavior.declaringClass)

    override fun matches(field: CtField) =
        isInternal(field.fieldInfo.getAttribute(AnnotationsAttribute.visibleTag))
                || isInternal(field.declaringClass)

    private fun getEnclosingClass(className: String, classPool: ClassPool): CtClass? {
        if (className.contains("$")) {
            val enclosingClassName = className.substringBeforeLast("$")
            return try {
                classPool.get(enclosingClassName)
            } catch (e: NotFoundException) {
                getEnclosingClass(enclosingClassName, classPool)
            }
        }
        return null
    }

    private fun isInternal(clazz: CtClass?): Boolean =
        isInternal(clazz?.classFile?.getAttribute(AnnotationsAttribute.visibleTag))

    private fun isInternal(attribute: AttributeInfo?): Boolean {
        return (attribute as AnnotationsAttribute?)
            ?.annotations
            ?.firstOrNull { it.typeName == "org.apiguardian.api.API" }
            ?.let { (it.getMemberValue("status") as EnumMemberValue).value == "INTERNAL" } ?: false
    }

}
