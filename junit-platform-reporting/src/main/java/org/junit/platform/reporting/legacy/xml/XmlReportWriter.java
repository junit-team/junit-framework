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

import static java.text.MessageFormat.format;
import static java.time.format.DateTimeFormatter.ISO_LOCAL_DATE_TIME;
import static java.util.Collections.emptyList;
import static java.util.Comparator.naturalOrder;
import static java.util.function.Function.identity;
import static java.util.stream.Collectors.counting;
import static java.util.stream.Collectors.groupingBy;
import static java.util.stream.Collectors.mapping;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toMap;
import static org.junit.platform.commons.util.ExceptionUtils.readStackTrace;
import static org.junit.platform.commons.util.StringUtils.isNotBlank;
import static org.junit.platform.engine.TestExecutionResult.Status.FAILED;
import static org.junit.platform.launcher.LauncherConstants.STDERR_REPORT_ENTRY_KEY;
import static org.junit.platform.launcher.LauncherConstants.STDOUT_REPORT_ENTRY_KEY;
import static org.junit.platform.reporting.legacy.xml.XmlReportWriter.AggregatedTestResult.Type.ERROR;
import static org.junit.platform.reporting.legacy.xml.XmlReportWriter.AggregatedTestResult.Type.FAILURE;
import static org.junit.platform.reporting.legacy.xml.XmlReportWriter.AggregatedTestResult.Type.SKIPPED;
import static org.junit.platform.reporting.legacy.xml.XmlReportWriter.AggregatedTestResult.Type.SUCCESS;

import java.io.IOException;
import java.io.Writer;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.text.NumberFormat;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Properties;
import java.util.TreeSet;
import java.util.regex.Pattern;

import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;

import org.jspecify.annotations.Nullable;
import org.junit.platform.engine.TestExecutionResult;
import org.junit.platform.engine.reporting.ReportEntry;
import org.junit.platform.launcher.TestIdentifier;
import org.junit.platform.launcher.TestPlan;
import org.junit.platform.reporting.legacy.LegacyReportingUtils;
import org.junit.platform.reporting.legacy.xml.XmlReportWriter.AggregatedTestResult.Type;

/**
 * {@code XmlReportWriter} writes an XML report whose format is compatible
 * with the de facto standard for JUnit 4 based test reports that was made
 * popular by the Ant build system.
 *
 * @since 1.4
 */
class XmlReportWriter {

	static final char ILLEGAL_CHARACTER_REPLACEMENT = '\uFFFD';

	private static final Map<Character, String> REPLACEMENTS_IN_ATTRIBUTE_VALUES = Map.of( //
		'\n', "&#10;", //
		'\r', "&#13;", //
		'\t', "&#9;" //
	);

	// Using zero-width assertions in the split pattern simplifies the splitting process: All split parts
	// (including the first and last one) can be used directly, without having to re-add separator characters.
	private static final Pattern CDATA_SPLIT_PATTERN = Pattern.compile("(?<=]])(?=>)");

	private final XmlReportData reportData;

	XmlReportWriter(XmlReportData reportData) {
		this.reportData = reportData;
	}

	void writeXmlReport(TestIdentifier rootDescriptor, Writer out) throws XMLStreamException {
		TestPlan testPlan = this.reportData.getTestPlan();
		Map<TestIdentifier, AggregatedTestResult> tests = testPlan.getDescendants(rootDescriptor) //
				.stream() //
				.filter(testIdentifier -> shouldInclude(testPlan, testIdentifier)) //
				.collect(toMap(identity(), this::toAggregatedResult)); //
		writeXmlReport(rootDescriptor, tests, out);
	}

	private AggregatedTestResult toAggregatedResult(TestIdentifier testIdentifier) {
		if (this.reportData.wasSkipped(testIdentifier)) {
			return AggregatedTestResult.skipped();
		}
		return AggregatedTestResult.nonSkipped(this.reportData.getResults(testIdentifier));
	}

