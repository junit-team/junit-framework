/*
 * Copyright 2015-2026 the original author or authors.
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v2.0 which
 * accompanies this distribution and is available at
 *
 * https://www.eclipse.org/legal/epl-v20.html
 */

package org.junit.jupiter.engine.extension;

import static java.nio.file.Files.deleteIfExists;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.fail;
import static org.junit.jupiter.api.Constants.DEFAULT_TEMP_DIR_CLEANUP_MODE_PROPERTY_NAME;
import static org.junit.jupiter.api.condition.OS.WINDOWS;
import static org.junit.jupiter.api.io.CleanupMode.ALWAYS;
import static org.junit.jupiter.api.io.CleanupMode.NEVER;
import static org.junit.jupiter.api.io.CleanupMode.ON_SUCCESS;
import static org.junit.platform.engine.discovery.DiscoverySelectors.selectClass;
import static org.junit.platform.engine.discovery.DiscoverySelectors.selectClasses;
import static org.junit.platform.engine.discovery.DiscoverySelectors.selectMethod;
import static org.junit.platform.launcher.core.LauncherDiscoveryRequestBuilder.request;

import java.io.IOException;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.logging.Level;
import java.util.logging.LogRecord;

import org.jspecify.annotations.NullUnmarked;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.junit.jupiter.api.condition.DisabledOnOs;
import org.junit.jupiter.api.condition.EnabledOnOs;
import org.junit.jupiter.api.fixtures.TrackLogRecords;
import org.junit.jupiter.api.io.CleanupMode;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.api.io.TempDir.LogLevel;
import org.junit.jupiter.engine.AbstractJupiterTestEngineTests;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.platform.commons.logging.LogRecordListener;
import org.junit.platform.launcher.LauncherDiscoveryRequest;

/**
 * Test that {@linkplain TempDir temporary directories} are not deleted with
 * {@link CleanupMode#NEVER}, are deleted with {@link CleanupMode#ON_SUCCESS}
 * but only if the test passes, and are always deleted with {@link CleanupMode#ALWAYS}.
 *
 * @see CleanupMode
 * @see TempDir
 * @since 5.9
 */
class TempDirectoryCleanupTests extends AbstractJupiterTestEngineTests {

	@Nested
	@NullUnmarked
	class TempDirFieldTests {

		private static Path defaultFieldDir;
		private static Path neverFieldDir;
		private static Path alwaysFieldDir;
		private static Path onSuccessFailingFieldDir;
		private static Path onSuccessPassingFieldDir;
		private static Path onSuccessPassingParameterDir;

		/**
		 * Ensure the cleanup mode defaults to ALWAYS for fields.
		 * <p/>
		 * Expect the TempDir to be cleaned up.
		 */
		@Test
		void cleanupModeDefaultField() {
			LauncherDiscoveryRequest request = request()//
					.selectors(selectMethod(DefaultFieldCase.class, "testDefaultField"))//
					.build();
			executeTests(request);

			assertThat(defaultFieldDir).doesNotExist();
		}

		/**
		 * Ensure that a custom, global cleanup mode is used for fields.
		 * <p/>
		 * Expect the TempDir NOT to be cleaned up if set to NEVER.
		 */
		@Test
		void cleanupModeCustomDefaultField() {
			LauncherDiscoveryRequest request = request()//
					.configurationParameter(DEFAULT_TEMP_DIR_CLEANUP_MODE_PROPERTY_NAME, "never")//
					.selectors(selectMethod(DefaultFieldCase.class, "testDefaultField"))//
					.build();
			executeTests(request);

			assertThat(defaultFieldDir).exists();
		}

		/**
		 * Ensure that NEVER cleanup modes are obeyed for fields.
		 * <p/>
		 * Expect the TempDir not to be cleaned up.
		 */
		@Test
		void cleanupModeNeverField() {
			LauncherDiscoveryRequest request = request()//
					.selectors(selectMethod(NeverFieldCase.class, "testNeverField"))//
					.build();
			executeTests(request);

			assertThat(neverFieldDir).exists();
		}

		/**
		 * Ensure that ALWAYS cleanup modes are obeyed for fields.
		 * <p/>
		 * Expect the TempDir to be cleaned up.
		 */
		@Test
		void cleanupModeAlwaysField() {
			LauncherDiscoveryRequest request = request()//
					.selectors(selectMethod(AlwaysFieldCase.class, "testAlwaysField"))//
					.build();
			executeTests(request);

			assertThat(alwaysFieldDir).doesNotExist();
		}

