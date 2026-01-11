/*
 * Copyright 2015-2026 the original author or authors.
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v2.0 which
 * accompanies this distribution and is available at
 *
 * https://www.eclipse.org/legal/epl-v20.html
 */

package org.junit.vintage.engine.descriptor;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.DynamicContainer.dynamicContainer;
import static org.junit.jupiter.api.DynamicTest.dynamicTest;

import java.util.stream.Stream;

import org.junit.internal.builders.AllDefaultPossibilitiesBuilder;
import org.junit.jupiter.api.DynamicNode;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.TestFactory;
import org.junit.platform.commons.support.ReflectionSupport;
import org.junit.runner.Description;
import org.junit.vintage.engine.discovery.IsPotentialJUnit4TestClass;

class DescriptionUtilsTests {

	@SuppressWarnings("deprecation")
	AllDefaultPossibilitiesBuilder builder = new AllDefaultPossibilitiesBuilder(true);

	@TestFactory
	Stream<DynamicNode> computedMethodNameCorrectly() {
		var testClasses = ReflectionSupport.findAllClassesInPackage("org.junit.vintage.engine.samples",
			new IsPotentialJUnit4TestClass(), name -> true);
		return testClasses.stream().flatMap(this::toDynamicTests);
	}

	private Stream<DynamicNode> toDynamicTests(Class<?> testClass) {
		try {
			var runner = builder.runnerForClass(testClass);
			return toDynamicTests(Stream.of(runner.getDescription()));
		}
		catch (Throwable throwable) {
			throw new RuntimeException(throwable);
		}
	}

	Stream<DynamicNode> toDynamicTests(Stream<Description> children) {
		return children.map(description -> description.isTest() //
				? toDynamicTest(description, "child: " + description) //
				: dynamicContainer("class: " + description, Stream.concat( //
					Stream.of(toDynamicTest(description, "self")), //
					toDynamicTests(description.getChildren().stream()))));
	}

	private DynamicTest toDynamicTest(Description description, String displayName) {
		return dynamicTest(displayName,
			() -> assertEquals(description.getMethodName(), DescriptionUtils.getMethodName(description)));
	}
}
