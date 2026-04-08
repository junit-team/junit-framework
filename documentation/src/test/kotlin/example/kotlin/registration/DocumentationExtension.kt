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

import org.junit.jupiter.api.extension.AfterEachCallback
import org.junit.jupiter.api.extension.ExtensionContext
import java.nio.file.Path

class DocumentationExtension private constructor(
    private val path: Path?
) : AfterEachCallback {
    override fun afterEach(context: ExtensionContext) {
        // no-op for demo
    }

    companion object {
        @JvmStatic
        fun forPath(path: Path?): DocumentationExtension = DocumentationExtension(path)
    }
}
