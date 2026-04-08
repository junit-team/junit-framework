/*
 * Copyright 2015-2026 the original author or authors.
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v2.0 which
 * accompanies this distribution and is available at
 *
 * https://www.eclipse.org/legal/epl-v20.html
 */

package example.kotlin.defaultmethods

// tag::user_guide[]
class StringTests :
    ComparableContract<String>,
    EqualsContract<String> {
    override fun createValue() = "banana"

    override fun createSmallerValue() = "apple" // 'a' < 'b' in "banana"

    override fun createNotEqualValue() = "cherry"
}
// end::user_guide[]
