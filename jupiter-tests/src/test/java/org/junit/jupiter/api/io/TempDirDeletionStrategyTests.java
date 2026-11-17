/*
 * Copyright 2026 the original author or authors.
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v2.0 which
 * accompanies this distribution and is available at
 *
 * https://www.eclipse.org/legal/epl-v20.html
 */

package org.junit.jupiter.api.io;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.junit.jupiter.api.condition.OS.WINDOWS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.LogRecord;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.DisabledOnOs;
import org.junit.jupiter.api.extension.AnnotatedElementContext;
import org.junit.jupiter.api.extension.ExtensionConfigurationException;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.fixtures.TrackLogRecords;
import org.junit.jupiter.api.io.TempDir.LogLevel;
import org.junit.jupiter.api.io.TempDirDeletionStrategy.DeletionException;
import org.junit.jupiter.api.io.TempDirDeletionStrategy.IgnoreFailures;
import org.junit.jupiter.api.io.TempDirDeletionStrategy.Standard;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.junit.platform.commons.logging.LogRecordListener;

/**
 * @since 6.1
 */
class TempDirDeletionStrategyTests {

	@Nested
	class StandardTests {

		@Test
		@DisabledOnOs(WINDOWS)
		void configurationDoesNotLeakBetweenInvocations(@TempDir Path tempDir, @TrackLogRecords LogRecordListener log)
				throws Exception {
			deleteLink(Standard.INSTANCE, Files.createDirectory(tempDir.resolve("debug")), mock(), context("DEBUG"));
			deleteLink(Standard.INSTANCE, Files.createDirectory(tempDir.resolve("default")), mock(), mock());

			assertThat(
				log.stream(Standard.class).filter(record -> record.getMessage().startsWith("Deleting "))).extracting(
					LogRecord::getLevel).containsExactly(Level.FINE, Level.WARNING);
		}

		@Test
		@DisabledOnOs(WINDOWS)
		void ignoreFailuresHonorsConfiguredLevel(@TempDir Path tempDir, @TrackLogRecords LogRecordListener log)
				throws Exception {
			deleteLink(new IgnoreFailures(), tempDir, mock(), context("DEBUG"));

			assertThat(log.stream(Standard.class, Level.FINE).map(LogRecord::getMessage)).anyMatch(
				message -> message.startsWith("Deleting symbolic link "));
			assertThat(log.stream(Level.WARNING)).isEmpty();
		}

		@ParameterizedTest
		@ValueSource(strings = { "", "  ", "WARNING", "FINE", "invalid", "DEFAULT" })
		void rejectsInvalidGlobalLogLevels(String value, @TempDir Path tempDir) {
			assertThatExceptionOfType(ExtensionConfigurationException.class).isThrownBy(
				() -> Standard.INSTANCE.delete(tempDir, mock(), context(value))).withMessageContaining(
					value).withMessageContaining(TempDir.EXTERNAL_TARGET_LOG_LEVEL_PROPERTY_NAME).withMessageContaining(
						"Supported values:");
			assertThat(tempDir).isDirectory();
		}

		private ExtensionContext context(String level) {
			final ExtensionContext context = mock();
			when(context.getConfigurationParameter(TempDir.EXTERNAL_TARGET_LOG_LEVEL_PROPERTY_NAME)).thenReturn(
				Optional.of(level));
			return context;
		}

		@ParameterizedTest
		@EnumSource(value = LogLevel.class, names = "DEFAULT", mode = EnumSource.Mode.EXCLUDE)
		@DisabledOnOs(WINDOWS)
		void explicitAnnotationLogLevelOverridesGlobalConfiguration(LogLevel level, @TempDir Path tempDir,
				@TrackLogRecords LogRecordListener log) throws Exception {
			final TempDir annotation = mock();
			when(annotation.externalTargetLogLevel()).thenReturn(level);
			final AnnotatedElementContext elementContext = mock();
			when(elementContext.findAnnotation(TempDir.class)).thenReturn(Optional.of(annotation));

			for (final var globalLevel : new String[] { "OFF", "invalid" }) {
				deleteLink(Standard.INSTANCE, Files.createDirectory(tempDir.resolve(globalLevel)), elementContext,
					context(globalLevel));
			}

			final var records = log.stream(Standard.class).filter(
				record -> record.getMessage().startsWith("Deleting "));
			if (level == LogLevel.OFF) {
				assertThat(records).isEmpty();
			}
			else {
				final var expectedLevel = switch (level) {
					case ERROR -> Level.SEVERE;
					case WARN -> Level.WARNING;
					case INFO -> Level.INFO;
					case CONFIG -> Level.CONFIG;
					case DEBUG -> Level.FINE;
					case TRACE -> Level.FINER;
					default -> throw new AssertionError("Unexpected log level: " + level);
				};
				assertThat(records).extracting(LogRecord::getLevel).containsExactly(expectedLevel, expectedLevel);
			}
		}

		private void deleteLink(TempDirDeletionStrategy strategy, Path tempDir, AnnotatedElementContext elementContext,
				ExtensionContext context) throws Exception {
			final var target = Files.createDirectory(tempDir.resolve("target"));
			final var targetFile = Files.writeString(target.resolve("file"), "preserved");
			final var root = Files.createDirectory(tempDir.resolve("root"));
			Files.createSymbolicLink(root.resolve("link"), target);

			assertThat(strategy.delete(root, elementContext, context).isSuccessful()).isTrue();
			assertThat(root).doesNotExist();
			assertThat(targetFile).hasContent("preserved");
		}
	}

	@Nested
	class IgnoreFailuresTests {

		@Test
		void logsAndIgnoresFailures(@TempDir Path tempDir, @TrackLogRecords LogRecordListener log) throws Exception {
			var undeletableDir = Files.createDirectory(tempDir.resolve("undeletable"));
			var strategy = new IgnoreFailures(new FailingTempDirDeletionStrategy());

			AnnotatedElementContext annotatedElementContext = mock();
			var testMethod = IgnoreFailuresTests.class.getDeclaredMethod("logsAndIgnoresFailures", Path.class,
				LogRecordListener.class);
			when(annotatedElementContext.getAnnotatedElement()).thenReturn(testMethod.getParameters()[0]);

			var result = strategy.delete(undeletableDir, annotatedElementContext, mock());

			assertThat(result.isSuccessful());

			var loggedWarnings = log.stream(IgnoreFailures.class, Level.WARNING).toList();

			assertThat(loggedWarnings) //
					.extracting(LogRecord::getMessage) //
					.containsExactly(
						"Failed to delete all temporary files for parameter 'tempDir' in method logsAndIgnoresFailures(Path, LogRecordListener)");

			var exception = loggedWarnings.getFirst().getThrown();
			assertThat(exception).isInstanceOf(DeletionException.class);
			assertThat(exception).hasMessage(
				"Failed to delete temp directory %s. The following paths could not be deleted (see suppressed exceptions for details): <root>".formatted(
					undeletableDir.toAbsolutePath()));

			assertThat(exception.getSuppressed()).hasSize(1);
			assertThat(exception.getSuppressed()[0]) //
					.isInstanceOf(IOException.class) //
					.hasMessage("Simulated failure");
		} //

	}

}
