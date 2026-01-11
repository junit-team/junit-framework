/*
 * Copyright 2015-2026 the original author or authors.
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v2.0 which
 * accompanies this distribution and is available at
 *
 * https://www.eclipse.org/legal/epl-v20.html
 */

package org.junit.platform.reporting.legacy.xml;

import static org.assertj.core.api.Assertions.assertThat;
import static org.joox.JOOX.$;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;
import static org.junit.jupiter.api.Assumptions.assumeFalse;
import static org.junit.platform.engine.TestExecutionResult.successful;
import static org.junit.platform.engine.discovery.DiscoverySelectors.selectUniqueId;
import static org.junit.platform.launcher.core.LauncherDiscoveryRequestBuilder.request;
import static org.junit.platform.launcher.core.LauncherFactoryForTestingPurposesOnly.createLauncher;
import static org.junit.platform.launcher.core.OutputDirectoryCreators.dummyOutputDirectoryCreator;
import static org.junit.platform.reporting.legacy.xml.XmlReportAssertions.assertValidAccordingToJenkinsSchema;
import static org.mockito.Mockito.mock;

import java.io.File;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.net.InetAddress;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.Year;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

import org.joox.Match;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.platform.engine.TestEngine;
import org.junit.platform.engine.UniqueId;
import org.junit.platform.engine.reporting.ReportEntry;
import org.junit.platform.engine.support.descriptor.EngineDescriptor;
import org.junit.platform.engine.support.hierarchical.DemoEngineExecutionContext;
import org.junit.platform.engine.support.hierarchical.DemoHierarchicalContainerDescriptor;
import org.junit.platform.engine.support.hierarchical.DemoHierarchicalTestDescriptor;
import org.junit.platform.engine.support.hierarchical.DemoHierarchicalTestEngine;
import org.junit.platform.fakes.TestDescriptorStub;
import org.junit.platform.launcher.LauncherConstants;
import org.junit.platform.launcher.TestIdentifier;
import org.junit.platform.launcher.TestPlan;
import org.opentest4j.AssertionFailedError;

/**
 * Tests for {@link LegacyXmlReportGeneratingListener}.
 *
 * @since 1.0
 */
class LegacyXmlReportGeneratingListenerTests {

	@TempDir
	Path tempDirectory;

	@Test
	void writesFileForSingleSucceedingTest() throws Exception {
		var engine = new DemoHierarchicalTestEngine("dummy");
		engine.addTest("succeedingTest", "display<-->Name 😎", () -> {
		});

		executeTests(engine);

		var testsuite = readValidXmlFile(tempDirectory.resolve("TEST-dummy.xml"));

		assertThat(testsuite.attr("name")).isEqualTo("dummy");
		assertThat(testsuite.attr("tests", int.class)).isEqualTo(1);
		assertThat(testsuite.attr("skipped", int.class)).isEqualTo(0);
		assertThat(testsuite.attr("failures", int.class)).isEqualTo(0);
		assertThat(testsuite.attr("errors", int.class)).isEqualTo(0);
		assertThat(testsuite.child("system-out").text()) //
				.containsSubsequence("unique-id: [engine:dummy]", "display-name: dummy");

		var testcase = testsuite.child("testcase");
		assertThat(testcase.attr("name")).isEqualTo("display<-->Name 😎");
		assertThat(testcase.attr("classname")).isEqualTo("dummy");
		assertThat(testcase.child("system-out").text()) //
				.containsSubsequence("unique-id: [engine:dummy]/[test:succeedingTest]",
					"display-name: display<-->Name 😎");

		assertThat(testsuite.find("skipped")).isEmpty();
		assertThat(testsuite.find("failure")).isEmpty();
		assertThat(testsuite.find("error")).isEmpty();
	}

