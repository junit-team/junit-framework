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

import static java.lang.String.join;
import static java.util.stream.Collectors.joining;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

import org.junit.platform.commons.util.ClassLoaderUtils;
import org.junit.platform.commons.util.CollectionUtils;
import org.junit.platform.engine.TestEngine;

class TestEngineFormatter {

	private TestEngineFormatter() {
	}

	@SuppressWarnings("unchecked")
	static String format(String title, Iterable<TestEngine> testEngines) {
		return format(title, (Stream<TestEngine>) CollectionUtils.toStream(testEngines));
	}

	private static String format(String title, Stream<TestEngine> testEngines) {
		String details = testEngines //
				.map(engine -> "- %s (%s)".formatted(engine.getId(), join(", ", computeAttributes(engine)))) //
				.collect(joining("\n"));
		return title + ":" + (details.isEmpty() ? " <none>" : "\n" + details);
	}

	private static List<String> computeAttributes(TestEngine engine) {
		List<String> attributes = new ArrayList<>(4);
		engine.getGroupId().ifPresent(groupId -> attributes.add("group ID: " + groupId));
		engine.getArtifactId().ifPresent(artifactId -> attributes.add("artifact ID: " + artifactId));
		engine.getVersion().ifPresent(version -> attributes.add("version: " + version));
		ClassLoaderUtils.getLocation(engine).ifPresent(location -> attributes.add("location: " + location));
		return attributes;
	}
}
