/*
 * Copyright 2015-2025 the original author or authors.
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v2.0 which
 * accompanies this distribution and is available at
 *
 * https://www.eclipse.org/legal/epl-v20.html
 */

package org.junit.jupiter.engine.extension;

import static java.util.Comparator.comparing;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.DynamicTest.dynamicTest;
import static org.junit.jupiter.api.MethodOrderer.Random.RANDOM_SEED_PROPERTY_NAME;
import static org.junit.jupiter.api.Order.DEFAULT;
import static org.junit.jupiter.engine.Constants.DEFAULT_PARALLEL_EXECUTION_MODE;
import static org.junit.jupiter.engine.Constants.DEFAULT_TEST_METHOD_ORDER_PROPERTY_NAME;
import static org.junit.jupiter.engine.Constants.PARALLEL_EXECUTION_ENABLED_PROPERTY_NAME;
import static org.junit.platform.engine.discovery.DiscoverySelectors.selectClass;
import static org.junit.platform.launcher.LauncherConstants.CRITICAL_DISCOVERY_ISSUE_SEVERITY_PROPERTY_NAME;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.regex.Pattern;

import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.MethodDescriptor;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.MethodOrderer.MethodName;
import org.junit.jupiter.api.MethodOrderer.OrderAnnotation;
import org.junit.jupiter.api.MethodOrderer.Random;
import org.junit.jupiter.api.MethodOrdererContext;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.RepeatedTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestFactory;
import org.junit.jupiter.api.TestInfo;
import org.junit.jupiter.api.TestMethodOrder;
import org.junit.jupiter.api.TestReporter;
import org.junit.jupiter.api.fixtures.TrackLogRecords;
import org.junit.jupiter.engine.JupiterTestEngine;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.junit.platform.commons.logging.LogRecordListener;
import org.junit.platform.commons.util.ClassUtils;
import org.junit.platform.engine.DiscoveryIssue;
import org.junit.platform.engine.DiscoveryIssue.Severity;
import org.junit.platform.engine.support.descriptor.ClassSource;
import org.junit.platform.engine.support.descriptor.MethodSource;
import org.junit.platform.testkit.engine.EngineDiscoveryResults;
import org.junit.platform.testkit.engine.EngineTestKit;
import org.junit.platform.testkit.engine.Events;
import org.mockito.Mockito;

/**
 * Integration tests that verify support for custom test method execution order
 * in the {@link JupiterTestEngine}.
 *
 * @since 5.4
 */
class OrderedMethodTests {

	private static final Set<String> callSequence = Collections.synchronizedSet(new LinkedHashSet<>());
	private static final Set<String> threadNames = Collections.synchronizedSet(new LinkedHashSet<>());

	@BeforeEach
	void clearCallSequence() {
		callSequence.clear();
		threadNames.clear();
	}

	@Test
	void methodName() {
		Class<?> testClass = AMethodNameTestCase.class;

		// The name of the base class MUST start with a letter alphanumerically
		// greater than "A" so that BaseTestCase comes after AMethodNameTestCase
		// if methods are sorted by class name for the fallback ordering if two
		// methods have the same name but different parameter lists. Note, however,
		// that MethodName actually does not order methods like that, but we want
		// this check to remain in place to ensure that the ordering does not rely
		// on the class names.
		assertThat(testClass.getSuperclass().getName()).isGreaterThan(testClass.getName());

		var tests = executeTestsInParallel(testClass, Random.class);

		tests.assertStatistics(stats -> stats.succeeded(callSequence.size()));

		assertThat(callSequence).containsExactly("$()", "AAA()", "AAA(org.junit.jupiter.api.TestInfo)",
			"AAA(org.junit.jupiter.api.TestReporter)", "ZZ_Top()", "___()", "a1()", "a2()", "b()", "c()", "zzz()");
		assertThat(threadNames).hasSize(1);
	}

	@Test
	void displayName() {
		var tests = executeTestsInParallel(DisplayNameTestCase.class, Random.class);

		tests.assertStatistics(stats -> stats.succeeded(callSequence.size()));

		assertThat(callSequence)//
				.containsExactly("$", "AAA", "No_display_name_attribute_1_caps()", "No_display_name_attribute_2_caps()",
					"ZZ_Top", "___", "a1", "a2", "b()", "no_display_name_attribute_1()",
					"no_display_name_attribute_2()", "repetition 1 of 1", "⑦ϼ\uD83D\uDC69\u200D⚕\uD83E\uDDD8\u200D♂");
		assertThat(threadNames).hasSize(1);
	}

