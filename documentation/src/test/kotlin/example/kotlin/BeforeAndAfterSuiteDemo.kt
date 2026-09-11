/*
 * Copyright 2015-2026 the original author or authors.
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v2.0 which
 * accompanies this distribution and is available at
 *
 * https://www.eclipse.org/legal/epl-v20.html
 */

package example.kotlin

import org.junit.platform.suite.api.AfterSuite
import org.junit.platform.suite.api.BeforeSuite
import org.junit.platform.suite.api.SelectPackages
import org.junit.platform.suite.api.Suite

// tag::user_guide[]
@Suite
@SelectPackages("example")
class BeforeAndAfterSuiteDemo {
    companion object {
        @JvmStatic
        @BeforeSuite
        fun beforeSuite() {
            // executes before the test suite
        }

        @JvmStatic
        @AfterSuite
        fun afterSuite() {
            // executes after the test suite
        }
    }
}
// end::user_guide[]
