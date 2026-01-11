/*
 * Copyright 2015-2026 the original author or authors.
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v2.0 which
 * accompanies this distribution and is available at
 *
 * https://www.eclipse.org/legal/epl-v20.html
 */

package org.junit.jupiter.engine.descriptor;

import static java.util.function.Predicate.isEqual;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.engine.descriptor.LifecycleMethodUtils.findAfterAllMethods;
import static org.junit.jupiter.engine.descriptor.LifecycleMethodUtils.findAfterEachMethods;
import static org.junit.jupiter.engine.descriptor.LifecycleMethodUtils.findBeforeAllMethods;
import static org.junit.jupiter.engine.descriptor.LifecycleMethodUtils.findBeforeEachMethods;
import static org.junit.platform.commons.util.FunctionUtils.where;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

import org.jspecify.annotations.NullUnmarked;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestInstance.Lifecycle;
import org.junit.platform.engine.DiscoveryIssue;
import org.junit.platform.engine.DiscoveryIssue.Severity;
import org.junit.platform.engine.support.descriptor.MethodSource;
import org.junit.platform.engine.support.discovery.DiscoveryIssueReporter;

/**
 * Unit tests for {@link LifecycleMethodUtils}.
 *
 * @since 5.0
 */
class LifecycleMethodUtilsTests {

	List<DiscoveryIssue> discoveryIssues = new ArrayList<>();
	DiscoveryIssueReporter issueReporter = DiscoveryIssueReporter.collecting(discoveryIssues);

	@Test
	void findNonVoidBeforeAllMethodsWithStandardLifecycle() throws Exception {
		var methods = findBeforeAllMethods(TestCaseWithInvalidLifecycleMethods.class, true, issueReporter);
		assertThat(methods).isEmpty();

		var methodSource = MethodSource.from(TestCaseWithInvalidLifecycleMethods.class.getDeclaredMethod("cc"));
		var notVoidIssue = DiscoveryIssue.builder(Severity.ERROR,
			"@BeforeAll method 'private java.lang.Double org.junit.jupiter.engine.descriptor.TestCaseWithInvalidLifecycleMethods.cc()' must not return a value.") //
				.source(methodSource) //
				.build();
		var notStaticIssue = DiscoveryIssue.builder(Severity.ERROR,
			"@BeforeAll method 'private java.lang.Double org.junit.jupiter.engine.descriptor.TestCaseWithInvalidLifecycleMethods.cc()' must be static unless the test class is annotated with @TestInstance(Lifecycle.PER_CLASS).") //
				.source(methodSource) //
				.build();
		var privateIssue = DiscoveryIssue.builder(Severity.WARNING,
			"@BeforeAll method 'private java.lang.Double org.junit.jupiter.engine.descriptor.TestCaseWithInvalidLifecycleMethods.cc()' should not be private. This will be disallowed in a future release.") //
				.source(methodSource) //
				.build();
		assertThat(discoveryIssues).containsExactlyInAnyOrder(notVoidIssue, notStaticIssue, privateIssue);
	}

	@Test
	void findNonVoidAfterAllMethodsWithStandardLifecycle() throws Exception {
		var methods = findAfterAllMethods(TestCaseWithInvalidLifecycleMethods.class, true, issueReporter);
		assertThat(methods).isEmpty();

		var methodSource = MethodSource.from(TestCaseWithInvalidLifecycleMethods.class.getDeclaredMethod("dd"));
		var notVoidIssue = DiscoveryIssue.builder(Severity.ERROR,
			"@AfterAll method 'private java.lang.String org.junit.jupiter.engine.descriptor.TestCaseWithInvalidLifecycleMethods.dd()' must not return a value.") //
				.source(methodSource) //
				.build();
		var notStaticIssue = DiscoveryIssue.builder(Severity.ERROR,
			"@AfterAll method 'private java.lang.String org.junit.jupiter.engine.descriptor.TestCaseWithInvalidLifecycleMethods.dd()' must be static unless the test class is annotated with @TestInstance(Lifecycle.PER_CLASS).") //
				.source(methodSource) //
				.build();
		var privateIssue = DiscoveryIssue.builder(Severity.WARNING,
			"@AfterAll method 'private java.lang.String org.junit.jupiter.engine.descriptor.TestCaseWithInvalidLifecycleMethods.dd()' should not be private. This will be disallowed in a future release.") //
				.source(methodSource) //
				.build();
		assertThat(discoveryIssues).containsExactlyInAnyOrder(notVoidIssue, notStaticIssue, privateIssue);
	}

	@Test
	void findNonVoidBeforeEachMethodsWithStandardLifecycle() throws Exception {
		var methods = findBeforeEachMethods(TestCaseWithInvalidLifecycleMethods.class, issueReporter);
		assertThat(methods).isEmpty();

		var methodSource = MethodSource.from(TestCaseWithInvalidLifecycleMethods.class.getDeclaredMethod("aa"));
		var notVoidIssue = DiscoveryIssue.builder(Severity.ERROR,
			"@BeforeEach method 'private java.lang.String org.junit.jupiter.engine.descriptor.TestCaseWithInvalidLifecycleMethods.aa()' must not return a value.") //
				.source(methodSource) //
				.build();
		var privateIssue = DiscoveryIssue.builder(Severity.WARNING,
			"@BeforeEach method 'private java.lang.String org.junit.jupiter.engine.descriptor.TestCaseWithInvalidLifecycleMethods.aa()' should not be private. This will be disallowed in a future release.") //
				.source(methodSource) //
				.build();
		assertThat(discoveryIssues).containsExactlyInAnyOrder(notVoidIssue, privateIssue);
	}

