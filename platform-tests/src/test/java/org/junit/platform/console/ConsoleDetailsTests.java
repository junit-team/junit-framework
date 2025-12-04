/*
 * Copyright 2015-2025 the original author or authors.
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v2.0 which
 * accompanies this distribution and is available at
 *
 * https://www.eclipse.org/legal/epl-v20.html
 */

package org.junit.platform.console;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.junit.jupiter.api.Assertions.assertLinesMatch;
import static org.junit.jupiter.api.Assertions.fail;
import static org.junit.jupiter.api.Assumptions.assumeTrue;
import static org.junit.jupiter.api.DynamicContainer.dynamicContainer;
import static org.junit.jupiter.api.DynamicTest.dynamicTest;
import static org.junit.platform.commons.support.HierarchyTraversalMode.TOP_DOWN;
import static org.junit.platform.commons.support.ReflectionSupport.findMethods;
import static org.junit.platform.commons.util.ReflectionUtils.getFullyQualifiedMethodName;

import java.io.File;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.DynamicNode;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestFactory;
import org.junit.jupiter.api.TestReporter;
import org.junit.jupiter.api.function.Executable;
import org.junit.platform.console.options.Details;
import org.junit.platform.console.output.Theme;
import org.opentest4j.TestAbortedException;

/**
 * @since 1.0
 */
class ConsoleDetailsTests {

	@TestFactory
	@DisplayName("Basic tests and annotations usage")
	List<DynamicNode> basic() {
		return scanContainerClassAndCreateDynamicTests(BasicTestCase.class);
	}

	@TestFactory
	@DisplayName("Skipped and disabled tests")
	List<DynamicNode> skipped() {
		return scanContainerClassAndCreateDynamicTests(SkipTestCase.class);
	}

	@TestFactory
	@DisplayName("Failed tests")
	List<DynamicNode> failed() {
		return scanContainerClassAndCreateDynamicTests(FailTestCase.class);
	}

	@TestFactory
	@DisplayName("Tests publishing report entries")
	List<DynamicNode> reports() {
		return scanContainerClassAndCreateDynamicTests(ReportTestCase.class);
	}

	private List<DynamicNode> scanContainerClassAndCreateDynamicTests(Class<?> containerClass) {
		var containerName = containerClass.getSimpleName().replace("TestCase", "");
		// String containerName = containerClass.getSimpleName();
		List<DynamicNode> nodes = new ArrayList<>();
		Map<Details, List<DynamicTest>> map = new EnumMap<>(Details.class);
		for (var method : findMethods(containerClass, m -> m.isAnnotationPresent(Test.class), TOP_DOWN)) {
			var methodName = method.getName();
			var types = method.getParameterTypes();
			for (var details : Details.values()) {
				var tests = map.computeIfAbsent(details, key -> new ArrayList<>());
				for (var theme : Theme.values()) {
					var caption = containerName + "-" + methodName + "-" + details + "-" + theme;
					String[] args = { //
							"execute", //
							"--include-engine", "junit-jupiter", //
							"--details", details.name(), //
							"--details-theme", theme.name(), //
							"--disable-ansi-colors", //
							"--disable-banner", //
							"--include-classname", containerClass.getCanonicalName(), //
							"--select-method", getFullyQualifiedMethodName(containerClass, methodName, types) //
					};
					var displayName = methodName + "() " + theme.name();
					var dirName = "console/details/" + containerName.toLowerCase();
					var outName = caption + ".out.txt";
					var runner = new Runner(dirName, outName, args);
					var source = toUri(dirName, outName).orElse(null);
					tests.add(dynamicTest(displayName, source, runner));
				}
			}
		}
		var source = new File("src/test/resources/console/details").toURI();
		map.forEach((details, tests) -> nodes.add(dynamicContainer(details.name(), source, tests.stream())));
		return nodes;
	}

