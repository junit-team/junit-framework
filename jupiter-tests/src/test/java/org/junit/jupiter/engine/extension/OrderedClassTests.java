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

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.engine.Constants.DEFAULT_TEST_CLASS_ORDER_PROPERTY_NAME;
import static org.junit.platform.engine.discovery.DiscoverySelectors.selectClass;
import static org.junit.platform.engine.discovery.DiscoverySelectors.selectClasses;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.stream.Stream;

import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.ClassOrderer;
import org.junit.jupiter.api.ClassTemplate;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestClassOrder;
import org.junit.jupiter.api.TestInfo;
import org.junit.jupiter.api.extension.ClassTemplateInvocationContext;
import org.junit.jupiter.api.extension.ClassTemplateInvocationContextProvider;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.fixtures.TrackLogRecords;
import org.junit.platform.commons.logging.LogRecordListener;
import org.junit.platform.engine.DiscoveryIssue;
import org.junit.platform.engine.DiscoveryIssue.Severity;
import org.junit.platform.engine.DiscoverySelector;
import org.junit.platform.engine.support.descriptor.ClassSource;
import org.junit.platform.testkit.engine.EngineDiscoveryResults;
import org.junit.platform.testkit.engine.EngineTestKit;
import org.junit.platform.testkit.engine.Events;

/**
 * Integration tests for {@link ClassOrderer} support.
 *
 * @since 5.8
 */
class OrderedClassTests {

	private static final List<String> callSequence = Collections.synchronizedList(new ArrayList<>());

	@BeforeEach
	@AfterEach
	void clearCallSequence() {
		callSequence.clear();
	}

	@Test
	void noOrderer() {
		var discoveryIssues = discoverTests(null).getDiscoveryIssues();
		assertIneffectiveOrderAnnotationIssues(discoveryIssues);

		executeTests(null)//
				.assertStatistics(stats -> stats.succeeded(callSequence.size()));

		assertThat(callSequence)//
				.containsExactlyInAnyOrder("A_TestCase", "B_TestCase", "C_TestCase");
	}

	@Test
	void className() {
		var discoveryIssues = discoverTests(ClassOrderer.ClassName.class).getDiscoveryIssues();
		assertIneffectiveOrderAnnotationIssues(discoveryIssues);

		executeTests(ClassOrderer.ClassName.class)//
				.assertStatistics(stats -> stats.succeeded(callSequence.size()));

		assertThat(callSequence)//
				.containsExactly("A_TestCase", "B_TestCase", "C_TestCase");
	}

	@Test
	void classNameAcrossPackages() {
		try {
			example.B_TestCase.callSequence = callSequence;

			// @formatter:off
			executeTests(ClassOrderer.ClassName.class, selectClasses(B_TestCase.class, example.B_TestCase.class))
					.assertStatistics(stats -> stats.succeeded(callSequence.size()));
			// @formatter:on

			assertThat(callSequence)//
					.containsExactly("example.B_TestCase", "B_TestCase");
		}
		finally {
			example.B_TestCase.callSequence = null;
		}
	}

	@Test
	void displayName() {
		var discoveryIssues = discoverTests(ClassOrderer.DisplayName.class).getDiscoveryIssues();
		assertIneffectiveOrderAnnotationIssues(discoveryIssues);

		executeTests(ClassOrderer.DisplayName.class)//
				.assertStatistics(stats -> stats.succeeded(callSequence.size()));

		assertThat(callSequence)//
				.containsExactly("C_TestCase", "B_TestCase", "A_TestCase");
	}

	@Test
	void orderAnnotation() {
		var discoveryIssues = discoverTests(ClassOrderer.OrderAnnotation.class).getDiscoveryIssues();
		assertThat(discoveryIssues).isEmpty();

		executeTests(ClassOrderer.OrderAnnotation.class)//
				.assertStatistics(stats -> stats.succeeded(callSequence.size()));

		assertThat(callSequence)//
				.containsExactly("A_TestCase", "C_TestCase", "B_TestCase");
	}

