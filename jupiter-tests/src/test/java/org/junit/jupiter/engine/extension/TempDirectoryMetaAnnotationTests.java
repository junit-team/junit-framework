/*
 * Copyright 2015-2025 the original author or authors.
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v2.0 which
 * accompanies this distribution and is available at
 *
 * https://www.eclipse.org/legal/epl-v20.html
 */

package org.junit.jupiter.engine.extension;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.nio.file.Files;
import java.nio.file.Path;

import org.jspecify.annotations.NullUnmarked;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.engine.AbstractJupiterTestEngineTests;

/**
 * Integration tests for the use of {@link TempDir} as a meta-annotation.
 *
 * @since 5.10
 */
@DisplayName("@TempDir as a meta-annotation")
class TempDirectoryMetaAnnotationTests extends AbstractJupiterTestEngineTests {

	@Test
	void annotationOnField() {
		executeTestsForClass(AnnotationOnFieldTestCase.class).testEvents()//
				.assertStatistics(stats -> stats.started(1).succeeded(1));
	}

	@Test
	void annotationOnParameter() {
		executeTestsForClass(AnnotationOnParameterTestCase.class).testEvents()//
				.assertStatistics(stats -> stats.started(1).succeeded(1));
	}

	@NullUnmarked
	static class AnnotationOnFieldTestCase {

		@CustomTempDir
		private Path tempDir;

		@Test
		void test() {
			assertTrue(Files.exists(tempDir));
		}

	}

	static class AnnotationOnParameterTestCase {

		@Test
		void test(@CustomTempDir Path tempDir) {
			assertTrue(Files.exists(tempDir));
		}

	}

	@TempDir
	@Target({ ElementType.FIELD, ElementType.PARAMETER })
	@Retention(RetentionPolicy.RUNTIME)
	@interface CustomTempDir {
	}

}