	@Test
	void findNonVoidAfterEachMethodsWithStandardLifecycle() throws Exception {
		var methods = findAfterEachMethods(TestCaseWithInvalidLifecycleMethods.class, issueReporter);
		assertThat(methods).isEmpty();

		var methodSource = MethodSource.from(TestCaseWithInvalidLifecycleMethods.class.getDeclaredMethod("bb"));
		var notVoidIssue = DiscoveryIssue.builder(Severity.ERROR,
			"@AfterEach method 'private int org.junit.jupiter.engine.descriptor.TestCaseWithInvalidLifecycleMethods.bb()' must not return a value.") //
				.source(methodSource) //
				.build();
		var privateIssue = DiscoveryIssue.builder(Severity.WARNING,
			"@AfterEach method 'private int org.junit.jupiter.engine.descriptor.TestCaseWithInvalidLifecycleMethods.bb()' should not be private. This will be disallowed in a future release.") //
				.source(methodSource) //
				.build();
		assertThat(discoveryIssues).containsExactlyInAnyOrder(notVoidIssue, privateIssue);
	}

	@Test
	void findBeforeEachMethodsWithStandardLifecycle() {
		List<Method> methods = findBeforeEachMethods(TestCaseWithStandardLifecycle.class, issueReporter);

		assertThat(namesOf(methods)).containsExactlyInAnyOrder("nine", "ten");
		assertThat(discoveryIssues).isEmpty();
	}

	@Test
	void findAfterEachMethodsWithStandardLifecycle() {
		List<Method> methods = findAfterEachMethods(TestCaseWithStandardLifecycle.class, issueReporter);

		assertThat(namesOf(methods)).containsExactlyInAnyOrder("eleven", "twelve");
	}

	@Test
	void findBeforeAllMethodsWithStandardLifecycleAndWithoutRequiringStatic() {
		List<Method> methods = findBeforeAllMethods(TestCaseWithStandardLifecycle.class, false, issueReporter);

		assertThat(namesOf(methods)).containsExactly("one");
		assertThat(discoveryIssues).isEmpty();
	}

	@Test
	void findBeforeAllMethodsWithStandardLifecycleAndRequiringStatic() throws Exception {
		var methods = findBeforeAllMethods(TestCaseWithStandardLifecycle.class, true, issueReporter);
		assertThat(methods).isEmpty();

		var expectedIssue = DiscoveryIssue.builder(Severity.ERROR,
			"@BeforeAll method 'void org.junit.jupiter.engine.descriptor.TestCaseWithStandardLifecycle.one()' must be static unless the test class is annotated with @TestInstance(Lifecycle.PER_CLASS).") //
				.source(MethodSource.from(TestCaseWithStandardLifecycle.class.getDeclaredMethod("one"))) //
				.build();
		assertThat(discoveryIssues).containsExactly(expectedIssue);
	}

	@Test
	void findBeforeAllMethodsWithLifeCyclePerClassAndRequiringStatic() {
		List<Method> methods = findBeforeAllMethods(TestCaseWithLifecyclePerClass.class, false, issueReporter);

		assertThat(namesOf(methods)).containsExactlyInAnyOrder("three", "four");
		assertThat(discoveryIssues).isEmpty();
	}

	@Test
	void findAfterAllMethodsWithStandardLifecycleAndWithoutRequiringStatic() {
		List<Method> methods = findAfterAllMethods(TestCaseWithStandardLifecycle.class, false, issueReporter);

		assertThat(namesOf(methods)).containsExactlyInAnyOrder("five", "six");
		assertThat(discoveryIssues).isEmpty();
	}

	@Test
	void findAfterAllMethodsWithStandardLifecycleAndRequiringStatic() {
		var methods = findAfterAllMethods(TestCaseWithStandardLifecycle.class, true, issueReporter);
		assertThat(methods).isEmpty();

		assertThat(discoveryIssues) //
				.filteredOn(where(DiscoveryIssue::severity, isEqual(Severity.ERROR))) //
				.isNotEmpty();
	}

	@Test
	void findAfterAllMethodsWithLifeCyclePerClassAndRequiringStatic() {
		List<Method> methods = findAfterAllMethods(TestCaseWithLifecyclePerClass.class, false, issueReporter);

		assertThat(namesOf(methods)).containsExactlyInAnyOrder("seven", "eight");
	}

	private static List<String> namesOf(List<Method> methods) {
		return methods.stream().map(Method::getName).toList();
	}

}

class TestCaseWithStandardLifecycle {

	@SuppressWarnings("JUnitMalformedDeclaration")
	@BeforeAll
	void one() {
	}

	@BeforeEach
	void nine() {
	}

	@BeforeEach
	void ten() {
	}

	@AfterEach
	void eleven() {
	}

	@AfterEach
	void twelve() {
	}

	@SuppressWarnings("JUnitMalformedDeclaration")
	@AfterAll
	void five() {
	}

	@SuppressWarnings("JUnitMalformedDeclaration")
	@AfterAll
	void six() {
	}

}

@TestInstance(Lifecycle.PER_CLASS)
class TestCaseWithLifecyclePerClass {

	@BeforeAll
	void three() {
	}

	@BeforeAll
	void four() {
	}

	@AfterAll
	void seven() {
	}

	@AfterAll
	void eight() {
	}

}

@SuppressWarnings("JUnitMalformedDeclaration")
@NullUnmarked
class TestCaseWithInvalidLifecycleMethods {

	@BeforeEach
	private String aa() {
		return null;
	}

	@AfterEach
	private int bb() {
		return 1;
	}

	@BeforeAll
	private Double cc() {
		return null;
	}

	@AfterAll
	private String dd() {
		return "";
	}

}