	private boolean shouldInclude(TestPlan testPlan, TestIdentifier testIdentifier) {
		return testIdentifier.isTest() || testPlan.getChildren(testIdentifier).isEmpty();
	}

	private void writeXmlReport(TestIdentifier testIdentifier, Map<TestIdentifier, AggregatedTestResult> tests,
			Writer out) throws XMLStreamException {

		try (XmlReport report = new XmlReport(out)) {
			report.write(testIdentifier, tests);
		}
	}

	private class XmlReport implements AutoCloseable {

		private final XMLStreamWriter xml;
		private final ReplacingWriter out;

		XmlReport(Writer out) throws XMLStreamException {
			this.out = new ReplacingWriter(out);
			XMLOutputFactory factory = XMLOutputFactory.newInstance();
			this.xml = factory.createXMLStreamWriter(this.out);
		}

		void write(TestIdentifier testIdentifier, Map<TestIdentifier, AggregatedTestResult> tests)
				throws XMLStreamException {
			xml.writeStartDocument("UTF-8", "1.0");
			newLine();
			writeTestsuite(testIdentifier, tests);
			xml.writeEndDocument();
		}

		private void writeTestsuite(TestIdentifier testIdentifier, Map<TestIdentifier, AggregatedTestResult> tests)
				throws XMLStreamException {

			// NumberFormat is not thread-safe. Thus, we instantiate it here and pass it to
			// writeTestcase instead of using a constant
			NumberFormat numberFormat = NumberFormat.getInstance(Locale.US);

			xml.writeStartElement("testsuite");

			writeSuiteAttributes(testIdentifier, tests.values(), numberFormat);

			newLine();
			writeSystemProperties();

			for (Entry<TestIdentifier, AggregatedTestResult> entry : tests.entrySet()) {
				writeTestcase(entry.getKey(), entry.getValue(), numberFormat);
			}

			writeOutputElement("system-out", formatNonStandardAttributesAsString(testIdentifier));

			xml.writeEndElement();
			newLine();
		}

		private void writeSuiteAttributes(TestIdentifier testIdentifier, Collection<AggregatedTestResult> testResults,
				NumberFormat numberFormat) throws XMLStreamException {

			writeAttributeSafely("name", testIdentifier.getDisplayName());
			writeTestCounts(testResults);
			writeAttributeSafely("time", getTime(testIdentifier, numberFormat));
			writeAttributeSafely("hostname", getHostname().orElse("<unknown host>"));
			writeAttributeSafely("timestamp", ISO_LOCAL_DATE_TIME.format(getCurrentDateTime()));
		}

		private void writeTestCounts(Collection<AggregatedTestResult> testResults) throws XMLStreamException {
			Map<Type, Long> counts = testResults.stream().map(it -> it.type).collect(
				groupingBy(identity(), counting()));
			long total = counts.values().stream().mapToLong(Long::longValue).sum();
			writeAttributeSafely("tests", String.valueOf(total));
			writeAttributeSafely("skipped", counts.getOrDefault(SKIPPED, 0L).toString());
			writeAttributeSafely("failures", counts.getOrDefault(FAILURE, 0L).toString());
			writeAttributeSafely("errors", counts.getOrDefault(ERROR, 0L).toString());
		}

		private void writeSystemProperties() throws XMLStreamException {
			xml.writeStartElement("properties");
			newLine();
			Properties systemProperties = System.getProperties();
			for (String propertyName : new TreeSet<>(systemProperties.stringPropertyNames())) {
				xml.writeEmptyElement("property");
				writeAttributeSafely("name", propertyName);
				writeAttributeSafely("value", systemProperties.getProperty(propertyName));
				newLine();
			}
			xml.writeEndElement();
			newLine();
		}