		/**
		 * Ensure that ON_SUCCESS cleanup modes are obeyed for passing field tests.
		 * <p/>
		 * Expect the TempDir to be cleaned up.
		 */
		@Test
		void cleanupModeOnSuccessPassingField() {
			LauncherDiscoveryRequest request = request()//
					.selectors(selectMethod(OnSuccessPassingFieldCase.class, "testOnSuccessPassingField"))//
					.build();
			executeTests(request);

			assertThat(onSuccessPassingFieldDir).doesNotExist();
		}

		/**
		 * Ensure that ON_SUCCESS cleanup modes are obeyed for failing field tests.
		 * <p/>
		 * Expect the TempDir not to be cleaned up.
		 */
		@Test
		void cleanupModeOnSuccessFailingField() {
			LauncherDiscoveryRequest request = request()//
					.selectors(selectMethod(OnSuccessFailingFieldCase.class, "testOnSuccessFailingField"))//
					.build();
			executeTests(request);

			assertThat(onSuccessFailingFieldDir).exists();
		}

		@Test
		void cleanupModeOnSuccessFailingThenPassingField() {
			executeTests(selectClasses(OnSuccessFailingFieldCase.class, OnSuccessPassingFieldCase.class));

			assertThat(onSuccessFailingFieldDir).exists();
			assertThat(onSuccessPassingFieldDir).doesNotExist();
		}

		/**
		 * Ensure that ON_SUCCESS cleanup modes are obeyed for static fields when tests are failing.
		 * <p/>
		 * Expect the TempDir not to be cleaned up.
		 */
		@Test
		void cleanupModeOnSuccessFailingStaticField() {
			LauncherDiscoveryRequest request = request()//
					.selectors(selectClass(OnSuccessFailingStaticFieldCase.class))//
					.build();
			executeTests(request);

			assertThat(onSuccessFailingFieldDir).exists();
		}

		/**
		 * Ensure that ON_SUCCESS cleanup modes are obeyed for static fields when nested tests are failing.
		 * <p/>
		 * Expect the TempDir not to be cleaned up.
		 */
		@Test
		void cleanupModeOnSuccessFailingStaticFieldWithNesting() {
			executeTestsForClass(OnSuccessFailingStaticFieldWithNestingCase.class);

			assertThat(onSuccessFailingFieldDir).exists();
			assertThat(onSuccessPassingParameterDir).doesNotExist();
		}

		@AfterEach
		void deleteTempDirs() throws IOException {
			deleteIfNotNullAndExists(defaultFieldDir);
			deleteIfNotNullAndExists(neverFieldDir);
			deleteIfNotNullAndExists(alwaysFieldDir);
			deleteIfNotNullAndExists(onSuccessFailingFieldDir);
			deleteIfNotNullAndExists(onSuccessPassingFieldDir);
			deleteIfNotNullAndExists(onSuccessPassingParameterDir);
		}

		static void deleteIfNotNullAndExists(Path dir) throws IOException {
			if (dir != null) {
				deleteIfExists(dir);
			}
		}

		// -------------------------------------------------------------------

		@SuppressWarnings("NewClassNamingConvention")
		static class DefaultFieldCase {

			@TempDir
			Path defaultFieldDir;

			@Test
			void testDefaultField() {
				TempDirFieldTests.defaultFieldDir = defaultFieldDir;
			}
		}

		@SuppressWarnings("NewClassNamingConvention")
		static class NeverFieldCase {

			@TempDir(cleanup = NEVER)
			Path neverFieldDir;

			@Test
			void testNeverField() {
				TempDirFieldTests.neverFieldDir = neverFieldDir;
			}
		}

		@SuppressWarnings("NewClassNamingConvention")
		static class AlwaysFieldCase {

			@TempDir(cleanup = ALWAYS)
			Path alwaysFieldDir;

			@Test
			void testAlwaysField() {
				TempDirFieldTests.alwaysFieldDir = alwaysFieldDir;
			}
		}

		@SuppressWarnings("NewClassNamingConvention")
		static class OnSuccessPassingFieldCase {

			@TempDir(cleanup = ON_SUCCESS)
			Path onSuccessPassingFieldDir;

			@Test
			void testOnSuccessPassingField() {
				TempDirFieldTests.onSuccessPassingFieldDir = onSuccessPassingFieldDir;
			}
		}

		@SuppressWarnings("NewClassNamingConvention")
		static class OnSuccessFailingFieldCase {