	@Test
	void orderAnnotation() {
		assertOrderAnnotationSupport(OrderAnnotationTestCase.class);
	}

	@Test
	void orderAnnotationInNestedTestClass() {
		assertOrderAnnotationSupport(OuterTestCase.class);
	}

	@Test
	void orderAnnotationWithNestedTestClass() {
		var tests = executeTestsInParallel(OrderAnnotationWithNestedClassTestCase.class, Random.class);

		tests.assertStatistics(stats -> stats.succeeded(callSequence.size()));

		assertThat(callSequence)//
				.containsExactly("test1", "test2", "test3", "test4", "test5", "test6", "test7", "test8", "nestedTest1",
					"nestedTest2");
		assertThat(threadNames).hasSize(1);
	}

	private void assertOrderAnnotationSupport(Class<?> testClass) {
		var tests = executeTestsInParallel(testClass, Random.class);

		tests.assertStatistics(stats -> stats.succeeded(callSequence.size()));

		assertThat(callSequence)//
				.containsExactly("test1", "test2", "test3", "test4", "test5", "test6", "test7", "test8");
		assertThat(threadNames).hasSize(1);
	}

	@Test
	void random() {
		var tests = executeTestsInParallel(RandomTestCase.class, Random.class);

		tests.assertStatistics(stats -> stats.succeeded(callSequence.size()));

		assertThat(threadNames).hasSize(1);
	}

	@ParameterizedTest
	@ValueSource(classes = { WithoutTestMethodOrderTestCase.class, ClassTemplateTestCase.class })
	void defaultOrderer(Class<?> testClass) {
		var tests = executeTestsInParallel(testClass, OrderAnnotation.class);

		tests.assertStatistics(stats -> stats.succeeded(callSequence.size()));

		assertThat(callSequence).containsExactly("test1()", "test2()", "test3()");
		assertThat(threadNames).hasSize(1);
	}

	@Test
	void randomWithBogusSeedRepeatedly(@TrackLogRecords LogRecordListener listener) {
		var seed = "explode";
		var expectedMessagePattern = Pattern.compile(
			"Failed to convert configuration parameter \\[" + Pattern.quote(Random.RANDOM_SEED_PROPERTY_NAME)
					+ "] with value \\[" + seed + "] to a long\\. Using default seed \\[\\d+] as fallback\\.");

		Set<String> uniqueSequences = new HashSet<>();

		for (var i = 0; i < 10; i++) {
			callSequence.clear();
			listener.clear();

			var tests = executeRandomTestCaseInParallelWithRandomSeed(seed);

			tests.assertStatistics(stats -> stats.succeeded(callSequence.size()));

			uniqueSequences.add(String.join(",", callSequence));

			// @formatter:off
			assertThat(listener.stream(Random.class, Level.WARNING)
					.map(LogRecord::getMessage))
					.anyMatch(expectedMessagePattern.asMatchPredicate());
			// @formatter:on
		}

		assertThat(uniqueSequences).size().isEqualTo(1);
	}

	@Test
	void randomWithDifferentSeedConsecutively(@TrackLogRecords LogRecordListener listener) {
		Set<String> uniqueSequences = new HashSet<>();

		for (var i = 0; i < 10; i++) {
			var seed = String.valueOf(i);
			var expectedMessage = "Using custom seed for configuration parameter [" + Random.RANDOM_SEED_PROPERTY_NAME
					+ "] with value [" + seed + "].";
			callSequence.clear();
			listener.clear();

			var tests = executeRandomTestCaseInParallelWithRandomSeed(seed);

			tests.assertStatistics(stats -> stats.succeeded(callSequence.size()));

			uniqueSequences.add(String.join(",", callSequence));

			// @formatter:off
			assertThat(listener.stream(Random.class, Level.CONFIG)
					.map(LogRecord::getMessage))
					.contains(expectedMessage);
			// @formatter:on

			assertThat(threadNames).hasSize(i + 1);
		}

		// We assume that at least 3 out of 10 are different...
		assertThat(uniqueSequences).size().isGreaterThanOrEqualTo(3);
	}