	@Test
	void writesFileForSingleFailingTest() throws Exception {
		var engine = new DemoHierarchicalTestEngine("dummy");
		engine.addTest("failingTest", () -> fail("expected to <b>fail</b>"));

		executeTests(engine);

		var testsuite = readValidXmlFile(tempDirectory.resolve("TEST-dummy.xml"));

		assertThat(testsuite.attr("tests", int.class)).isEqualTo(1);
		assertThat(testsuite.attr("skipped", int.class)).isEqualTo(0);
		assertThat(testsuite.attr("failures", int.class)).isEqualTo(1);
		assertThat(testsuite.attr("errors", int.class)).isEqualTo(0);

		var testcase = testsuite.child("testcase");
		assertThat(testcase.attr("name")).isEqualTo("failingTest");

		var failure = testcase.child("failure");
		assertThat(failure.attr("message")).isEqualTo("expected to <b>fail</b>");
		assertThat(failure.attr("type")).isEqualTo(AssertionFailedError.class.getName());
		assertThat(failure.text()).containsSubsequence("AssertionFailedError: expected to <b>fail</b>", "\tat");

		assertThat(testsuite.find("skipped")).isEmpty();
		assertThat(testsuite.find("error")).isEmpty();
	}

	@Test
	void writesFileForSingleErroneousTest() throws Exception {
		var engine = new DemoHierarchicalTestEngine("dummy");
		engine.addTest("failingTest", () -> {
			throw new RuntimeException("error occurred");
		});

		executeTests(engine);

		var testsuite = readValidXmlFile(tempDirectory.resolve("TEST-dummy.xml"));

		assertThat(testsuite.attr("tests", int.class)).isEqualTo(1);
		assertThat(testsuite.attr("skipped", int.class)).isEqualTo(0);
		assertThat(testsuite.attr("failures", int.class)).isEqualTo(0);
		assertThat(testsuite.attr("errors", int.class)).isEqualTo(1);

		var testcase = testsuite.child("testcase");
		assertThat(testcase.attr("name")).isEqualTo("failingTest");

		var error = testcase.child("error");
		assertThat(error.attr("message")).isEqualTo("error occurred");
		assertThat(error.attr("type")).isEqualTo(RuntimeException.class.getName());
		assertThat(error.text()).containsSubsequence("RuntimeException: error occurred", "\tat");

		assertThat(testsuite.find("skipped")).isEmpty();
		assertThat(testsuite.find("failure")).isEmpty();
	}

	@Test
	void writesFileForSingleSkippedTest() throws Exception {
		var engine = new DemoHierarchicalTestEngine("dummy");
		var testDescriptor = engine.addTest("skippedTest", () -> fail("never called"));
		testDescriptor.markSkipped("should be skipped");

		executeTests(engine);

		var testsuite = readValidXmlFile(tempDirectory.resolve("TEST-dummy.xml"));

		assertThat(testsuite.attr("tests", int.class)).isEqualTo(1);
		assertThat(testsuite.attr("skipped", int.class)).isEqualTo(1);
		assertThat(testsuite.attr("failures", int.class)).isEqualTo(0);
		assertThat(testsuite.attr("errors", int.class)).isEqualTo(0);

		var testcase = testsuite.child("testcase");
		assertThat(testcase.attr("name")).isEqualTo("skippedTest");
		assertThat(testcase.child("skipped").text()).isEqualTo("should be skipped");

		assertThat(testsuite.find("failure")).isEmpty();
		assertThat(testsuite.find("error")).isEmpty();
	}

