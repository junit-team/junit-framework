/*
 * Copyright 2015-2025 the original author or authors.
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v2.0 which
 * accompanies this distribution and is available at
 *
 * https://www.eclipse.org/legal/epl-v20.html
 */

package org.junit.jupiter.engine;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.engine.Constants.DEFAULT_TEST_INSTANCE_LIFECYCLE_PROPERTY_NAME;
import static org.junit.platform.engine.discovery.DiscoverySelectors.selectClass;
import static org.junit.platform.launcher.core.LauncherDiscoveryRequestBuilder.request;
import static org.junit.platform.launcher.core.OutputDirectoryCreators.hierarchicalOutputDirectoryCreator;
import static org.junit.platform.testkit.engine.EventConditions.fileEntry;
import static org.junit.platform.testkit.engine.EventConditions.reportEntry;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Predicate;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.MediaType;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance.Lifecycle;
import org.junit.jupiter.api.TestReporter;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.platform.commons.PreconditionViolationException;
import org.junit.platform.engine.reporting.FileEntry;

/**
 * @since 5.0
 */
class ReportingTests extends AbstractJupiterTestEngineTests {

	@ParameterizedTest
	@CsvSource(textBlock = """
			PER_CLASS,  1, 7, 5
			PER_METHOD, 0, 9, 7
			""")
	void reportAndFileEntriesArePublished(Lifecycle lifecycle, int containerEntries, int testReportEntries,
			int testFileEntries, @TempDir Path tempDir) {
		var request = request() //
				.selectors(selectClass(MyReportingTestCase.class)) //
				.configurationParameter(DEFAULT_TEST_INSTANCE_LIFECYCLE_PROPERTY_NAME, lifecycle.name()) //
				.outputDirectoryCreator(hierarchicalOutputDirectoryCreator(tempDir));

		var results = executeTests(request);

		results //
				.containerEvents() //
				.assertStatistics(stats -> stats //
						.started(2) //
						.succeeded(2) //
						.reportingEntryPublished(containerEntries) //
						.fileEntryPublished(containerEntries));

		results //
				.testEvents() //
				.assertStatistics(stats -> stats //
						.started(2) //
						.succeeded(2) //
						.reportingEntryPublished(testReportEntries) //
						.fileEntryPublished(testFileEntries)) //
				.assertThatEvents() //
				.haveExactly(2, reportEntry(Map.of("value", "@BeforeEach"))) //
				.haveExactly(2, fileEntry(nameAndContent("beforeEach", MediaType.TEXT_PLAIN_UTF_8))) //
				.haveExactly(1, reportEntry(Map.of())) //
				.haveExactly(1, reportEntry(Map.of("user name", "dk38"))) //
				.haveExactly(1, reportEntry(Map.of("value", "message"))) //
				.haveExactly(1, fileEntry(nameAndContent("succeedingTest", MediaType.APPLICATION_OCTET_STREAM))) //
				.haveExactly(2, reportEntry(Map.of("value", "@AfterEach"))) //
				.haveExactly(2, fileEntry(nameAndContent("afterEach", MediaType.TEXT_PLAIN_UTF_8)));
	}

	@SuppressWarnings("JUnitMalformedDeclaration")
	static class MyReportingTestCase {

		MyReportingTestCase(TestReporter reporter) {
			// Reported on class-level for PER_CLASS lifecycle and on method-level for PER_METHOD lifecycle
			reporter.publishEntry("Constructor");
			reporter.publishFile("constructor", MediaType.TEXT_PLAIN_UTF_8,
				file -> Files.writeString(file, "constructor"));
		}

		@BeforeEach
		void beforeEach(TestReporter reporter) {
			reporter.publishEntry("@BeforeEach");
			reporter.publishFile("beforeEach", MediaType.TEXT_PLAIN_UTF_8,
				file -> Files.writeString(file, "beforeEach"));
		}

		@AfterEach
		void afterEach(TestReporter reporter) {
			reporter.publishEntry("@AfterEach");
			reporter.publishFile("afterEach", MediaType.TEXT_PLAIN_UTF_8, file -> Files.writeString(file, "afterEach"));
		}

		@Test
		void succeedingTest(TestReporter reporter) {
			reporter.publishEntry(Map.of());
			reporter.publishEntry("user name", "dk38");
			reporter.publishEntry("message");
			reporter.publishFile("succeedingTest", MediaType.APPLICATION_OCTET_STREAM,
				file -> Files.writeString(file, "succeedingTest"));
		}

		@SuppressWarnings("DataFlowIssue")
		@Test
		void invalidReportData(TestReporter reporter) {

			// Maps
			Map<String, String> map = new HashMap<>();

			map.put("key", null);
			assertThrows(PreconditionViolationException.class, () -> reporter.publishEntry(map));

			map.clear();
			map.put(null, "value");
			assertThrows(PreconditionViolationException.class, () -> reporter.publishEntry(map));

			assertThrows(PreconditionViolationException.class, () -> reporter.publishEntry((Map<String, String>) null));

			// Key-Value pair
			assertThrows(PreconditionViolationException.class, () -> reporter.publishEntry(null, "bar"));
			assertThrows(PreconditionViolationException.class, () -> reporter.publishEntry("foo", null));

			// Value
			assertThrows(PreconditionViolationException.class, () -> reporter.publishEntry((String) null));
		}

	}

	private static Predicate<FileEntry> nameAndContent(String expectedName, MediaType mediaType) {
		Predicate<Path> filePredicate = file -> {
			try {
				return Path.of(expectedName).equals(file.getFileName()) && expectedName.equals(Files.readString(file));
			}
			catch (IOException e) {
				throw new UncheckedIOException(e);
			}
		};
		return fileEntry -> filePredicate.test(fileEntry.getPath()) //
				&& mediaType.toString().equals(fileEntry.getMediaType().orElse(null));
	}

}