			@TempDir(cleanup = ON_SUCCESS)
			Path onSuccessFailingFieldDir;

			@Test
			void testOnSuccessFailingField() {
				TempDirFieldTests.onSuccessFailingFieldDir = onSuccessFailingFieldDir;
				fail();
			}
		}

		@SuppressWarnings("NewClassNamingConvention")
		@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
		static class OnSuccessFailingStaticFieldCase {

			@TempDir(cleanup = ON_SUCCESS)
			static Path onSuccessFailingFieldDir;

			@Test
			@Order(1)
			void failing() {
				TempDirFieldTests.onSuccessFailingFieldDir = onSuccessFailingFieldDir;
				fail();
			}

			@Test
			@Order(2)
			void passing() {
			}
		}

		@SuppressWarnings("NewClassNamingConvention")
		static class OnSuccessFailingStaticFieldWithNestingCase {

			@TempDir(cleanup = ON_SUCCESS)
			static Path onSuccessFailingFieldDir;

			@Nested
			@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
			class NestedTestCase {

				@Test
				@Order(1)
				void failingTest() {
					TempDirFieldTests.onSuccessFailingFieldDir = onSuccessFailingFieldDir;
					fail();
				}

				@Test
				@Order(2)
				void passingTest(@TempDir(cleanup = ON_SUCCESS) Path tempDir) {
					TempDirFieldTests.onSuccessPassingParameterDir = tempDir;
				}
			}
		}

	}

	@Nested
	@NullUnmarked
	class TempDirParameterTests {

		private static Path defaultParameterDir;
		private static Path neverParameterDir;
		private static Path alwaysParameterDir;
		private static Path onSuccessFailingParameterDir;
		private static Path onSuccessPassingParameterDir;

		/**
		 * Ensure the cleanup mode defaults to ALWAYS for parameters.
		 * <p/>
		 * Expect the TempDir to be cleaned up.
		 */
		@Test
		void cleanupModeDefaultParameter() {
			LauncherDiscoveryRequest request = request()//
					.selectors(selectMethod(DefaultParameterCase.class, "testDefaultParameter", "java.nio.file.Path"))//
					.build();
			executeTests(request);

			assertThat(defaultParameterDir).doesNotExist();
		}

		/**
		 * Ensure that a custom, global cleanup mode is used for parameters.
		 * <p/>
		 * Expect the TempDir NOT to be cleaned up if set to NEVER.
		 */
		@Test
		void cleanupModeCustomDefaultParameter() {
			LauncherDiscoveryRequest request = request()//
					.configurationParameter(DEFAULT_TEMP_DIR_CLEANUP_MODE_PROPERTY_NAME, "never")//
					.selectors(selectMethod(DefaultParameterCase.class, "testDefaultParameter", "java.nio.file.Path"))//
					.build();
			executeTests(request);

			assertThat(defaultParameterDir).exists();
		}

		/**
		 * Ensure that NEVER cleanup modes are obeyed for parameters.
		 * <p/>
		 * Expect the TempDir not to be cleaned up.
		 */
		@Test
		void cleanupModeNeverParameter() {
			LauncherDiscoveryRequest request = request()//
					.selectors(selectMethod(NeverParameterCase.class, "testNeverParameter", "java.nio.file.Path"))//
					.build();
			executeTests(request);

			assertThat(neverParameterDir).exists();
		}

		/**
		 * Ensure that ALWAYS cleanup modes are obeyed for parameters.
		 * <p/>
		 * Expect the TempDir to be cleaned up.
		 */
		@Test
		void cleanupModeAlwaysParameter() {
			LauncherDiscoveryRequest request = request()//
					.selectors(selectMethod(AlwaysParameterCase.class, "testAlwaysParameter", "java.nio.file.Path"))//
					.build();
			executeTests(request);

			assertThat(alwaysParameterDir).doesNotExist();
		}

		/**
		 * Ensure that ON_SUCCESS cleanup modes are obeyed for passing parameter tests.
		 * <p/>
		 * Expect the TempDir to be cleaned up.
		 */
		@Test
		void cleanupModeOnSuccessPassingParameter() {
			LauncherDiscoveryRequest request = request()//
					.selectors(selectMethod(OnSuccessPassingParameterCase.class, "testOnSuccessPassingParameter",
						"java.nio.file.Path"))//
					.build();
			executeTests(request);

			assertThat(onSuccessPassingParameterDir).doesNotExist();
		}