	@Test
	void randomWithCustomSeed(@TrackLogRecords LogRecordListener listener) {
		var seed = "42";
		var expectedMessage = "Using custom seed for configuration parameter [" + Random.RANDOM_SEED_PROPERTY_NAME
				+ "] with value [" + seed + "].";

		for (var i = 0; i < 10; i++) {
			callSequence.clear();
			listener.clear();

			var tests = executeRandomTestCaseInParallelWithRandomSeed(seed);

			tests.assertStatistics(stats -> stats.succeeded(callSequence.size()));

			// With a custom seed, the "randomness" must be the same for every iteration.
			assertThat(callSequence).containsExactly("test2()", "test3()", "test4()", "repetition 1 of 1", "test1()");

			// @formatter:off
			assertThat(listener.stream(Random.class, Level.CONFIG)
					.map(LogRecord::getMessage))
					.contains(expectedMessage);
			// @formatter:on
		}

		assertThat(threadNames).size().isGreaterThanOrEqualTo(3);
	}

	@Test
	void reportsDiscoveryIssuesForIneffectiveOrderAnnotations() throws Exception {
		var results = discoverTests(WithoutTestMethodOrderTestCase.class, OrderAnnotation.class);
		assertThat(results.getDiscoveryIssues()).isEmpty();

		results = discoverTests(WithoutTestMethodOrderTestCase.class, null);
		assertIneffectiveOrderAnnotationIssues(results.getDiscoveryIssues());

		results = discoverTests(WithoutTestMethodOrderTestCase.class, Random.class);
		assertIneffectiveOrderAnnotationIssues(results.getDiscoveryIssues());
	}

	@Test
	void misbehavingMethodOrdererThatAddsElements() {
		Class<?> testClass = MisbehavingByAddingTestCase.class;

		var discoveryIssues = discoverTests(testClass, null).getDiscoveryIssues();
		assertThat(discoveryIssues).hasSize(1);

		var issue = discoveryIssues.getFirst();
		assertThat(issue.severity()).isEqualTo(Severity.WARNING);
		assertThat(issue.message()).isEqualTo(
			"MethodOrderer [%s] added 2 MethodDescriptor(s) for test class [%s] which will be ignored.",
			MisbehavingByAdding.class.getName(), testClass.getName());
		assertThat(issue.source()).contains(ClassSource.from(testClass));

		executeTestsInParallel(testClass, null, Severity.ERROR) //
				.assertStatistics(stats -> stats.succeeded(2));

		assertThat(callSequence).containsExactly("test1()", "test2()");
	}

	@Test
	void misbehavingMethodOrdererThatImpersonatesElements() {
		Class<?> testClass = MisbehavingByImpersonatingTestCase.class;

		executeTestsInParallel(testClass, Random.class).assertStatistics(stats -> stats.succeeded(2));

		assertThat(callSequence).containsExactlyInAnyOrder("test1()", "test2()");
	}

	@Test
	void misbehavingMethodOrdererThatRemovesElements() {
		Class<?> testClass = MisbehavingByRemovingTestCase.class;

		var discoveryIssues = discoverTests(testClass, null).getDiscoveryIssues();
		assertThat(discoveryIssues).hasSize(1);

		var issue = discoveryIssues.getFirst();
		assertThat(issue.severity()).isEqualTo(Severity.WARNING);
		assertThat(issue.message()).isEqualTo(
			"MethodOrderer [%s] removed 2 MethodDescriptor(s) for test class [%s] which will be retained with arbitrary ordering.",
			MisbehavingByRemoving.class.getName(), testClass.getName());
		assertThat(issue.source()).contains(ClassSource.from(testClass));

		executeTestsInParallel(testClass, null, Severity.ERROR) //
				.assertStatistics(stats -> stats.succeeded(4));

		assertThat(callSequence) //
				.containsExactlyInAnyOrder("test1()", "test2()", "test3()", "test4()") //
				.containsSubsequence("test3()", "test4()") // ordered in MisbehavingByRemoving
				.containsSubsequence("test1()", "test3()") // removed item is re-added before ordered item
				.containsSubsequence("test1()", "test4()") // removed item is re-added before ordered item
				.containsSubsequence("test2()", "test3()") // removed item is re-added before ordered item
				.containsSubsequence("test2()", "test4()");// removed item is re-added before ordered item
	}