	@Test
	void orderAnnotationOnNestedTestClassesWithGlobalConfig() {
		executeTests(ClassOrderer.OrderAnnotation.class, selectClass(OuterWithGlobalConfig.class))//
				.assertStatistics(stats -> stats.succeeded(callSequence.size()));

		assertThat(callSequence)//
				.containsExactly("Inner2", "Inner1", "Inner0", "Inner3");
	}

	@Test
	void orderAnnotationOnNestedTestClassesWithLocalConfig(@TrackLogRecords LogRecordListener listener) {
		executeTests(ClassOrderer.class, selectClass(OuterWithLocalConfig.class))//
				.assertStatistics(stats -> stats.succeeded(callSequence.size()));

		// Ensure that supplying the ClassOrderer interface instead of an implementation
		// class results in a WARNING log message. This also lets us know the local
		// config is used.
		assertTrue(listener.stream(Level.WARNING)//
				.map(LogRecord::getMessage)//
				.anyMatch(m -> m.startsWith(
					"Failed to load default class orderer class 'org.junit.jupiter.api.ClassOrderer'")));

		assertThat(callSequence)//
				.containsExactly("Inner2", "Inner1", "Inner1Inner1", "Inner1Inner0", "Inner0", "Inner3");
	}

	@Test
	void random() {
		var discoveryIssues = discoverTests(ClassOrderer.Random.class).getDiscoveryIssues();
		assertIneffectiveOrderAnnotationIssues(discoveryIssues);

		executeTests(ClassOrderer.Random.class)//
				.assertStatistics(stats -> stats.succeeded(callSequence.size()));
	}

	@Test
	void classTemplateWithLocalConfig() {
		var classTemplate = ClassTemplateWithLocalConfigTestCase.class;
		var inner0 = ClassTemplateWithLocalConfigTestCase.Inner0.class;
		var inner1 = ClassTemplateWithLocalConfigTestCase.Inner1.class;
		var inner1Inner1 = ClassTemplateWithLocalConfigTestCase.Inner1.Inner1Inner1.class;
		var inner1Inner0 = ClassTemplateWithLocalConfigTestCase.Inner1.Inner1Inner0.class;

		executeTests(ClassOrderer.Random.class, selectClass(classTemplate))//
				.assertStatistics(stats -> stats.succeeded(callSequence.size()));

		var inner1InvocationCallSequence = Stream.of(inner1, inner1Inner1, inner1Inner0, inner1Inner0).toList();
		var inner1CallSequence = twice(inner1InvocationCallSequence).toList();
		var outerCallSequence = Stream.concat(Stream.of(classTemplate),
			Stream.concat(inner1CallSequence.stream(), Stream.of(inner0))).toList();
		var expectedCallSequence = twice(outerCallSequence).map(Class::getSimpleName).toList();

		assertThat(callSequence).containsExactlyElementsOf(expectedCallSequence);
	}

	private static <T> Stream<T> twice(List<T> values) {
		return Stream.concat(values.stream(), values.stream());
	}

	@Test
	void classTemplateWithGlobalConfig() {
		var classTemplate = ClassTemplateWithLocalConfigTestCase.class;
		var otherClass = A_TestCase.class;

		executeTests(ClassOrderer.OrderAnnotation.class, selectClasses(otherClass, classTemplate))//
				.assertStatistics(stats -> stats.succeeded(callSequence.size()));

		assertThat(callSequence)//
				.containsSubsequence(classTemplate.getSimpleName(), otherClass.getSimpleName());
	}

