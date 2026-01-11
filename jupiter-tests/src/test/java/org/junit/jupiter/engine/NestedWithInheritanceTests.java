/*
 * Copyright 2015-2026 the original author or authors.
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v2.0 which
 * accompanies this distribution and is available at
 *
 * https://www.eclipse.org/legal/epl-v20.html
 */

package org.junit.jupiter.engine;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.List;

import org.jspecify.annotations.NullUnmarked;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@NullUnmarked
class NestedWithInheritanceTests extends SuperClass {

	static List<String> lifecycleInvokingClassNames;

	static String OUTER = NestedWithInheritanceTests.class.getSimpleName();
	static String NESTED = NestedClass.class.getSimpleName();
	static String NESTEDNESTED = NestedClass.NestedNestedClass.class.getSimpleName();

	@Nested
	class NestedClass extends SuperClass {

		@Test
		public void test() {
			assertThat(lifecycleInvokingClassNames).containsExactly(OUTER, NESTED);
		}

		@Nested
		class NestedNestedClass extends SuperClass {

			@Test
			public void test() {
				assertThat(lifecycleInvokingClassNames).containsExactly(OUTER, NESTED, NESTEDNESTED);
			}
		}

	}

}

class SuperClass {

	@BeforeAll
	static void setup() {
		NestedWithInheritanceTests.lifecycleInvokingClassNames = new ArrayList<>();
	}

	@BeforeEach
	public void beforeEach() {
		String invokingClass = this.getClass().getSimpleName();
		NestedWithInheritanceTests.lifecycleInvokingClassNames.add(invokingClass);
	}

}