	private EngineDiscoveryResults discoverTests(Class<?> testClass,
			@Nullable Class<? extends MethodOrderer> defaultOrderer) {
		return testKit(testClass, defaultOrderer, Severity.INFO).discover();
	}

	private Events executeTestsInParallel(Class<?> testClass, @Nullable Class<? extends MethodOrderer> defaultOrderer) {
		return executeTestsInParallel(testClass, defaultOrderer, Severity.INFO);
	}

	private Events executeTestsInParallel(Class<?> testClass, @Nullable Class<? extends MethodOrderer> defaultOrderer,
			Severity criticalSeverity) {
		return testKit(testClass, defaultOrderer, criticalSeverity) //
				.execute() //
				.testEvents();
	}

	private static EngineTestKit.Builder testKit(Class<?> testClass,
			@Nullable Class<? extends MethodOrderer> defaultOrderer, Severity criticalSeverity) {
		var testKit = EngineTestKit.engine("junit-jupiter") //
				.configurationParameter(PARALLEL_EXECUTION_ENABLED_PROPERTY_NAME, "true") //
				.configurationParameter(DEFAULT_PARALLEL_EXECUTION_MODE, "concurrent") //
				.configurationParameter(CRITICAL_DISCOVERY_ISSUE_SEVERITY_PROPERTY_NAME, criticalSeverity.name());
		if (defaultOrderer != null) {
			testKit.configurationParameter(DEFAULT_TEST_METHOD_ORDER_PROPERTY_NAME, defaultOrderer.getName());
		}
		return testKit.selectors(selectClass(testClass));
	}

	private Events executeRandomTestCaseInParallelWithRandomSeed(String seed) {
		var configurationParameters = Map.of(//
			PARALLEL_EXECUTION_ENABLED_PROPERTY_NAME, "true", //
			DEFAULT_PARALLEL_EXECUTION_MODE, "concurrent", //
			RANDOM_SEED_PROPERTY_NAME, seed //
		);

		// @formatter:off
		return EngineTestKit
				.engine("junit-jupiter")
				.configurationParameters(configurationParameters)
				.selectors(selectClass(RandomTestCase.class))
				.execute()
				.testEvents();
		// @formatter:on
	}

	private static void assertIneffectiveOrderAnnotationIssues(List<DiscoveryIssue> discoveryIssues) throws Exception {
		assertThat(discoveryIssues).hasSize(3);
		assertThat(discoveryIssues).extracting(DiscoveryIssue::severity).containsOnly(Severity.INFO);
		assertThat(discoveryIssues).extracting(DiscoveryIssue::message) //
				.allMatch(it -> it.startsWith("Ineffective @Order annotation on method")
						&& it.contains("It will not be applied because MethodOrderer.OrderAnnotation is not in use.")
						&& it.endsWith(
							"Note that the annotation may be either directly present or meta-present on the method."));
		var testClass = WithoutTestMethodOrderTestCase.class;
		assertThat(discoveryIssues).extracting(DiscoveryIssue::source).extracting(Optional::orElseThrow) //
				.containsExactlyInAnyOrder(MethodSource.from(testClass.getDeclaredMethod("test1")),
					MethodSource.from(testClass.getDeclaredMethod("test2")),
					MethodSource.from(testClass.getDeclaredMethod("test3")));
	}

	// -------------------------------------------------------------------------

	@SuppressWarnings("JUnitMalformedDeclaration")
	static class BaseTestCase {

		@Test
		void AAA() {
		}

		@Test
		void c() {
		}

	}

	@SuppressWarnings({ "unused", "JUnitMalformedDeclaration" })
	@TestMethodOrder(MethodName.class)
	static class AMethodNameTestCase extends BaseTestCase {

		@BeforeEach
		void trackInvocations(TestInfo testInfo) {
			var method = testInfo.getTestMethod().orElseThrow(AssertionError::new);
			var signature = "%s(%s)".formatted(method.getName(),
				ClassUtils.nullSafeToString(method.getParameterTypes()));

			callSequence.add(signature);
			threadNames.add(Thread.currentThread().getName());
		}

		@TestFactory
		DynamicTest b() {
			return dynamicTest("dynamic", () -> {
			});
		}

		@Test
		void $() {
		}

		@Test
		void ___() {
		}

		@Test
		void AAA(TestReporter testReporter) {
		}

		@Test
		void AAA(TestInfo testInfo) {
		}