		private void writeTestcase(TestIdentifier testIdentifier, AggregatedTestResult testResult,
				NumberFormat numberFormat) throws XMLStreamException {

			xml.writeStartElement("testcase");

			writeAttributeSafely("name", getName(testIdentifier));
			writeAttributeSafely("classname", getClassName(testIdentifier));
			writeAttributeSafely("time", getTime(testIdentifier, numberFormat));
			newLine();

			writeSkippedOrErrorOrFailureElement(testIdentifier, testResult);

			List<String> systemOutElements = new ArrayList<>();
			List<String> systemErrElements = new ArrayList<>();
			systemOutElements.add(formatNonStandardAttributesAsString(testIdentifier));
			collectReportEntries(testIdentifier, systemOutElements, systemErrElements);
			writeOutputElements("system-out", systemOutElements);
			writeOutputElements("system-err", systemErrElements);

			xml.writeEndElement();
			newLine();
		}

		private String getName(TestIdentifier testIdentifier) {
			return testIdentifier.getLegacyReportingName();
		}

		private String getClassName(TestIdentifier testIdentifier) {
			return LegacyReportingUtils.getClassName(reportData.getTestPlan(), testIdentifier);
		}

		private void writeSkippedOrErrorOrFailureElement(TestIdentifier testIdentifier, AggregatedTestResult testResult)
				throws XMLStreamException {

			if (testResult.type == SKIPPED) {
				writeSkippedElement(reportData.getSkipReason(testIdentifier), xml);
			}
			else {
				Map<Type, List<Optional<Throwable>>> throwablesByType = testResult.getThrowablesByType();
				for (Type type : EnumSet.of(FAILURE, ERROR)) {
					for (Optional<Throwable> throwable : throwablesByType.getOrDefault(type, emptyList())) {
						writeErrorOrFailureElement(type, throwable.orElse(null), xml);
					}
				}
			}
		}

		private void writeSkippedElement(@Nullable String reason, XMLStreamWriter writer) throws XMLStreamException {
			if (isNotBlank(reason)) {
				writer.writeStartElement("skipped");
				writeCDataSafely(reason);
				writer.writeEndElement();
			}
			else {
				writer.writeEmptyElement("skipped");
			}
			newLine();
		}

		private void writeErrorOrFailureElement(Type type, @Nullable Throwable throwable, XMLStreamWriter writer)
				throws XMLStreamException {

			String elementName = type == FAILURE ? "failure" : "error";
			if (throwable != null) {
				writer.writeStartElement(elementName);
				writeFailureAttributesAndContent(throwable);
				writer.writeEndElement();
			}
			else {
				writer.writeEmptyElement(elementName);
			}
			newLine();
		}

		private void writeFailureAttributesAndContent(Throwable throwable) throws XMLStreamException {

			if (throwable.getMessage() != null) {
				writeAttributeSafely("message", throwable.getMessage());
			}
			writeAttributeSafely("type", throwable.getClass().getName());
			writeCDataSafely(readStackTrace(throwable));
		}

		private void collectReportEntries(TestIdentifier testIdentifier, List<String> systemOutElements,
				List<String> systemErrElements) {
			List<ReportEntry> entries = reportData.getReportEntries(testIdentifier);
			if (!entries.isEmpty()) {
				List<String> systemOutElementsForCapturedOutput = new ArrayList<>();
				StringBuilder formattedReportEntries = new StringBuilder();
				for (int i = 0; i < entries.size(); i++) {
					ReportEntry reportEntry = entries.get(i);
					Map<String, String> keyValuePairs = new LinkedHashMap<>(reportEntry.getKeyValuePairs());
					removeIfPresentAndAddAsSeparateElement(keyValuePairs, STDOUT_REPORT_ENTRY_KEY,
						systemOutElementsForCapturedOutput);
					removeIfPresentAndAddAsSeparateElement(keyValuePairs, STDERR_REPORT_ENTRY_KEY, systemErrElements);
					if (!keyValuePairs.isEmpty()) {
						buildReportEntryDescription(reportEntry.getTimestamp(), keyValuePairs, i + 1,
							formattedReportEntries);
					}
				}
				systemOutElements.add(formattedReportEntries.toString().strip());
				systemOutElements.addAll(systemOutElementsForCapturedOutput);
			}
		}