	@Test
	void nestedClassedCanUseDefaultOrder(@TrackLogRecords LogRecordListener logRecords) {
		executeTests(null, selectClass(RevertingBackToDefaultOrderTestCase.Inner.class));
		assertThat(callSequence).containsExactly("Test1", "Test2", "Test3", "Test4");
		callSequence.clear();

		executeTests(ClassOrderer.OrderAnnotation.class, selectClass(RevertingBackToDefaultOrderTestCase.Inner.class));
		assertThat(callSequence).containsExactly("Test4", "Test2", "Test1", "Test3");
		callSequence.clear();

		executeTests(ClassOrderer.Default.class, selectClass(RevertingBackToDefaultOrderTestCase.Inner.class));
		assertThat(callSequence).containsExactly("Test1", "Test2", "Test3", "Test4");
		assertThat(logRecords.stream()) //
				.filteredOn(it -> it.getLevel().intValue() >= Level.WARNING.intValue()) //
				.map(LogRecord::getMessage) //
				.isEmpty();
	}

	private static void assertIneffectiveOrderAnnotationIssues(List<DiscoveryIssue> discoveryIssues) {
		assertThat(discoveryIssues).hasSize(2);
		assertThat(discoveryIssues).extracting(DiscoveryIssue::severity).containsOnly(Severity.INFO);
		assertThat(discoveryIssues).extracting(DiscoveryIssue::message) //
				.allMatch(it -> it.startsWith("Ineffective @Order annotation on class")
						&& it.contains("It will not be applied because ClassOrderer.OrderAnnotation is not in use.")
						&& it.endsWith(
							"Note that the annotation may be either directly present or meta-present on the class."));
		assertThat(discoveryIssues).extracting(DiscoveryIssue::source).extracting(Optional::orElseThrow) //
				.containsExactlyInAnyOrder(ClassSource.from(A_TestCase.class), ClassSource.from(C_TestCase.class));
	}

	private Events executeTests(@Nullable Class<? extends ClassOrderer> classOrderer) {
		return executeTests(classOrderer, selectClasses(A_TestCase.class, B_TestCase.class, C_TestCase.class));
	}

	private Events executeTests(@Nullable Class<? extends ClassOrderer> classOrderer, DiscoverySelector... selectors) {
		return executeTests(classOrderer, List.of(selectors));
	}

	private Events executeTests(@Nullable Class<? extends ClassOrderer> classOrderer,
			List<? extends DiscoverySelector> selectors) {
		// @formatter:off
		return testKit(classOrderer, selectors)
				.execute()
				.testEvents();
		// @formatter:on
	}

	private EngineDiscoveryResults discoverTests(@Nullable Class<? extends ClassOrderer> classOrderer) {
		return discoverTests(classOrderer, selectClasses(A_TestCase.class, B_TestCase.class, C_TestCase.class));
	}

	private EngineDiscoveryResults discoverTests(@Nullable Class<? extends ClassOrderer> classOrderer,
			List<? extends DiscoverySelector> selectors) {
		return testKit(classOrderer, selectors).discover();
	}

	private static EngineTestKit.Builder testKit(@Nullable Class<? extends ClassOrderer> classOrderer,
			List<? extends DiscoverySelector> selectors) {

		var testKit = EngineTestKit.engine("junit-jupiter");
		if (classOrderer != null) {
			testKit.configurationParameter(DEFAULT_TEST_CLASS_ORDER_PROPERTY_NAME, classOrderer.getName());
		}
		return testKit.selectors(selectors);
	}

	static abstract class BaseTestCase {

		@BeforeEach
		void trackInvocations(TestInfo testInfo) {
			var testClass = testInfo.getTestClass().orElseThrow();

			callSequence.add(testClass.getSimpleName());
		}

		@Test
		void a() {
		}
	}

	@Order(2)
	@DisplayName("Z")
	static class A_TestCase extends BaseTestCase {
	}

	static class B_TestCase extends BaseTestCase {
	}

	@Order(10)
	@DisplayName("A")
	static class C_TestCase extends BaseTestCase {
	}

	static class OuterWithGlobalConfig {

		@Nested
		class Inner0 {
			@Test
			void test() {
				callSequence.add(getClass().getSimpleName());
			}
		}

