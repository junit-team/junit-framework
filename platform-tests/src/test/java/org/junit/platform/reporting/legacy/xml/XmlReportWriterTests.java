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

import static java.util.stream.Collectors.joining;
import static org.assertj.core.api.Assertions.assertThat;
import static org.joox.JOOX.$;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.params.provider.Arguments.arguments;
import static org.junit.platform.commons.util.CollectionUtils.getOnlyElement;
import static org.junit.platform.engine.TestExecutionResult.failed;
import static org.junit.platform.engine.TestExecutionResult.successful;
import static org.junit.platform.launcher.LauncherConstants.STDERR_REPORT_ENTRY_KEY;
import static org.junit.platform.launcher.LauncherConstants.STDOUT_REPORT_ENTRY_KEY;
import static org.junit.platform.launcher.core.OutputDirectoryCreators.dummyOutputDirectoryCreator;
import static org.junit.platform.reporting.legacy.xml.XmlReportAssertions.assertValidAccordingToJenkinsSchema;
import static org.junit.platform.reporting.legacy.xml.XmlReportWriter.ILLEGAL_CHARACTER_REPLACEMENT;
import static org.mockito.Mockito.mock;

import java.io.StringReader;
import java.io.StringWriter;
import java.io.Writer;
import java.time.Clock;
import java.util.Map;
import java.util.Set;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import org.joox.Match;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.platform.engine.ConfigurationParameters;
import org.junit.platform.engine.UniqueId;
import org.junit.platform.engine.reporting.ReportEntry;
import org.junit.platform.engine.support.descriptor.EngineDescriptor;
import org.junit.platform.fakes.TestDescriptorStub;
import org.junit.platform.launcher.TestIdentifier;
import org.junit.platform.launcher.TestPlan;

/**
 * @since 1.0
 */
class XmlReportWriterTests {

	private final ConfigurationParameters configParams = mock();

	private EngineDescriptor engineDescriptor = new EngineDescriptor(UniqueId.forEngine("engine"), "Engine");

	@Test
	void writesTestsuiteElementsWithoutTestcaseElementsWithoutAnyTests() throws Exception {
		var testPlan = TestPlan.from(true, Set.of(engineDescriptor), configParams, dummyOutputDirectoryCreator());

		var reportData = new XmlReportData(testPlan, Clock.systemDefaultZone());

		var testsuite = writeXmlReport(testPlan, reportData);

		assertValidAccordingToJenkinsSchema(testsuite.document());
		assertThat(testsuite.document().getDocumentElement().getTagName()).isEqualTo("testsuite");
		assertThat(testsuite.attr("name")).isEqualTo("Engine");
		assertThat(testsuite.attr("tests", int.class)).isEqualTo(0);
		assertThat(testsuite.find("testcase")).isEmpty();
	}

	@Test
	void writesReportEntry() throws Exception {
		var uniqueId = engineDescriptor.getUniqueId().append("test", "test");
		var testDescriptor = new TestDescriptorStub(uniqueId, "successfulTest");
		engineDescriptor.addChild(testDescriptor);
		var testPlan = TestPlan.from(true, Set.of(engineDescriptor), configParams, dummyOutputDirectoryCreator());

		var reportData = new XmlReportData(testPlan, Clock.systemDefaultZone());
		reportData.addReportEntry(TestIdentifier.from(testDescriptor), ReportEntry.from("myKey", "myValue"));
		reportData.markFinished(testPlan.getTestIdentifier(uniqueId), successful());

		var testsuite = writeXmlReport(testPlan, reportData);

		assertValidAccordingToJenkinsSchema(testsuite.document());
		assertThat(String.join("\n", testsuite.find("system-out").texts())) //
				.containsSubsequence("Report Entry #1 (timestamp: ", "- myKey: myValue");
	}