		/**
		 * Ensure that ON_SUCCESS cleanup modes are obeyed for failing parameter tests.
		 * <p/>
		 * Expect the TempDir not to be cleaned up.
		 */
		@Test
		void cleanupModeOnSuccessFailingParameter() {
			LauncherDiscoveryRequest request = request()//
					.selectors(selectMethod(OnSuccessFailingParameterCase.class, "testOnSuccessFailingParameter",
						"java.nio.file.Path"))//
					.build();
			executeTests(request);

			assertThat(onSuccessFailingParameterDir).exists();
		}

		@Test
		void cleanupModeOnSuccessFailingThenPassingParameter() {
			executeTestsForClass(OnSuccessFailingThenPassingParameterCase.class);

			assertThat(onSuccessFailingParameterDir).exists();
			assertThat(onSuccessPassingParameterDir).doesNotExist();
		}

		@AfterEach
		void deleteTempDirs() throws IOException {
			TempDirFieldTests.deleteIfNotNullAndExists(defaultParameterDir);
			TempDirFieldTests.deleteIfNotNullAndExists(neverParameterDir);
			TempDirFieldTests.deleteIfNotNullAndExists(alwaysParameterDir);
			TempDirFieldTests.deleteIfNotNullAndExists(onSuccessFailingParameterDir);
			TempDirFieldTests.deleteIfNotNullAndExists(onSuccessPassingParameterDir);
		}

		// -------------------------------------------------------------------

		@SuppressWarnings("NewClassNamingConvention")
		static class DefaultParameterCase {

			@Test
			void testDefaultParameter(@TempDir Path defaultParameterDir) {
				TempDirParameterTests.defaultParameterDir = defaultParameterDir;
			}
		}

		@SuppressWarnings("NewClassNamingConvention")
		static class NeverParameterCase {

			@Test
			void testNeverParameter(@TempDir(cleanup = NEVER) Path neverParameterDir) {
				TempDirParameterTests.neverParameterDir = neverParameterDir;
			}
		}

		@SuppressWarnings("NewClassNamingConvention")
		static class AlwaysParameterCase {

			@Test
			void testAlwaysParameter(@TempDir(cleanup = ALWAYS) Path alwaysParameterDir) {
				TempDirParameterTests.alwaysParameterDir = alwaysParameterDir;
			}
		}

		@SuppressWarnings("NewClassNamingConvention")
		static class OnSuccessPassingParameterCase {

			@Test
			void testOnSuccessPassingParameter(@TempDir(cleanup = ON_SUCCESS) Path onSuccessPassingParameterDir) {
				TempDirParameterTests.onSuccessPassingParameterDir = onSuccessPassingParameterDir;
			}
		}

		@SuppressWarnings("NewClassNamingConvention")
		static class OnSuccessFailingParameterCase {

			@Test
			void testOnSuccessFailingParameter(@TempDir(cleanup = ON_SUCCESS) Path onSuccessFailingParameterDir) {
				TempDirParameterTests.onSuccessFailingParameterDir = onSuccessFailingParameterDir;
				fail();
			}
		}

		@SuppressWarnings("NewClassNamingConvention")
		@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
		static class OnSuccessFailingThenPassingParameterCase {

			@Test
			@Order(1)
			void testOnSuccessFailingParameter(@TempDir(cleanup = ON_SUCCESS) Path onSuccessFailingParameterDir) {
				TempDirParameterTests.onSuccessFailingParameterDir = onSuccessFailingParameterDir;
				fail();
			}

			@Test
			@Order(2)
			void testOnSuccessPassingParameter(@TempDir(cleanup = ON_SUCCESS) Path onSuccessPassingParameterDir) {
				TempDirParameterTests.onSuccessPassingParameterDir = onSuccessPassingParameterDir;
			}
		}

	}

	@ParameterizedTest
	@CsvSource({ ", WARNING", "'  DeBuG  ', FINE", "'  oFf  '," })
	@DisabledOnOs(WINDOWS)
	void configuresExternalSymbolicLinkLogLevel(@Nullable String configuredLevel, @Nullable String expectedLevel,
			@TempDir Path outsideDir, @TrackLogRecords LogRecordListener listener) throws IOException {
		var testFile = Files.writeString(outsideDir.resolve("test.txt"), "test");
		var builder = request().selectors(selectClass(SymbolicLinkTestCase.class));
		if (configuredLevel != null) {
			builder.configurationParameter(TempDir.EXTERNAL_TARGET_LOG_LEVEL_PROPERTY_NAME, configuredLevel);
		}

		SymbolicLinkTestCase.target = outsideDir;
		try {
			executeTests(builder.build()).testEvents().assertStatistics(stats -> stats.started(1).succeeded(1));

			assertThat(SymbolicLinkTestCase.tempDir).doesNotExist();
			assertThat(outsideDir).isDirectory();
			assertThat(testFile).hasContent("test");
			assertExternalTargetLogLevel(listener, expectedLevel == null ? null : Level.parse(expectedLevel));
		}
		finally {
			SymbolicLinkTestCase.target = null;
			SymbolicLinkTestCase.tempDir = null;
		}
	}

