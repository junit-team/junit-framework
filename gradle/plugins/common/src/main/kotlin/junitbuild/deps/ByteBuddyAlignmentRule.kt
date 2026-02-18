package junitbuild.deps

import org.gradle.api.artifacts.ComponentMetadataContext
import org.gradle.api.artifacts.ComponentMetadataRule

abstract class ByteBuddyAlignmentRule : ComponentMetadataRule {
    override fun execute(ctx: ComponentMetadataContext) {
        ctx.details.run {
            if (id.group.startsWith("net.bytebuddy")) {
                belongsTo("net.bytebuddy:byte-buddy-virtual-platform:${id.version}")
            }
        }
    }
}