		private void removeIfPresentAndAddAsSeparateElement(Map<String, String> keyValuePairs, String key,
				List<String> elements) {
			String value = keyValuePairs.remove(key);
			if (value != null) {
				elements.add(value);
			}
		}

		private void buildReportEntryDescription(LocalDateTime timestamp, Map<String, String> keyValuePairs,
				int entryNumber, StringBuilder result) {
			result.append(
				format("Report Entry #{0} (timestamp: {1})\n", entryNumber, ISO_LOCAL_DATE_TIME.format(timestamp)));
			keyValuePairs.forEach((key, value) -> result.append(format("\t- {0}: {1}\n", key, value)));
		}

		private String getTime(TestIdentifier testIdentifier, NumberFormat numberFormat) {
			return numberFormat.format(reportData.getDurationInSeconds(testIdentifier));
		}

		private Optional<String> getHostname() {
			try {
				return Optional.ofNullable(InetAddress.getLocalHost().getHostName());
			}
			catch (UnknownHostException e) {
				return Optional.empty();
			}
		}

		private LocalDateTime getCurrentDateTime() {
			return LocalDateTime.now(reportData.getClock()).withNano(0);
		}

		private String formatNonStandardAttributesAsString(TestIdentifier testIdentifier) {
			return "unique-id: " + testIdentifier.getUniqueId() //
					+ "\ndisplay-name: " + testIdentifier.getDisplayName();
		}

		private void writeOutputElements(String elementName, List<String> elements) throws XMLStreamException {
			for (String content : elements) {
				writeOutputElement(elementName, content);
			}
		}

		private void writeOutputElement(String elementName, String content) throws XMLStreamException {
			xml.writeStartElement(elementName);
			writeCDataSafely("\n" + content + "\n");
			xml.writeEndElement();
			newLine();
		}

		private void writeAttributeSafely(String name, String value) throws XMLStreamException {
			// Workaround for XMLStreamWriter implementations that don't escape
			// '\n', '\r', and '\t' characters in attribute values
			xml.flush();
			out.setWhitespaceReplacingEnabled(true);
			xml.writeAttribute(name, replaceIllegalCharacters(value));
			xml.flush();
			out.setWhitespaceReplacingEnabled(false);
		}

		private void writeCDataSafely(String data) throws XMLStreamException {
			for (String safeDataPart : CDATA_SPLIT_PATTERN.split(replaceIllegalCharacters(data))) {
				xml.writeCData(safeDataPart);
			}
		}

		private void newLine() throws XMLStreamException {
			xml.writeCharacters("\n");
		}

		@Override
		public void close() throws XMLStreamException {
			xml.flush();
			xml.close();
		}
	}

	static String replaceIllegalCharacters(String text) {
		if (text.codePoints().allMatch(XmlReportWriter::isAllowedXmlCharacter)) {
			return text;
		}
		StringBuilder result = new StringBuilder(text.length() * 2);
		text.codePoints().forEach(codePoint -> {
			if (isAllowedXmlCharacter(codePoint)) {
				result.appendCodePoint(codePoint);
			}
			else {
				result.append(ILLEGAL_CHARACTER_REPLACEMENT);
			}
		});
		return result.toString();
	}

	static boolean isAllowedXmlCharacter(int codePoint) {
		// source: https://www.w3.org/TR/xml/#charsets
		return codePoint == 0x9 //
				|| codePoint == 0xA //
				|| codePoint == 0xD //
				|| (codePoint >= 0x20 && codePoint <= 0xD7FF) //
				|| (codePoint >= 0xE000 && codePoint <= 0xFFFD) //
				|| (codePoint >= 0x10000 && codePoint <= 0x10FFFF);
	}

	static class AggregatedTestResult {

		private static final AggregatedTestResult SKIPPED_RESULT = new AggregatedTestResult(SKIPPED, emptyList());

