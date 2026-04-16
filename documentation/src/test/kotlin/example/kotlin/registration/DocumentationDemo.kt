/*
 * Copyright 2015-2026 the original author or authors.
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v2.0 which
 * accompanies this distribution and is available at
 *
 * https://www.eclipse.org/legal/epl-v20.html
 */

package example.kotlin.registration

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.RegisterExtension
import java.nio.file.Path

// tag::user_guide[]
class DocumentationDemo {
    @JvmField
    @RegisterExtension
    val docs: DocumentationExtension =
        DocumentationExtension.forPath(lookUpDocsDir())

    @Test
    fun generateDocumentation() {
        // use this.docs ...
    }

    companion object {
        fun lookUpDocsDir(): Path? {
            // return path to docs dir
            return null
        }
    }
}
// end::user_guide[]