	@SuppressWarnings("JUnitMalformedDeclaration")
	@DisplayName("Basic")
	static class BasicTestCase {

		@Test
		void empty() {
		}

		@Test
		@DisplayName(".oO fancy display name Oo.")
		void changeDisplayName() {
		}

	}

	@SuppressWarnings("JUnitMalformedDeclaration")
	@DisplayName("Skip")
	static class SkipTestCase {

		@Test
		@Disabled("single line skip reason")
		void skipWithSingleLineReason() {
		}

		@Test
		@Disabled("multi\nline\nfail\nmessage")
		void skipWithMultiLineMessage() {
		}

	}

	@SuppressWarnings("JUnitMalformedDeclaration")
	@DisplayName("Fail")
	static class FailTestCase {

		@Test
		void failWithSingleLineMessage() {
			fail("single line fail message");
		}

		@Test
		void failWithMultiLineMessage() {
			fail("multi\nline\nfail\nmessage");
		}

	}

	@SuppressWarnings("JUnitMalformedDeclaration")
	@DisplayName("Report")
	static class ReportTestCase {

		@Test
		void reportSingleMessage(TestReporter reporter) {
			reporter.publishEntry("foo");
		}

		@Test
		void reportMultipleMessages(TestReporter reporter) {
			reporter.publishEntry("foo");
			reporter.publishEntry("bar");
		}

		@Test
		void reportSingleEntryWithSingleMapping(TestReporter reporter) {
			reporter.publishEntry("foo", "bar");
		}

		@Test
		void reportMultiEntriesWithSingleMapping(TestReporter reporter) {
			reporter.publishEntry("foo", "bar");
			reporter.publishEntry("far", "boo");
		}

		@Test
		void reportMultiEntriesWithMultiMappings(TestReporter reporter) {
			Map<String, String> values = new LinkedHashMap<>();
			values.put("user name", "dk38");
			values.put("award year", "1974");
			reporter.publishEntry(values);
			reporter.publishEntry("single", "mapping");
			Map<String, String> more = new LinkedHashMap<>();
			more.put("user name", "st77");
			more.put("award year", "1977");
			more.put("last seen", "2001");
			reporter.publishEntry(more);
		}

	}

	private record Runner(String dirName, String outName, String... args) implements Executable {

		@Override
		public void execute() throws Throwable {
			var wrapper = new ConsoleLauncherWrapper();
			var result = wrapper.execute(Optional.empty(), args);

			var optionalUri = toUri(dirName, outName);
			if (optionalUri.isEmpty()) {
				if (Boolean.getBoolean("org.junit.platform.console.ConsoleDetailsTests.writeResultOut")) {
					// do not use Files.createTempDirectory(prefix) as we want one folder for one container
					var temp = Path.of(System.getProperty("java.io.tmpdir"), dirName.replace('/', '-'));
					Files.createDirectories(temp);
					var path = Files.writeString(temp.resolve(outName), result.out);
					throw new TestAbortedException(
						"resource `%s` not found\nwrote console stdout to: %s/%s".formatted(dirName, outName, path));
				}
				fail("could not load resource named `" + dirName + "/" + outName + "`");
			}

			var path = Path.of(optionalUri.get());
			assumeTrue(Files.exists(path), "path does not exist: " + path);
			assumeTrue(Files.isReadable(path), "can not read: " + path);

			var expectedLines = Files.readAllLines(path, UTF_8);
			var actualLines = List.of(result.out.split("\\R"));

			assertLinesMatch(expectedLines, actualLines);
		}
	}

	static Optional<URI> toUri(String dirName, String outName) {
		var resourceName = dirName + "/" + outName;
		var url = ConsoleDetailsTests.class.getClassLoader().getResource(resourceName);
		if (url == null) {
			return Optional.empty();
		}
		try {
			return Optional.of(url.toURI());
		}
		catch (URISyntaxException e) {
			return Optional.empty();
		}
	}

}
