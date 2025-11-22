/*
 * Copyright 2015-2025 the original author or authors.
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v2.0 which
 * accompanies this distribution and is available at
 *
 * https://www.eclipse.org/legal/epl-v20.html
 */

package org.junit.jupiter.testkit;

import static java.util.Arrays.stream;
import static java.util.stream.Collectors.joining;

import java.util.List;
import java.util.Map;

import org.opentest4j.TestAbortedException;

public class JUnitJupiterTestKit {

	/**
	 * Returns the execution results of the given test class.
	 *
	 * @param testClass The test class instance
	 * @return The execution results
	 */
	public static ExecutionResults executeTestClass(Class<?> testClass) {
		return ExecutionResults.builder().selectTestClass(testClass).execute();
	}

	/**
	 * Returns the execution results of the given test classes.
	 *
	 * @param testClasses The collection of test class instances
	 * @return The execution results
	 */
	public static ExecutionResults executeTestClasses(Iterable<Class<?>> testClasses) {
		return ExecutionResults.builder().selectTestClasses(testClasses).execute();
	}

	/**
	 * Returns the execution results of the given method of a given test class.
	 *
	 * @param testClass The test class instance
	 * @param testMethodName Name of the test method (of the given class)
	 * @return The execution results
	 */
	public static ExecutionResults executeTestMethod(Class<?> testClass, String testMethodName) {
		return ExecutionResults.builder().selectTestMethod(testClass, testMethodName).execute();
	}

	/**
	 * Returns the execution results of the given method of a given test class.
	 *
	 * @param testClass The test class instance
	 * @param testMethodName Name of the test method (of the given class)
	 * @param methodParameterTypes Class type(s) of the parameter(s)
	 * @return The execution results
	 * @throws IllegalArgumentException when methodParameterTypes is null
	 * 			This method only checks parameters which are not part of the underlying
	 * 			Jupiter TestKit. The Jupiter TestKit may throw other exceptions!
	 */
	public static ExecutionResults executeTestMethodWithParameterTypes(Class<?> testClass, String testMethodName,
			Class<?>... methodParameterTypes) {

		String allTypeNames = toMethodParameterTypesString(methodParameterTypes);

		return ExecutionResults.builder().selectTestMethodWithParameterTypes(testClass, testMethodName,
			allTypeNames).execute();
	}

	/**
	 * Returns the execution results of the given nested test class.
	 *
	 * @param enclosingClasses List of the enclosing classes
	 * @param testClass Name of the test class, the results should be returned
	 * @return The execution results
	 */
	public static ExecutionResults executeNestedTestClass(List<Class<?>> enclosingClasses, Class<?> testClass) {
		return ExecutionResults.builder().selectNestedTestClass(enclosingClasses, testClass).execute();
	}

	/**
	 * Returns the execution results of the given method of a given nested test class.
	 *
	 * @param enclosingClasses List of the enclosing classes
	 * @param testClass Name of the test class
	 * @param testMethodName Name of the test method (of the given class)
	 * @return The execution results
	 */
	public static ExecutionResults executeNestedTestMethod(List<Class<?>> enclosingClasses, Class<?> testClass,
			String testMethodName) {
		return ExecutionResults.builder().selectNestedTestMethod(enclosingClasses, testClass, testMethodName).execute();
	}

	/**
	 * Returns the execution results of the given method of a given nested test class.
	 *
	 * @param enclosingClasses List of the enclosing classes
	 * @param testClass Name of the test class
	 * @param testMethodName Name of the test method (of the given class)
	 * @param methodParameterTypes Class type(s) of the parameter(s)
	 * @return The execution results
	 * @throws IllegalArgumentException when methodParameterTypes is null
	 *			This method only checks parameters which are not part of the underlying
	 *			Jupiter TestKit. The Jupiter TestKit may throw other exceptions!
	 */
	public static ExecutionResults executeNestedTestMethodWithParameterTypes(List<Class<?>> enclosingClasses,
			Class<?> testClass, String testMethodName, Class<?>... methodParameterTypes) {

		String allTypeNames = toMethodParameterTypesString(methodParameterTypes);

		return ExecutionResults.builder().selectNestedTestMethodWithParameterTypes(enclosingClasses, testClass,
			testMethodName, allTypeNames).execute();
	}

	/**
	 * Returns the execution results of the given method of a given test class
	 * and passes the additional configuration parameters.
	 *
	 * @param configurationParameters additional configuration parameters
	 * @param testClass The test class instance
	 * @param testMethodName Name of the test method (of the given class)
	 * @param methodParameterTypes Class type(s) of the parameter(s)
	 * @return The execution results
	 * @throws IllegalArgumentException when methodParameterTypes is null
	 * 			This method only checks parameters which are not part of the underlying
	 * 			Jupiter TestKit. The Jupiter TestKit may throw other exceptions!
	 */
	public static ExecutionResults executeTestMethodWithParameterTypesAndConfigurationParameters(
			Map<String, String> configurationParameters, Class<?> testClass, String testMethodName,
			Class<?>... methodParameterTypes) {

		String allTypeNames = toMethodParameterTypesString(methodParameterTypes);

		return ExecutionResults.builder().addConfigurationParameters(
			configurationParameters).selectTestMethodWithParameterTypes(testClass, testMethodName,
				allTypeNames).execute();
	}

	private static String toMethodParameterTypesString(Class<?>... methodParameterTypes) {
		// throw IllegalArgumentException for a `null` array instead of NPE
		// (hence no use of `Objects::requireNonNull`)
		if (methodParameterTypes == null) {
			throw new IllegalArgumentException("methodParameterTypes must not be null");
		}

		// Concatenating all type names, because DiscoverySelectors.selectMethod only takes String as a parameter.
		return stream(methodParameterTypes).map(Class::getName).collect(joining(","));
	}

	/**
	 * Aborts the test execution. Makes the test code a little nicer.
	 *
	 * @throws TestAbortedException always throws this
	 */
	public static void abort() {
		throw new TestAbortedException();
	}

}
