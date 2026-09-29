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

import static org.junit.platform.engine.DiscoveryIssue.Severity.WARNING;

import java.util.HashSet;
import java.util.Set;

import org.jspecify.annotations.Nullable;
import org.junit.platform.commons.JUnitException;
import org.junit.platform.commons.util.Preconditions;
import org.junit.platform.engine.DiscoveryIssue;
import org.junit.platform.engine.TestEngine;
import org.junit.platform.engine.UniqueId;

/**
 * @since 1.7
 */
class EngineIdValidator {

	private EngineIdValidator() {
		/* no-op */
	}

	static void validateReservedPrefix(TestEngine testEngine, UniqueId uniqueEngineId,
			DiscoveryIssueCollector issueCollector) {
		String engineId = testEngine.getId();
		if (engineId.startsWith("junit-") && wellKnownClassNameForEngineId(testEngine) == null) {
			var message = "Third-party TestEngine implementations are forbidden to use the reserved 'junit-' prefix for their ID";
			issueCollector.issueEncountered(uniqueEngineId, DiscoveryIssue.create(WARNING, message));
		}
	}

	static Iterable<TestEngine> validate(Iterable<TestEngine> testEngines) {
		Set<String> ids = new HashSet<>();
		for (TestEngine testEngine : testEngines) {
			// check usage of reserved ids
			validateReservedIds(testEngine);
			// check uniqueness
			if (!ids.add(testEngine.getId())) {
				throw new JUnitException(
					"Cannot create Launcher for multiple engines with the same ID '%s'.".formatted(testEngine.getId()));
			}
		}
		return testEngines;
	}

	// https://github.com/junit-team/junit-framework/issues/1557
	private static void validateReservedIds(TestEngine testEngine) {
		var expectedClassName = wellKnownClassNameForEngineId(testEngine);
		if (expectedClassName == null) {
			return;
		}
		validateWellKnownClassName(testEngine, expectedClassName);
	}

	private static @Nullable String wellKnownClassNameForEngineId(TestEngine testEngine) {
		String engineId = Preconditions.notBlank(testEngine.getId(),
			() -> "ID for TestEngine [%s] must not be null or blank".formatted(testEngine.getClass().getName()));
		return switch (engineId) {
			case "junit-jupiter" -> "org.junit.jupiter.engine.JupiterTestEngine";
			case "junit-vintage" -> "org.junit.vintage.engine.VintageTestEngine";
			case "junit-platform-suite" -> "org.junit.platform.suite.engine.SuiteTestEngine";
			default -> null;
		};
	}

	private static void validateWellKnownClassName(TestEngine testEngine, String expectedClassName) {
		String actualClassName = testEngine.getClass().getName();
		if (actualClassName.equals(expectedClassName)) {
			return;
		}
		throw new JUnitException(
			"Third-party TestEngine '%s' is forbidden to use the reserved '%s' TestEngine ID.".formatted(
				actualClassName, testEngine.getId()));
	}

}
