/*
 * Copyright 2015-2026 the original author or authors.
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v2.0 which
 * accompanies this distribution and is available at
 *
 * https://www.eclipse.org/legal/epl-v20.html
 */

package example;

// tag::user_guide[]
import org.junit.Ignore;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.migrationsupport.EnableJUnit4MigrationSupport;

// @ExtendWith(IgnoreCondition.class)
@SuppressWarnings("removal")
@EnableJUnit4MigrationSupport
class IgnoredTestsDemo {

	@Ignore
	@Test
	void testWillBeIgnored() {
	}

	@Test
	void testWillBeExecuted() {
	}
}
// end::user_guide[]