	@SuppressWarnings("ConstantConditions")
	@Test
	void writesFileForSingleAbortedTest() throws Exception {
		var engine = new DemoHierarchicalTestEngine("dummy");
		engine.addTest("abortedTest", () -> assumeFalse(true, "deliberately aborted"));

		executeTests(engine);

		var testsuite = readValidXmlFile(tempDirectory.resolve("TEST-dummy.xml"));

		assertThat(testsuite.attr("tests", int.class)).isEqualTo(1);
		assertThat(testsuite.attr("skipped", int.class)).isEqualTo(1);
		assertThat(testsuite.attr("failures", int.class)).isEqualTo(0);
		assertThat(testsuite.attr("errors", int.class)).isEqualTo(0);

		var testcase = testsuite.child("testcase");
		assertThat(testcase.attr("name")).isEqualTo("abortedTest");
		assertThat(testcase.child("skipped").text()) //
				.containsSubsequence("TestAbortedException: ", "deliberately aborted", "at ");

		assertThat(testsuite.find("failure")).isEmpty();
		assertThat(testsuite.find("error")).isEmpty();
	}

	@Test
	void measuresTimesInSeconds() throws Exception {
		var engine = new DemoHierarchicalTestEngine("dummy");
		engine.addTest("firstTest", () -> {
		});
		engine.addTest("secondTest", () -> {
		});

		executeTests(engine, new IncrementingClock(0, Duration.ofMillis(333)));

		var testsuite = readValidXmlFile(tempDirectory.resolve("TEST-dummy.xml"));

		//               start        end
		// ----------- ---------- -----------
		// engine          0 (1)    1,665 (6)
		// firstTest     333 (2)      666 (3)
		// secondTest    999 (4)    1,332 (5)

		assertThat(testsuite.attr("time", double.class)) //
				.isEqualTo(1.665);
		assertThat(testsuite.children("testcase").matchAttr("name", "firstTest").attr("time", double.class)) //
				.isEqualTo(0.333);
		assertThat(testsuite.children("testcase").matchAttr("name", "secondTest").attr("time", double.class)) //
				.isEqualTo(0.333);
	}

	@Test
	void testWithImmeasurableTimeIsOutputCorrectly() throws Exception {
		var engine = new DemoHierarchicalTestEngine("dummy");
		engine.addTest("test", () -> {
		});

		executeTests(engine, Clock.fixed(Instant.EPOCH, ZoneId.systemDefault()));

		var testsuite = readValidXmlFile(tempDirectory.resolve("TEST-dummy.xml"));

		assertThat(testsuite.child("testcase").attr("time")).isEqualTo("0");
	}

	@Test
	void writesFileForSkippedContainer() throws Exception {
		var engine = new DemoHierarchicalTestEngine("dummy");
		engine.addTest("test", () -> fail("never called"));
		engine.getEngineDescriptor().markSkipped("should be skipped");

		executeTests(engine);

		var testsuite = readValidXmlFile(tempDirectory.resolve("TEST-dummy.xml"));

		assertThat(testsuite.attr("tests", int.class)).isEqualTo(1);
		assertThat(testsuite.attr("skipped", int.class)).isEqualTo(1);

		var testcase = testsuite.child("testcase");
		assertThat(testcase.attr("name")).isEqualTo("test");
		assertThat(testcase.child("skipped").text()).isEqualTo("parent was skipped: should be skipped");
	}

	@Test
	void writesFileForFailingContainer() throws Exception {
		var engine = new DemoHierarchicalTestEngine("dummy");
		engine.addTest("test", () -> fail("never called"));
		engine.getEngineDescriptor().setBeforeAllBehavior(() -> fail("failure before all tests"));

		executeTests(engine);

		var testsuite = readValidXmlFile(tempDirectory.resolve("TEST-dummy.xml"));

		assertThat(testsuite.attr("tests", int.class)).isEqualTo(1);
		assertThat(testsuite.attr("failures", int.class)).isEqualTo(1);

		var testcase = testsuite.child("testcase");
		assertThat(testcase.attr("name")).isEqualTo("test");

		var failure = testcase.child("failure");
		assertThat(failure.attr("message")).isEqualTo("failure before all tests");
		assertThat(failure.attr("type")).isEqualTo(AssertionFailedError.class.getName());
		assertThat(failure.text()).containsSubsequence("AssertionFailedError: failure before all tests", "\tat");
	}