		@Nested
		@Order(2)
		class Inner1 {
			@Test
			void test() {
				callSequence.add(getClass().getSimpleName());
			}
		}

		@Nested
		@Order(1)
		class Inner2 {
			@Test
			void test() {
				callSequence.add(getClass().getSimpleName());
			}
		}

		@Nested
		@Order(Integer.MAX_VALUE)
		class Inner3 {
			@Test
			void test() {
				callSequence.add(getClass().getSimpleName());
			}
		}
	}

	@TestClassOrder(ClassOrderer.OrderAnnotation.class)
	static class OuterWithLocalConfig {

		@Nested
		class Inner0 {
			@Test
			void test() {
				callSequence.add(getClass().getSimpleName());
			}
		}

		@Nested
		@Order(2)
		class Inner1 {

			@Test
			void test() {
				callSequence.add(getClass().getSimpleName());
			}

			@Nested
			@Order(2)
			class Inner1Inner0 {
				@Test
				void test() {
					callSequence.add(getClass().getSimpleName());
				}
			}

			@Nested
			@Order(1)
			class Inner1Inner1 {
				@Test
				void test() {
					callSequence.add(getClass().getSimpleName());
				}
			}
		}

		@Nested
		@Order(1)
		class Inner2 {
			@Test
			void test() {
				callSequence.add(getClass().getSimpleName());
			}
		}

		@Nested
		@Order(Integer.MAX_VALUE)
		class Inner3 {
			@Test
			void test() {
				callSequence.add(getClass().getSimpleName());
			}
		}
	}

	@SuppressWarnings("JUnitMalformedDeclaration")
	@Order(1)
	@TestClassOrder(ClassOrderer.OrderAnnotation.class)
	@ClassTemplate
	@ExtendWith(ClassTemplateWithLocalConfigTestCase.Twice.class)
	static class ClassTemplateWithLocalConfigTestCase {

		@Test
		void test() {
			callSequence.add(ClassTemplateWithLocalConfigTestCase.class.getSimpleName());
		}

		@Nested
		@Order(1)
		class Inner0 {
			@Test
			void test() {
				callSequence.add(getClass().getSimpleName());
			}
		}

		@Nested
		@ClassTemplate
		@Order(0)
		class Inner1 {

			@Test
			void test() {
				callSequence.add(getClass().getSimpleName());
			}

			@Nested
			@ClassTemplate
			@Order(2)
			class Inner1Inner0 {
				@Test
				void test() {
					callSequence.add(getClass().getSimpleName());
				}
			}

			@Nested
			@Order(1)
			class Inner1Inner1 {
				@Test
				void test() {
					callSequence.add(getClass().getSimpleName());
				}
			}
		}

		private static class Twice implements ClassTemplateInvocationContextProvider {

			@Override
			public boolean supportsClassTemplate(ExtensionContext context) {
				return true;
			}

			@Override
			public Stream<ClassTemplateInvocationContext> provideClassTemplateInvocationContexts(
					ExtensionContext context) {
				return Stream.of(new Ctx(), new Ctx());
			}

			private record Ctx() implements ClassTemplateInvocationContext {
			}
		}
	}

	@TestClassOrder(ClassOrderer.DisplayName.class)
	static class RevertingBackToDefaultOrderTestCase {

		@Nested
		@TestClassOrder(ClassOrderer.Default.class)
		class Inner {

			@Nested
			@Order(3)
			class Test1 {
				@Test
				void test() {
					callSequence.add(getClass().getSimpleName());
				}
			}

			@Nested
			@Order(2)
			class Test2 {
				@Test
				void test() {
					callSequence.add(getClass().getSimpleName());
				}
			}

			@Nested
			@Order(4)
			class Test3 {
				@Test
				void test() {
					callSequence.add(getClass().getSimpleName());
				}
			}

			@Nested
			@Order(1)
			class Test4 {
				@Test
				void test() {
					callSequence.add(getClass().getSimpleName());
				}
			}
		}
	}

}