		@Test
		void ZZ_Top() {
		}

		@Test
		void a1() {
		}

		@Test
		void a2() {
		}

		@RepeatedTest(1)
		void zzz() {
		}
	}

	@SuppressWarnings("JUnitMalformedDeclaration")
	@TestMethodOrder(MethodOrderer.DisplayName.class)
	static class DisplayNameTestCase {

		@BeforeEach
		void trackInvocations(TestInfo testInfo) {
			callSequence.add(testInfo.getDisplayName());
			threadNames.add(Thread.currentThread().getName());
		}

		@TestFactory
		DynamicTest b() {
			return dynamicTest("dynamic", () -> {
			});
		}

		@DisplayName("$")
		@Test
		void $() {
		}

		@DisplayName("___")
		@Test
		void ___() {
		}

		@DisplayName("AAA")
		@Test
		void AAA() {
		}

		@DisplayName("ZZ_Top")
		@Test
		void ZZ_Top() {
		}

		@DisplayName("a1")
		@Test
		void a1() {
		}

		@DisplayName("a2")
		@Test
		void a2() {
		}

		@DisplayName("zzz")
		@RepeatedTest(1)
		void zzz() {
		}

		@Test
		@DisplayName("⑦ϼ\uD83D\uDC69\u200D⚕\uD83E\uDDD8\u200D♂")
		void special_characters() {
		}

		@Test
		void no_display_name_attribute_1() {
		}

		@Test
		void no_display_name_attribute_2() {
		}

		@Test
		void No_display_name_attribute_1_caps() {
		}

		@Test
		void No_display_name_attribute_2_caps() {
		}
	}

	@SuppressWarnings("JUnitMalformedDeclaration")
	@TestMethodOrder(OrderAnnotation.class)
	static class OrderAnnotationTestCase {

		@BeforeEach
		void trackInvocations(TestInfo testInfo) {
			callSequence.add(testInfo.getDisplayName());
			threadNames.add(Thread.currentThread().getName());
		}

		@Test
		@DisplayName("test8")
		@Order(Integer.MAX_VALUE)
		void maxInteger() {
		}

		@Test
		@DisplayName("test7")
		@Order(DEFAULT + 1)
		void defaultOrderValuePlusOne() {
		}

		@Test
		@DisplayName("test6")
		// @Order(DEFAULT)
		void defaultOrderValue() {
		}

		@Test
		@DisplayName("test3")
		@Order(3)
		void $() {
		}

		@Test
		@DisplayName("test5")
		@Order(5)
		void AAA() {
		}

		@TestFactory
		@DisplayName("test4")
		@Order(4)
		DynamicTest aaa() {
			return dynamicTest("test4", () -> {
			});
		}

		@Test
		@DisplayName("test1")
		@Order(1)
		void zzz() {
		}

		@RepeatedTest(value = 1, name = "{displayName}")
		@DisplayName("test2")
		@Order(2)
		void ___() {
		}
	}

	static class OuterTestCase {

		@Nested
		class InnerTestCase {

			@Nested
			class NestedOrderAnnotationTestCase extends OrderAnnotationTestCase {
			}
		}
	}

	@SuppressWarnings("JUnitMalformedDeclaration")
	@TestMethodOrder(Random.class)
	static class RandomTestCase {

		@BeforeEach
		void trackInvocations(TestInfo testInfo) {
			callSequence.add(testInfo.getDisplayName());
			threadNames.add(Thread.currentThread().getName());
		}

		@Test
		void test1() {
		}

		@Test
		void test2() {
		}

		@Test
		void test3() {
		}

		@TestFactory
		DynamicTest test4() {
			return dynamicTest("dynamic", () -> {
			});
		}

		@RepeatedTest(1)
		void test5() {
		}
	}

	@SuppressWarnings("JUnitMalformedDeclaration")
	@TestMethodOrder(MisbehavingByAdding.class)
	static class MisbehavingByAddingTestCase {

		@BeforeEach
		void trackInvocations(TestInfo testInfo) {
			callSequence.add(testInfo.getDisplayName());
		}

		@Test
		void test2() {
		}

		@Test
		void test1() {
		}
	}

	@SuppressWarnings("JUnitMalformedDeclaration")
	@TestMethodOrder(MisbehavingByImpersonating.class)
	static class MisbehavingByImpersonatingTestCase {

