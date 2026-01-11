/*
 * Copyright 2015-2026 the original author or authors.
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v2.0 which
 * accompanies this distribution and is available at
 *
 * https://www.eclipse.org/legal/epl-v20.html
 */

package standalone;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.DynamicContainer.dynamicContainer;
import static org.junit.jupiter.api.DynamicTest.dynamicTest;

import java.util.Arrays;
import java.util.stream.Stream;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DynamicNode;
import org.junit.jupiter.api.TestFactory;
import org.junit.jupiter.engine.descriptor.ClassBasedTestDescriptor;
import org.junit.jupiter.engine.descriptor.ClassTestDescriptor;
import org.junit.jupiter.engine.descriptor.JupiterTestDescriptor;
import org.junit.jupiter.engine.descriptor.MethodBasedTestDescriptor;
import org.junit.jupiter.engine.descriptor.NestedClassTestDescriptor;
import org.junit.jupiter.engine.descriptor.TestFactoryTestDescriptor;
import org.junit.jupiter.engine.descriptor.TestMethodTestDescriptor;
import org.junit.jupiter.engine.descriptor.TestTemplateInvocationTestDescriptor;
import org.junit.jupiter.engine.descriptor.TestTemplateTestDescriptor;

class ReflectionTestCase {

	@BeforeAll
	static void printJavaVersion() {
		System.out.println("Using Java version: " + System.getProperty("java.specification.version"));
	}

	@TestFactory
	Stream<DynamicNode> canReadParameters() {
		return Stream.of(JupiterTestDescriptor.class, ClassBasedTestDescriptor.class, ClassTestDescriptor.class,
			MethodBasedTestDescriptor.class, TestMethodTestDescriptor.class, TestTemplateTestDescriptor.class,
			TestTemplateInvocationTestDescriptor.class, TestFactoryTestDescriptor.class,
			NestedClassTestDescriptor.class) //
				.map(descriptorClass -> dynamicContainer(descriptorClass.getSimpleName(),
					Arrays.stream(descriptorClass.getDeclaredMethods()) //
							.map(method -> dynamicTest(method.getName(),
								() -> assertDoesNotThrow(method::getParameters)))));
	}
}
