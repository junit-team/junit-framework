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

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Integration tests that verify support for {@link BeforeEach} and {@link AfterEach}
 * when used as meta-annotations in the {@link JupiterTestEngine}.
 *
 * @since 5.0
 * @see BeforeAllAndAfterAllComposedAnnotationTests
 */
class BeforeEachAndAfterEachComposedAnnotationTests extends AbstractJupiterTestEngineTests {

	private static final List<String> methodsInvoked = new ArrayList<>();

	@Test
	void beforeEachAndAfterEachAsMetaAnnotations() {
		executeTestsForClass(TestCase.class).testEvents().assertStatistics(stats -> stats.started(1).succeeded(1));

		assertThat(methodsInvoked).containsExactly("beforeEach", "test", "afterEach");
	}

	static class TestCase {

		@CustomBeforeEach
		void beforeEach() {
			methodsInvoked.add("beforeEach");
		}

		@Test
		void test() {
			methodsInvoked.add("test");
		}

		@CustomAfterEach
		void afterEach() {
			methodsInvoked.add("afterEach");
		}

	}

	@BeforeEach
	@Retention(RetentionPolicy.RUNTIME)
	private @interface CustomBeforeEach {
	}

	@AfterEach
	@Retention(RetentionPolicy.RUNTIME)
	private @interface CustomAfterEach {
	}

}