	@NullUnmarked
	static class SymbolicLinkTestCase {
		static Path target;
		static Path tempDir;

		@Test
		void createSymbolicLinkToTarget(@TempDir Path dir) throws IOException {
			tempDir = dir;
			Files.createSymbolicLink(dir.resolve("link"), target);
		}
	}

	private static void assertExternalTargetLogLevel(LogRecordListener listener, @Nullable Level expectedLevel) {
		var records = listener.stream().filter(record -> record.getMessage().matches(
			"Deleting (symbolic link|link) from location inside of temp dir \\(.+\\) to location outside of temp dir \\(.+\\) but not the target file/directory"));
		if (expectedLevel == null) {
			assertThat(records).isEmpty();
		}
		else {
			assertThat(records).extracting(LogRecord::getLevel).containsExactly(expectedLevel);
		}
	}

	@Nested
	@NullUnmarked
	@DisabledOnOs(WINDOWS)
	class LocalExternalTargetLogLevelTests {

		private static Path target;
		private static Path[] tempDirs;

		@ParameterizedTest
		@CsvSource({ "true, OFF", "true, DEBUG", "true, invalid", "false, OFF", "false, DEBUG", "false, invalid" })
		void annotationOverridesGlobalConfiguration(boolean fields, String globalLevel, @TempDir Path outsideDir,
				@TrackLogRecords LogRecordListener listener) throws IOException {
			assertCleanupAndLogs(fields ? LocalFieldCase.class : LocalParameterCase.class, globalLevel, outsideDir,
				listener, Level.WARNING, Level.WARNING, Level.FINE, null);
		}

		@ParameterizedTest
		@CsvSource({ "true, OFF,", "true, DEBUG, FINE", "true, WARN, WARNING", "false, OFF,", "false, DEBUG, FINE",
				"false, WARN, WARNING" })
		void defaultAnnotationInheritsGlobalConfiguration(boolean fields, String globalLevel, String expectedLevel,
				@TempDir Path outsideDir, @TrackLogRecords LogRecordListener listener) throws IOException {
			assertCleanupAndLogs(fields ? DefaultFieldCase.class : DefaultParameterCase.class, globalLevel, outsideDir,
				listener, expectedLevel == null ? null : Level.parse(expectedLevel));
		}

		private void assertCleanupAndLogs(Class<?> testCase, String globalLevel, Path outsideDir,
				LogRecordListener listener, Level... expectedLevels) throws IOException {
			final var testFile = Files.writeString(outsideDir.resolve("test.txt"), "test");
			final var discoveryRequest = request().selectors(selectClass(testCase)).configurationParameter(
				TempDir.EXTERNAL_TARGET_LOG_LEVEL_PROPERTY_NAME, globalLevel).build();
			target = outsideDir;
			try {
				executeTests(discoveryRequest).testEvents().assertStatistics(stats -> stats.started(1).succeeded(1));

				assertThat(tempDirs).hasSize(expectedLevels.length);
				for (int i = 0; i < tempDirs.length; i++) {
					final var dir = tempDirs[i];
					assertThat(dir).doesNotExist();
					final var records = listener.stream().filter(
						record -> record.getMessage().startsWith("Deleting ")).filter(
							record -> record.getMessage().contains("(" + dir.resolve("link") + ")"));
					if (expectedLevels[i] == null) {
						assertThat(records).isEmpty();
					}
					else {
						assertThat(records).extracting(LogRecord::getLevel).containsExactly(expectedLevels[i]);
					}
				}
				assertThat(outsideDir).isDirectory();
				assertThat(testFile).hasContent("test");
			}
			finally {
				target = null;
				tempDirs = null;
			}
		}

		private static void createSymbolicLinks(Path... dirs) throws IOException {
			tempDirs = dirs;
			for (final var dir : dirs) {
				Files.createSymbolicLink(dir.resolve("link"), target);
			}
		}