		@BeforeEach
		void trackInvocations(TestInfo testInfo) {
			callSequence.add(testInfo.getDisplayName());
		}

		@Test
		void test2() {
		}

		@Test
		void test1() {
		}
	}

	@SuppressWarnings("JUnitMalformedDeclaration")
	@TestMethodOrder(MisbehavingByRemoving.class)
	static class MisbehavingByRemovingTestCase {

		@BeforeEach
		void trackInvocations(TestInfo testInfo) {
			callSequence.add(testInfo.getDisplayName());
		}

		@Test
		void test1() {
		}

		@Test
		void test4() {
		}

		@Test
		void test2() {
		}

		@Test
		void test3() {
		}
	}

	static class OrderAnnotationWithNestedClassTestCase extends OrderAnnotationTestCase {
		@Nested
		@TestMethodOrder(OrderAnnotation.class)
		class NestedTests {

			@BeforeEach
			void trackInvocations(TestInfo testInfo) {
				callSequence.add(testInfo.getDisplayName());
			}

			@Test
			@Order(1)
			@DisplayName("nestedTest1")
			void nestedTest1() {
			}

			@Test
			@Order(2)
			@DisplayName("nestedTest2")
			void nestedTest2() {
			}
		}
	}

	static class MisbehavingByAdding implements MethodOrderer {

		@Override
		public void orderMethods(MethodOrdererContext context) {
			context.getMethodDescriptors().sort(comparing(MethodDescriptor::getDisplayName));
			context.getMethodDescriptors().add(mockMethodDescriptor());
			context.getMethodDescriptors().add(mockMethodDescriptor());
		}

		@SuppressWarnings("unchecked")
		static <T> T mockMethodDescriptor() {
			return (T) Mockito.mock((Class<? super T>) MethodDescriptor.class);
		}

	}

	static class MisbehavingByImpersonating implements MethodOrderer {

		@Override
		public void orderMethods(MethodOrdererContext context) {
			context.getMethodDescriptors().sort(comparing(MethodDescriptor::getDisplayName));
			MethodDescriptor method1 = context.getMethodDescriptors().get(0);
			MethodDescriptor method2 = context.getMethodDescriptors().get(1);

			context.getMethodDescriptors().set(0, createMethodDescriptorImpersonator(method1));
			context.getMethodDescriptors().set(1, createMethodDescriptorImpersonator(method2));
		}

		@SuppressWarnings("unchecked")
		static <T> T createMethodDescriptorImpersonator(MethodDescriptor method) {
			MethodDescriptor stub = new MethodDescriptor() {
				@Override
				public Method getMethod() {
					throw new UnsupportedOperationException();
				}

				@Override
				public String getDisplayName() {
					throw new UnsupportedOperationException();
				}

				@Override
				public boolean isAnnotated(Class<? extends Annotation> annotationType) {
					return false;
				}

				@Override
				public <A extends Annotation> Optional<A> findAnnotation(Class<A> annotationType) {
					return Optional.empty();
				}

				@Override
				public <A extends Annotation> List<A> findRepeatableAnnotations(Class<A> annotationType) {
					return List.of();
				}

				@SuppressWarnings("EqualsWhichDoesntCheckParameterClass")
				@Override
				public boolean equals(Object obj) {
					return method.equals(obj);
				}

				@Override
				public int hashCode() {
					return method.hashCode();
				}
			};
			return (T) stub;
		}
	}

	static class MisbehavingByRemoving implements MethodOrderer {

		@Override
		public void orderMethods(MethodOrdererContext context) {
			context.getMethodDescriptors().sort(comparing(MethodDescriptor::getDisplayName));
			context.getMethodDescriptors().removeFirst();
			context.getMethodDescriptors().removeFirst();
		}
	}

	@SuppressWarnings("JUnitMalformedDeclaration")
	static class WithoutTestMethodOrderTestCase {

		@BeforeEach
		void trackInvocations(TestInfo testInfo) {
			callSequence.add(testInfo.getDisplayName());
			threadNames.add(Thread.currentThread().getName());
		}

		@Test
		@Order(2)
		void test2() {
		}

		@Test
		@Order(3)
		void test3() {
		}

		@Test
		@Order(1)
		void test1() {
		}

	}

	static class ClassTemplateTestCase extends WithoutTestMethodOrderTestCase {
	}

}