	@Test
	void writesCapturedOutput() throws Exception {
		var uniqueId = engineDescriptor.getUniqueId().append("test", "test");
		var testDescriptor = new TestDescriptorStub(uniqueId, "successfulTest");
		engineDescriptor.addChild(testDescriptor);
		var testPlan = TestPlan.from(true, Set.of(engineDescriptor), configParams, dummyOutputDirectoryCreator());

		var reportData = new XmlReportData(testPlan, Clock.systemDefaultZone());
		var reportEntry = ReportEntry.from(Map.of( //
			STDOUT_REPORT_ENTRY_KEY, "normal output", //
			STDERR_REPORT_ENTRY_KEY, "error output", //
			"foo", "bar"));
		reportData.addReportEntry(TestIdentifier.from(testDescriptor), reportEntry);
		reportData.addReportEntry(TestIdentifier.from(testDescriptor), ReportEntry.from(Map.of("baz", "qux")));
		reportData.markFinished(testPlan.getTestIdentifier(uniqueId), successful());

		var testsuite = writeXmlReport(testPlan, reportData);

		assertValidAccordingToJenkinsSchema(testsuite.document());
		assertThat(testsuite.find("system-out").text(0)) //
				.containsSubsequence("unique-id: ", "test:test", "display-name: successfulTest");
		assertThat(testsuite.find("system-out").text(1)) //
				.containsSubsequence("Report Entry #1 (timestamp: ", "- foo: bar", "Report Entry #2 (timestamp: ",
					"- baz: qux");
		assertThat(testsuite.find("system-out").text(2).strip()) //
				.isEqualTo("normal output");
		assertThat(testsuite.find("system-err").text().strip()) //
				.isEqualTo("error output");
	}

	@Test
	void writesEmptySkippedElementForSkippedTestWithoutReason() throws Exception {
		var uniqueId = engineDescriptor.getUniqueId().append("test", "test");
		engineDescriptor.addChild(new TestDescriptorStub(uniqueId, "skippedTest"));
		var testPlan = TestPlan.from(true, Set.of(engineDescriptor), configParams, dummyOutputDirectoryCreator());

		var reportData = new XmlReportData(testPlan, Clock.systemDefaultZone());
		reportData.markSkipped(testPlan.getTestIdentifier(uniqueId), null);

		var testsuite = writeXmlReport(testPlan, reportData);

		assertValidAccordingToJenkinsSchema(testsuite.document());
		var testcase = testsuite.child("testcase");
		assertThat(testcase.attr("name")).isEqualTo("skippedTest");
		var skipped = testcase.child("skipped");
		assertThat(skipped.size()).isEqualTo(1);
		assertThat(skipped.children()).isEmpty();
	}

	@Test
	void writesEmptyErrorElementForFailedTestWithoutCause() throws Exception {
		engineDescriptor = new EngineDescriptor(UniqueId.forEngine("myEngineId"), "Fancy Engine") {
			@Override
			public String getLegacyReportingName() {
				return "myEngine";
			}
		};
		var uniqueId = engineDescriptor.getUniqueId().append("test", "test");
		engineDescriptor.addChild(new TestDescriptorStub(uniqueId, "some fancy name") {
			@Override
			public String getLegacyReportingName() {
				return "failedTest";
			}
		});
		var testPlan = TestPlan.from(true, Set.of(engineDescriptor), configParams, dummyOutputDirectoryCreator());

		var reportData = new XmlReportData(testPlan, Clock.systemDefaultZone());
		reportData.markFinished(testPlan.getTestIdentifier(uniqueId), failed(null));

		var testsuite = writeXmlReport(testPlan, reportData);

		assertValidAccordingToJenkinsSchema(testsuite.document());
		var testcase = testsuite.child("testcase");
		assertThat(testcase.attr("name")).isEqualTo("failedTest");
		assertThat(testcase.attr("classname")).isEqualTo("myEngine");
		var error = testcase.child("error");
		assertThat(error.size()).isEqualTo(1);
		assertThat(error.children()).isEmpty();
	}

	@Test
	void omitsMessageAttributeForFailedTestWithThrowableWithoutMessage() throws Exception {
		var uniqueId = engineDescriptor.getUniqueId().append("test", "test");
		engineDescriptor.addChild(new TestDescriptorStub(uniqueId, "failedTest"));
		var testPlan = TestPlan.from(true, Set.of(engineDescriptor), configParams, dummyOutputDirectoryCreator());

		var reportData = new XmlReportData(testPlan, Clock.systemDefaultZone());
		reportData.markFinished(testPlan.getTestIdentifier(uniqueId), failed(new NullPointerException()));

		var testsuite = writeXmlReport(testPlan, reportData);

		assertValidAccordingToJenkinsSchema(testsuite.document());
		var error = testsuite.find("error");
		assertThat(error.attr("type")).isEqualTo("java.lang.NullPointerException");
		assertThat(error.attr("message")).isNull();
	}

	@Test
	void writesValidXmlEvenIfExceptionMessageContainsCData() throws Exception {
		var uniqueId = engineDescriptor.getUniqueId().append("test", "test");
		engineDescriptor.addChild(new TestDescriptorStub(uniqueId, "test"));
		var testPlan = TestPlan.from(true, Set.of(engineDescriptor), configParams, dummyOutputDirectoryCreator());

		var reportData = new XmlReportData(testPlan, Clock.systemDefaultZone());
		var assertionError = new AssertionError("<foo><![CDATA[bar]]></foo>");
		reportData.markFinished(testPlan.getTestIdentifier(uniqueId), failed(assertionError));

		var testsuite = writeXmlReport(testPlan, reportData);

		assertValidAccordingToJenkinsSchema(testsuite.document());
		assertThat(testsuite.find("failure").attr("message")).isEqualTo("<foo><![CDATA[bar]]></foo>");
	}