		@Retention(RetentionPolicy.RUNTIME)
		@Target({ ElementType.FIELD, ElementType.PARAMETER })
		@TempDir(externalTargetLogLevel = LogLevel.WARN)
		@interface WarnTempDir {
		}

		@SuppressWarnings("NewClassNamingConvention")
		static class LocalFieldCase {

			@TempDir(externalTargetLogLevel = LogLevel.WARN)
			Path warnDir;

			@WarnTempDir
			Path composedDir;

			@TempDir(externalTargetLogLevel = LogLevel.DEBUG)
			Path debugDir;

			@TempDir(externalTargetLogLevel = LogLevel.OFF)
			Path offDir;

			@Test
			void createLinks() throws IOException {
				createSymbolicLinks(warnDir, composedDir, debugDir, offDir);
			}
		}

		@SuppressWarnings("NewClassNamingConvention")
		static class LocalParameterCase {

			@Test
			void createLinks(@TempDir(externalTargetLogLevel = LogLevel.WARN) Path warnDir,
					@WarnTempDir Path composedDir, @TempDir(externalTargetLogLevel = LogLevel.DEBUG) Path debugDir,
					@TempDir(externalTargetLogLevel = LogLevel.OFF) Path offDir) throws IOException {
				createSymbolicLinks(warnDir, composedDir, debugDir, offDir);
			}
		}

		@SuppressWarnings("NewClassNamingConvention")
		static class DefaultFieldCase {

			@TempDir(externalTargetLogLevel = LogLevel.DEFAULT)
			Path dir;

			@Test
			void createLink() throws IOException {
				createSymbolicLinks(dir);
			}
		}

		@SuppressWarnings("NewClassNamingConvention")
		static class DefaultParameterCase {

			@Test
			void createLink(@TempDir(externalTargetLogLevel = LogLevel.DEFAULT) Path dir) throws IOException {
				createSymbolicLinks(dir);
			}
		}
	}

	@Nested
	@EnabledOnOs(WINDOWS)
	class WindowsTests {

		@Test
		void deletesBrokenJunctions(@TempDir Path dir) throws Exception {
			var test = Files.createDirectory(dir.resolve("test"));
			createWindowsJunction(dir.resolve("link"), test);
			// The error might also occur without the source folder being deleted
			// but it depends on the order that the TempDir cleanup does its work,
			// so the following line forces the error to occur always
			Files.delete(test);
		}

		@ParameterizedTest
		@CsvSource({ ", WARNING", "'  DeBuG  ', FINE", "'  oFf  '," })
		void doesNotFollowJunctions(@Nullable String configuredLevel, @Nullable String expectedLevel,
				@TempDir Path tempDir, @TrackLogRecords LogRecordListener listener) throws IOException {
			var outsideDir = Files.createDirectory(tempDir.resolve("outside"));
			var testFile = Files.writeString(outsideDir.resolve("test.txt"), "test");
			var builder = request().selectors(selectClass(JunctionTestCase.class));
			if (configuredLevel != null) {
				builder.configurationParameter(TempDir.EXTERNAL_TARGET_LOG_LEVEL_PROPERTY_NAME, configuredLevel);
			}

			JunctionTestCase.target = outsideDir;
			try {
				executeTests(builder.build()).testEvents() //
						.assertStatistics(stats -> stats.started(1).succeeded(1));

				assertThat(JunctionTestCase.tempDir).doesNotExist();
			}
			finally {
				JunctionTestCase.target = null;
				JunctionTestCase.tempDir = null;
			}

			assertThat(outsideDir).exists();
			assertThat(testFile).hasContent("test");
			assertExternalTargetLogLevel(listener, expectedLevel == null ? null : Level.parse(expectedLevel));
		}

		@NullUnmarked
		static class JunctionTestCase {
			public static Path target;
			static Path tempDir;

			@Test
			void createJunctionToTarget(@TempDir Path dir) throws Exception {
				tempDir = dir;
				var link = createWindowsJunction(dir.resolve("link"), target);
				try (var files = Files.list(link)) {
					files.forEach(it -> System.out.println("- " + it));
				}
			}
		}

		private static Path createWindowsJunction(Path link, Path target) throws Exception {
			// This creates a Windows "junction" which you can't do with Java code
			String[] command = { "cmd.exe", "/c", "mklink", "/j", link.toString(), target.toString() };
			Runtime.getRuntime().exec(command).waitFor();
			return link;
		}
	}

}