	@Test
	void writesFileForFailingContainerWithoutTest() throws Exception {
		var engine = new DemoHierarchicalTestEngine("dummy");
		engine.addContainer("failingContainer", () -> {
			throw new RuntimeException("boom");
		});

		executeTests(engine);

		var testsuite = readValidXmlFile(tempDirectory.resolve("TEST-dummy.xml"));

		assertThat(testsuite.attr("tests", int.class)).isEqualTo(1);
		assertThat(testsuite.attr("errors", int.class)).isEqualTo(1);

		var testcase = testsuite.child("testcase");
		assertThat(testcase.attr("name")).isEqualTo("failingContainer");
		assertThat(testcase.attr("classname")).isEqualTo("dummy");

		var error = testcase.child("error");
		assertThat(error.attr("message")).isEqualTo("boom");
		assertThat(error.attr("type")).isEqualTo(RuntimeException.class.getName());
		assertThat(error.text()).containsSubsequence("RuntimeException: boom", "\tat");
	}

	@Test
	void writesFileForContainerFailingAfterTest() throws Exception {
		var engine = new DemoHierarchicalTestEngine("dummy");

		var container = engine.addChild("failingContainer",
			uniqueId -> new DemoHierarchicalContainerDescriptor(uniqueId, "failingContainer", null, null) {
				@Override
				public void after(DemoEngineExecutionContext context) {
					throw new RuntimeException("boom");
				}
			}, "child");
		container.addChild(new DemoHierarchicalTestDescriptor(container.getUniqueId().append("test", "someTest"),
			"someTest", (c, t) -> {
			}));

		executeTests(engine);

		var testsuite = readValidXmlFile(tempDirectory.resolve("TEST-dummy.xml"));

		assertThat(testsuite.attr("tests", int.class)).isEqualTo(1);
		assertThat(testsuite.attr("errors", int.class)).isEqualTo(1);

		var testcase = testsuite.child("testcase");
		assertThat(testcase.attr("name")).isEqualTo("someTest");
		assertThat(testcase.attr("classname")).isEqualTo("failingContainer");

		var error = testcase.child("error");
		assertThat(error.attr("message")).isEqualTo("boom");
		assertThat(error.attr("type")).isEqualTo(RuntimeException.class.getName());
		assertThat(error.text()).containsSubsequence("RuntimeException: boom", "\tat");
	}

	@Test
	void writesSystemProperties() throws Exception {
		var engine = new DemoHierarchicalTestEngine("dummy");
		engine.addTest("test", () -> {
		});

		executeTests(engine);

		var testsuite = readValidXmlFile(tempDirectory.resolve("TEST-dummy.xml"));
		var properties = testsuite.child("properties").children("property");
		assertThat(properties.matchAttr("name", "file\\.separator").attr("value")).isEqualTo(File.separator);
		assertThat(properties.matchAttr("name", "path\\.separator").attr("value")).isEqualTo(File.pathSeparator);
	}

	@Test
	void writesHostNameAndTimestamp() throws Exception {
		var engine = new DemoHierarchicalTestEngine("dummy");
		engine.addTest("test", () -> {
		});

		var now = LocalDateTime.parse("2016-01-28T14:02:59.123");
		var zone = ZoneId.systemDefault();

		executeTests(engine, Clock.fixed(ZonedDateTime.of(now, zone).toInstant(), zone));

		var testsuite = readValidXmlFile(tempDirectory.resolve("TEST-dummy.xml"));
		assertThat(testsuite.attr("hostname")).isEqualTo(InetAddress.getLocalHost().getHostName());
		assertThat(testsuite.attr("timestamp")).isEqualTo("2016-01-28T14:02:59");
	}