	@Test
	void escapesInvalidCharactersInSystemPropertiesAndExceptionMessages() throws Exception {
		var uniqueId = engineDescriptor.getUniqueId().append("test", "test");
		engineDescriptor.addChild(new TestDescriptorStub(uniqueId, "test"));
		var testPlan = TestPlan.from(true, Set.of(engineDescriptor), configParams, dummyOutputDirectoryCreator());

		var reportData = new XmlReportData(testPlan, Clock.systemDefaultZone());
		var assertionError = new AssertionError("expected: <A> but was: <B\0>");
		reportData.markFinished(testPlan.getTestIdentifier(uniqueId), failed(assertionError));

		System.setProperty("foo.bar", "\1");
		Match testsuite;
		try {
			testsuite = writeXmlReport(testPlan, reportData);
		}
		finally {
			System.getProperties().remove("foo.bar");
		}

		assertValidAccordingToJenkinsSchema(testsuite.document());
		assertThat(testsuite.find("property").matchAttr("name", "foo\\.bar").attr("value")) //
				.isEqualTo(String.valueOf(ILLEGAL_CHARACTER_REPLACEMENT));
		var failure = testsuite.find("failure");
		assertThat(failure.attr("message")) //
				.isEqualTo("expected: <A> but was: <B" + ILLEGAL_CHARACTER_REPLACEMENT + ">");
		assertThat(failure.text()) //
				.contains("AssertionError: expected: <A> but was: <B" + ILLEGAL_CHARACTER_REPLACEMENT + ">");
	}

	@ParameterizedTest(name = "[{index}]")
	@MethodSource("stringPairs")
	void replacesIllegalCharacters(String input, String output) {
		assertEquals(output, XmlReportWriter.replaceIllegalCharacters(input));
	}

	@Test
	void writesValidXmlForExceptionMessagesContainingLineBreaks() throws Exception {
		var uniqueId = engineDescriptor.getUniqueId().append("test", "test");
		engineDescriptor.addChild(new TestDescriptorStub(uniqueId, "test"));
		var testPlan = TestPlan.from(true, Set.of(engineDescriptor), configParams, dummyOutputDirectoryCreator());

		var allWhitespaceCharacters = IntStream.range(0, 0x10000) //
				.filter(Character::isWhitespace) //
				.filter(XmlReportWriter::isAllowedXmlCharacter) //
				.mapToObj(Character::toString) //
				.collect(joining());

		var message = "a" + allWhitespaceCharacters + " b<&>";
		var reportData = new XmlReportData(testPlan, Clock.systemDefaultZone());
		var assertionError = new AssertionError(message);
		reportData.markFinished(testPlan.getTestIdentifier(uniqueId), failed(assertionError));

		var testsuite = writeXmlReport(testPlan, reportData);

		assertValidAccordingToJenkinsSchema(testsuite.document());

		var attributeValue = testsuite.find("failure").attr("message");
		assertThat(attributeValue).isEqualTo(message);
	}

	static Stream<Arguments> stringPairs() {
		return Stream.of( //
			arguments("\0", String.valueOf(ILLEGAL_CHARACTER_REPLACEMENT)), //
			arguments("\1", String.valueOf(ILLEGAL_CHARACTER_REPLACEMENT)), //
			arguments("\t", "\t"), //
			arguments("\r", "\r"), //
			arguments("\n", "\n"), //
			arguments("\u001f", String.valueOf(ILLEGAL_CHARACTER_REPLACEMENT)), //
			arguments("✅", "✅"), //
			arguments(" ", " "), //
			arguments("foo!", "foo!"), //
			arguments("\uD801\uDC00", "\uD801\uDC00") //
		);
	}

	private Match writeXmlReport(TestPlan testPlan, XmlReportData reportData) throws Exception {
		var out = new StringWriter();
		writeXmlReport(testPlan, reportData, out);
		return $(new StringReader(out.toString()));
	}

	private void writeXmlReport(TestPlan testPlan, XmlReportData reportData, Writer out) throws Exception {
		new XmlReportWriter(reportData).writeXmlReport(getOnlyElement(testPlan.getRoots()), out);
	}
}
