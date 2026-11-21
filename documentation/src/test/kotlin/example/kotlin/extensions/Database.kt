/*
 * Copyright 2015-2026 the original author or authors.
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v2.0 which
 * accompanies this distribution and is available at
 *
 * https://www.eclipse.org/legal/epl-v20.html
 */

package example.kotlin.extensions

/**
 * Simple stand-in for an expensive resource such as a database.
 */
class Database {
    private val data = mutableMapOf<String, String>()

    fun insert(
        key: String,
        value: String
    ) {
        data[key] = value
    }

    fun find(key: String): String? = data[key]
}