	@Test
	void printsExceptionWhenReportsDirCannotBeCreated() throws Exception {
		var reportsDir = tempDirectory.resolve("dummy.txt");
		Files.write(reportsDir, Set.of("content"));

		var out = new StringWriter();
		var listener = new LegacyXmlReportGeneratingListener(reportsDir, new PrintWriter(out));

		listener.testPlanExecutionStarted(TestPlan.from(true, Set.of(), mock(), dummyOutputDirectoryCreator()));

		assertThat(out.toString()).containsSubsequence("Could not create reports directory",
			"FileAlreadyExistsException", "at ");
	}

	@Test
	void printsExceptionWhenReportCouldNotBeWritten() throws Exception {
		var engineDescriptor = new EngineDescriptor(UniqueId.forEngine("engine"), "Engine");

		var xmlFile = tempDirectory.resolve("TEST-engine.xml");
		Files.createDirectories(xmlFile);

		var out = new StringWriter();
		var listener = new LegacyXmlReportGeneratingListener(tempDirectory, new PrintWriter(out));

		listener.testPlanExecutionStarted(
			TestPlan.from(true, Set.of(engineDescriptor), mock(), dummyOutputDirectoryCreator()));
		listener.executionFinished(TestIdentifier.from(engineDescriptor), successful());

		assertThat(out.toString()).containsSubsequence("Could not write XML report", "Exception", "at ");
	}

	@Test
	void writesReportEntriesToSystemOutElement() throws Exception {
		var engineDescriptor = new EngineDescriptor(UniqueId.forEngine("engine"), "Engine");
		var childUniqueId = UniqueId.root("child", "test");
		engineDescriptor.addChild(new TestDescriptorStub(childUniqueId, "test"));
		var testPlan = TestPlan.from(true, Set.of(engineDescriptor), mock(), dummyOutputDirectoryCreator());

		var out = new StringWriter();
		var listener = new LegacyXmlReportGeneratingListener(tempDirectory, new PrintWriter(out));

		listener.testPlanExecutionStarted(testPlan);
		var testIdentifier = testPlan.getTestIdentifier(childUniqueId);
		listener.executionStarted(testIdentifier);
		listener.reportingEntryPublished(testIdentifier, ReportEntry.from("foo", "bar"));
		Map<String, String> map = new LinkedHashMap<>();
		map.put("bar", "baz");
		map.put("qux", "foo");
		listener.reportingEntryPublished(testIdentifier, ReportEntry.from(map));
		listener.executionFinished(testIdentifier, successful());
		listener.executionFinished(testPlan.getTestIdentifier(engineDescriptor.getUniqueId()), successful());

		var testsuite = readValidXmlFile(tempDirectory.resolve("TEST-engine.xml"));

		assertThat(String.join("\n", testsuite.child("testcase").children("system-out").texts())) //
				.containsSubsequence( //
					"Report Entry #1 (timestamp: " + Year.now(), "- foo: bar\n",
					"Report Entry #2 (timestamp: " + Year.now(), "- bar: baz\n", "- qux: foo\n");
	}

	private void executeTests(TestEngine engine) {
		executeTests(engine, Clock.systemDefaultZone());
	}

	private void executeTests(TestEngine engine, Clock clock) {
		var out = new PrintWriter(new StringWriter());
		var reportListener = new LegacyXmlReportGeneratingListener(tempDirectory.toString(), out, clock);
		var launcher = createLauncher(engine);
		launcher.registerTestExecutionListeners(reportListener);
		var request = request() //
				.configurationParameter(LauncherConstants.STACKTRACE_PRUNING_ENABLED_PROPERTY_NAME, "false") //
				.selectors(selectUniqueId(UniqueId.forEngine(engine.getId()))) //
				.forExecution() //
				.build();
		launcher.execute(request);
	}

	private Match readValidXmlFile(Path xmlFile) throws Exception {
		assertTrue(Files.exists(xmlFile), () -> "File does not exist: " + xmlFile);
		try (var reader = Files.newBufferedReader(xmlFile)) {
			var xml = $(reader);
			assertValidAccordingToJenkinsSchema(xml.document());
			return xml;
		}
	}

}
