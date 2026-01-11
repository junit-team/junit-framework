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

import java.util.HashSet;
import java.util.Set;

import org.junit.platform.commons.JUnitException;
import org.junit.platform.commons.logging.Logger;
import org.junit.platform.commons.logging.LoggerFactory;
import org.junit.platform.commons.util.Preconditions;
import org.junit.platform.engine.TestEngine;

/**
 * @since 1.7
 */
class EngineIdValidator {

	private EngineIdValidator() {
	}

	static Iterable<TestEngine> validate(Iterable<TestEngine> testEngines) {
		Set<String> ids = new HashSet<>();
		for (TestEngine testEngine : testEngines) {
			// check usage of reserved ID prefix
			if (!validateReservedIds(testEngine)) {
				getLogger().warn(
					() -> "Third-party TestEngine implementations are forbidden to use the reserved 'junit-' prefix for their ID: '%s'".formatted(
						testEngine.getId()));
			}

			// check uniqueness
			if (!ids.add(testEngine.getId())) {
				throw new JUnitException(
					"Cannot create Launcher for multiple engines with the same ID '%s'.".formatted(testEngine.getId()));
			}
		}
		return testEngines;
	}

	private static Logger getLogger() {
		// Not a constant to avoid problems with building GraalVM native images
		return LoggerFactory.getLogger(EngineIdValidator.class);
	}

	// https://github.com/junit-team/junit-framework/issues/1557
	private static boolean validateReservedIds(TestEngine testEngine) {
		String engineId = Preconditions.notBlank(testEngine.getId(),
			() -> "ID for TestEngine [%s] must not be null or blank".formatted(testEngine.getClass().getName()));
		if (!engineId.startsWith("junit-")) {
			return true;
		}
		return switch (engineId) {
			case "junit-jupiter" -> {
				validateWellKnownClassName(testEngine, "org.junit.jupiter.engine.JupiterTestEngine");
				yield true;
			}
			case "junit-vintage" -> {
				validateWellKnownClassName(testEngine, "org.junit.vintage.engine.VintageTestEngine");
				yield true;
			}
			case "junit-platform-suite" -> {
				validateWellKnownClassName(testEngine, "org.junit.platform.suite.engine.SuiteTestEngine");
				yield true;
			}
			default -> false;
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
