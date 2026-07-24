package junitbuild.javadoc

data class VersionNumber(val components: List<Int>) : Comparable<VersionNumber> {

    constructor(version: String) : this(version.split('.').map { it.toInt() })

    override fun compareTo(other: VersionNumber): Int {
        for (i in 0 until maxOf(this.components.size, other.components.size)) {
            val thisComponent = this.components.getOrElse(i) { 0 }
            val otherComponent = other.components.getOrElse(i) { 0 }
            if (thisComponent != otherComponent) {
                return thisComponent - otherComponent
            }
        }
        return 0
    }

}