		public static AggregatedTestResult skipped() {
			return SKIPPED_RESULT;
		}

		public static AggregatedTestResult nonSkipped(List<TestExecutionResult> executionResults) {
			Type type = executionResults.stream() //
					.map(Type::from) //
					.max(naturalOrder()) //
					.orElse(SUCCESS);
			return new AggregatedTestResult(type, executionResults);
		}

		private final Type type;
		private final List<TestExecutionResult> executionResults;

		private AggregatedTestResult(Type type, List<TestExecutionResult> executionResults) {
			this.type = type;
			this.executionResults = executionResults;
		}

		public Map<Type, List<Optional<Throwable>>> getThrowablesByType() {
			return executionResults.stream() //
					.collect(groupingBy(Type::from, mapping(TestExecutionResult::getThrowable, toList())));
		}

		enum Type {

			SUCCESS, SKIPPED, FAILURE, ERROR;

			private static Type from(TestExecutionResult executionResult) {
				if (executionResult.getStatus() == FAILED) {
					return isFailure(executionResult) ? FAILURE : ERROR;
				}
				return SUCCESS;
			}

			private static boolean isFailure(TestExecutionResult result) {
				Optional<Throwable> throwable = result.getThrowable();
				return throwable.isPresent() && throwable.get() instanceof AssertionError;
			}
		}
	}

	private static class ReplacingWriter extends Writer {

		private final Writer delegate;
		private boolean whitespaceReplacingEnabled;

		ReplacingWriter(Writer delegate) {
			this.delegate = delegate;
		}

		void setWhitespaceReplacingEnabled(boolean whitespaceReplacingEnabled) {
			this.whitespaceReplacingEnabled = whitespaceReplacingEnabled;
		}

		@Override
		public void write(char[] cbuf, int off, int len) throws IOException {
			if (!whitespaceReplacingEnabled) {
				delegate.write(cbuf, off, len);
				return;
			}
			StringBuilder stringBuilder = new StringBuilder(len * 2);
			for (int i = off; i < off + len; i++) {
				char c = cbuf[i];
				String replacement = REPLACEMENTS_IN_ATTRIBUTE_VALUES.get(c);
				if (replacement != null) {
					stringBuilder.append(replacement);
				}
				else {
					stringBuilder.append(c);
				}
			}
			delegate.write(stringBuilder.toString());
		}

		@Override
		public void write(int c) throws IOException {
			if (whitespaceReplacingEnabled) {
				super.write(c);
			}
			else {
				delegate.write(c);
			}
		}

		@Override
		public void write(char[] cbuf) throws IOException {
			if (whitespaceReplacingEnabled) {
				super.write(cbuf);
			}
			else {
				delegate.write(cbuf);
			}
		}

		@Override
		public void write(String str) throws IOException {
			if (whitespaceReplacingEnabled) {
				super.write(str);
			}
			else {
				delegate.write(str);
			}
		}

		@Override
		public void write(String str, int off, int len) throws IOException {
			if (whitespaceReplacingEnabled) {
				super.write(str, off, len);
			}
			else {
				delegate.write(str, off, len);
			}
		}

		@Override
		public Writer append(CharSequence csq) throws IOException {
			if (whitespaceReplacingEnabled) {
				return super.append(csq);
			}
			else {
				return delegate.append(csq);
			}
		}

		@Override
		public Writer append(CharSequence csq, int start, int end) throws IOException {
			if (whitespaceReplacingEnabled) {
				return super.append(csq, start, end);
			}
			else {
				return delegate.append(csq, start, end);
			}
		}

		@Override
		public Writer append(char c) throws IOException {
			if (whitespaceReplacingEnabled) {
				return super.append(c);
			}
			else {
				return delegate.append(c);
			}
		}

		@Override
		public void flush() throws IOException {
			delegate.flush();
		}

		@Override
		public void close() throws IOException {
			delegate.close();
		}
	}

}
