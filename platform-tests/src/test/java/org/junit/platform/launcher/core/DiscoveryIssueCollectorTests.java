/*
 * Copyright 2015-2026 the original author or authors.
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v2.0 which
 * accompanies this distribution and is available at
 *
 * https://www.eclipse.org/legal/epl-v20.html
 */

package org.junit.platform.launcher.core;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.platform.engine.discovery.DiscoverySelectors.selectClass;
import static org.junit.platform.engine.discovery.DiscoverySelectors.selectClasspathResource;
import static org.junit.platform.engine.discovery.DiscoverySelectors.selectDirectory;
import static org.junit.platform.engine.discovery.DiscoverySelectors.selectFile;
import static org.junit.platform.engine.discovery.DiscoverySelectors.selectMethod;
import static org.junit.platform.engine.discovery.DiscoverySelectors.selectPackage;
import static org.junit.platform.engine.discovery.DiscoverySelectors.selectUri;
import static org.mockito.Mockito.mock;

import java.io.File;
import java.net.URI;
import java.util.stream.Stream;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.RecordArguments;
import org.junit.platform.engine.DiscoveryIssue;
import org.junit.platform.engine.DiscoveryIssue.Severity;
import org.junit.platform.engine.DiscoverySelector;
import org.junit.platform.engine.SelectorResolutionResult;
import org.junit.platform.engine.TestSource;
import org.junit.platform.engine.UniqueId;
import org.junit.platform.engine.discovery.FilePosition;
import org.junit.platform.engine.support.descriptor.ClassSource;
import org.junit.platform.engine.support.descriptor.ClasspathResourceSource;
import org.junit.platform.engine.support.descriptor.DirectorySource;
import org.junit.platform.engine.support.descriptor.FileSource;
import org.junit.platform.engine.support.descriptor.PackageSource;
import org.junit.platform.engine.support.descriptor.UriSource;

class DiscoveryIssueCollectorTests {

	@ParameterizedTest(name = "{0}")
	@MethodSource("pairs")
	void reportsFailedResolutionResultAsDiscoveryIssue(DiscoverySelector selector, TestSource source) {
		var collector = new DiscoveryIssueCollector(mock());
		var failure = SelectorResolutionResult.failed(new RuntimeException("boom"));
		collector.selectorProcessed(UniqueId.forEngine("dummy"), selector, failure);

		var expectedIssue = DiscoveryIssue.builder(Severity.ERROR, selector + " resolution failed") //
				.cause(failure.getThrowable()) //
				.source(source) //
				.build();
		assertThat(collector.toNotifier().getAllIssues()).containsExactly(expectedIssue);
	}

	public static Stream<Pair> pairs() {
		return Stream.of( //
			new Pair(selectClass("SomeClass"), ClassSource.from("SomeClass")), //
			new Pair(selectMethod("SomeClass#someMethod(int,int)"),
				org.junit.platform.engine.support.descriptor.MethodSource.from("SomeClass", "someMethod", "int,int")), //
			new Pair(selectClasspathResource("someResource"), ClasspathResourceSource.from("someResource")), //
			new Pair(selectClasspathResource("someResource", FilePosition.from(42)),
				ClasspathResourceSource.from("someResource",
					org.junit.platform.engine.support.descriptor.FilePosition.from(42))), //
			new Pair(selectClasspathResource("someResource", FilePosition.from(42, 23)),
				ClasspathResourceSource.from("someResource",
					org.junit.platform.engine.support.descriptor.FilePosition.from(42, 23))), //
			new Pair(selectPackage(""), PackageSource.from("")), //
			new Pair(selectPackage("some.package"), PackageSource.from("some.package")), //
			new Pair(selectFile("someFile"), FileSource.from(new File("someFile"))), //
			new Pair(selectFile("someFile", FilePosition.from(42)),
				FileSource.from(new File("someFile"),
					org.junit.platform.engine.support.descriptor.FilePosition.from(42))), //
			new Pair(selectFile("someFile", FilePosition.from(42, 23)),
				FileSource.from(new File("someFile"),
					org.junit.platform.engine.support.descriptor.FilePosition.from(42, 23))), //
			new Pair(selectDirectory("someDir"), DirectorySource.from(new File("someDir"))), //
			new Pair(selectUri("some:uri"), UriSource.from(URI.create("some:uri"))) //
		);
	}

	record Pair(DiscoverySelector selector, TestSource source) implements RecordArguments {
	}
}
