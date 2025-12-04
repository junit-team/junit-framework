/*
 * Copyright 2015-2025 the original author or authors.
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v2.0 which
 * accompanies this distribution and is available at
 *
 * https://www.eclipse.org/legal/epl-v20.html
 */

package org.junit.jupiter.engine.discovery.predicates;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestFactory;
import org.junit.jupiter.api.TestTemplate;
import org.junit.platform.engine.DiscoveryIssue;
import org.junit.platform.engine.DiscoveryIssue.Severity;
import org.junit.platform.engine.support.descriptor.ClassSource;
import org.junit.platform.engine.support.discovery.DiscoveryIssueReporter;

public class TestClassPredicatesTests {

	private final List<DiscoveryIssue> discoveryIssues = new ArrayList<>();
	private final TestClassPredicates predicates = new TestClassPredicates(
		DiscoveryIssueReporter.collecting(discoveryIssues));

	@Nested
	class StandaloneTestClasses {

		@Test
		void classWithTestMethodEvaluatesToTrue() {
			assertTrue(predicates.looksLikeIntendedTestClass(ClassWithTestMethod.class));
			assertTrue(predicates.isValidStandaloneTestClass(ClassWithTestMethod.class));
		}

		@Test
		void classWithTestFactoryEvaluatesToTrue() {
			assertTrue(predicates.looksLikeIntendedTestClass(ClassWithTestFactory.class));
			assertTrue(predicates.isValidStandaloneTestClass(ClassWithTestFactory.class));
		}

		@Test
		void classWithTestTemplateEvaluatesToTrue() {
			assertTrue(predicates.looksLikeIntendedTestClass(ClassWithTestTemplate.class));
			assertTrue(predicates.isValidStandaloneTestClass(ClassWithTestTemplate.class));
		}

		@Test
		void classWithNestedTestClassEvaluatesToTrue() {
			assertTrue(predicates.looksLikeIntendedTestClass(ClassWithNestedTestClass.class));
			assertTrue(predicates.isValidStandaloneTestClass(ClassWithNestedTestClass.class));
		}

		@Test
		void staticTestClassEvaluatesToTrue() {
			assertTrue(predicates.looksLikeIntendedTestClass(TestCases.StaticTestCase.class));
			assertTrue(predicates.isValidStandaloneTestClass(TestCases.StaticTestCase.class));
		}

		// -------------------------------------------------------------------------

		@Test
		void abstractClassEvaluatesToFalse() {
			assertTrue(predicates.looksLikeIntendedTestClass(AbstractClass.class));
			assertFalse(predicates.isValidStandaloneTestClass(AbstractClass.class));
			assertThat(discoveryIssues).isEmpty();
		}

		@Test
		void localClassEvaluatesToFalse() {

			@SuppressWarnings({ "JUnitMalformedDeclaration", "NewClassNamingConvention" })
			class LocalClass {
				@SuppressWarnings("unused")
				@Test
				void test() {
				}
			}

			var candidate = LocalClass.class;

			assertTrue(predicates.looksLikeIntendedTestClass(candidate));
			assertFalse(predicates.isValidStandaloneTestClass(candidate));

			var issue = DiscoveryIssue.builder(Severity.WARNING,
				"Test class '%s' must not be a local class. It will not be executed.".formatted(candidate.getName())) //
					.source(ClassSource.from(candidate)) //
					.build();
			assertThat(discoveryIssues).containsExactly(issue);
		}

		@Test
		void anonymousClassEvaluatesToFalse() {

			Object object = new Object() {
				@SuppressWarnings("unused")
				@Test
				void test() {
				}
			};

			Class<?> candidate = object.getClass();

			assertTrue(predicates.looksLikeIntendedTestClass(candidate));
			assertFalse(predicates.isValidStandaloneTestClass(candidate));

			var issue = DiscoveryIssue.builder(Severity.WARNING,
				"Test class '%s' must not be anonymous. It will not be executed.".formatted(candidate.getName())) //
					.source(ClassSource.from(candidate)) //
					.build();
			assertThat(discoveryIssues).containsExactly(issue);
		}

		@Test
		void privateClassWithTestMethodEvaluatesToFalse() {
			var candidate = TestCases.PrivateClassWithTestMethod.class;

			assertTrue(predicates.looksLikeIntendedTestClass(candidate));
			assertFalse(predicates.isValidStandaloneTestClass(candidate));

			var notPrivateIssue = DiscoveryIssue.builder(Severity.WARNING,
				"Test class '%s' must not be private. It will not be executed.".formatted(candidate.getName())) //
					.source(ClassSource.from(candidate)) //
					.build();
			var notInnerClassIssue = DiscoveryIssue.builder(Severity.WARNING,
				"Test class '%s' must not be an inner class unless annotated with @Nested. It will not be executed.".formatted(
					candidate.getName())) //
					.source(ClassSource.from(candidate)) //
					.build();
			assertThat(discoveryIssues).containsExactlyInAnyOrder(notPrivateIssue, notInnerClassIssue);
		}

		@Test
		void privateClassWithTestFactoryEvaluatesToFalse() {
			var candidate = TestCases.PrivateClassWithTestFactory.class;

			assertTrue(predicates.looksLikeIntendedTestClass(candidate));
			assertFalse(predicates.isValidStandaloneTestClass(candidate));

			var notPrivateIssue = DiscoveryIssue.builder(Severity.WARNING,
				"Test class '%s' must not be private. It will not be executed.".formatted(candidate.getName())) //
					.source(ClassSource.from(candidate)) //
					.build();
			var notInnerClassIssue = DiscoveryIssue.builder(Severity.WARNING,
				"Test class '%s' must not be an inner class unless annotated with @Nested. It will not be executed.".formatted(
					candidate.getName())) //
					.source(ClassSource.from(candidate)) //
					.build();
			assertThat(discoveryIssues).containsExactlyInAnyOrder(notPrivateIssue, notInnerClassIssue);
		}

		@Test
		void privateClassWithTestTemplateEvaluatesToFalse() {
			var candidate = TestCases.PrivateClassWithTestTemplate.class;

			assertTrue(predicates.looksLikeIntendedTestClass(candidate));
			assertFalse(predicates.isValidStandaloneTestClass(candidate));

			var notPrivateIssue = DiscoveryIssue.builder(Severity.WARNING,
				"Test class '%s' must not be private. It will not be executed.".formatted(candidate.getName())) //
					.source(ClassSource.from(candidate)) //
					.build();
			var notInnerClassIssue = DiscoveryIssue.builder(Severity.WARNING,
				"Test class '%s' must not be an inner class unless annotated with @Nested. It will not be executed.".formatted(
					candidate.getName())) //
					.source(ClassSource.from(candidate)) //
					.build();
			assertThat(discoveryIssues).containsExactlyInAnyOrder(notPrivateIssue, notInnerClassIssue);
		}

		@Test
		void privateClassWithNestedTestCasesEvaluatesToFalse() {
			var candidate = TestCases.PrivateClassWithNestedTestClass.class;

			assertTrue(predicates.looksLikeIntendedTestClass(candidate));
			assertFalse(predicates.isValidStandaloneTestClass(candidate));

			var notPrivateIssue = DiscoveryIssue.builder(Severity.WARNING,
				"Test class '%s' must not be private. It will not be executed.".formatted(candidate.getName())) //
					.source(ClassSource.from(candidate)) //
					.build();
			var notInnerClassIssue = DiscoveryIssue.builder(Severity.WARNING,
				"Test class '%s' must not be an inner class unless annotated with @Nested. It will not be executed.".formatted(
					candidate.getName())) //
					.source(ClassSource.from(candidate)) //
					.build();
			assertThat(discoveryIssues).containsExactlyInAnyOrder(notPrivateIssue, notInnerClassIssue);
		}

		@Test
		void privateStaticTestClassEvaluatesToFalse() {
			var candidate = TestCases.PrivateStaticTestCase.class;

			assertTrue(predicates.looksLikeIntendedTestClass(candidate));
			assertFalse(predicates.isValidStandaloneTestClass(candidate));

			var notPrivateIssue = DiscoveryIssue.builder(Severity.WARNING,
				"Test class '%s' must not be private. It will not be executed.".formatted(candidate.getName())) //
					.source(ClassSource.from(candidate)) //
					.build();
			assertThat(discoveryIssues).containsExactly(notPrivateIssue);
		}

		/*
		 * see https://github.com/junit-team/junit-framework/issues/2249
		 */
		@Test
		void recursiveHierarchies() {
			assertTrue(predicates.looksLikeIntendedTestClass(TestCases.OuterClass.class));
			assertTrue(predicates.isValidStandaloneTestClass(TestCases.OuterClass.class));
			assertThat(discoveryIssues).isEmpty();

			var candidate = TestCases.OuterClass.RecursiveInnerClass.class;

			assertTrue(predicates.looksLikeIntendedTestClass(candidate));
			assertFalse(predicates.isValidStandaloneTestClass(candidate));

			var notInnerClassIssue = DiscoveryIssue.builder(Severity.WARNING,
				"Test class '%s' must not be an inner class unless annotated with @Nested. It will not be executed.".formatted(
					candidate.getName())) //
					.source(ClassSource.from(candidate)) //
					.build();
			assertThat(discoveryIssues).containsExactly(notInnerClassIssue);
		}

	}

	@Nested
	class NestedTestClasses {

		@Test
		void innerClassEvaluatesToTrue() {
			var candidate = TestCases.NestedClassesTestCase.InnerClass.class;
			assertThat(predicates.isAnnotatedWithNested).accepts(candidate);
			assertTrue(predicates.isValidNestedTestClass(candidate));
			assertThat(predicates.isAnnotatedWithNestedAndValid).accepts(candidate);
		}

		@Test
		void staticNestedClassEvaluatesToFalse() {
			var candidate = TestCases.NestedClassesTestCase.StaticNestedClass.class;
			assertThat(predicates.isAnnotatedWithNested).accepts(candidate);
			assertFalse(predicates.isValidNestedTestClass(candidate));
			assertThat(predicates.isAnnotatedWithNestedAndValid).rejects(candidate);

			var issue = DiscoveryIssue.builder(Severity.WARNING,
				"@Nested class '%s' must not be static. ".formatted(candidate.getName())
						+ "It will only be executed if discovered as a standalone test class. "
						+ "You should remove the annotation or make it non-static to resolve this warning.") //
					.source(ClassSource.from(candidate)) //
					.build();
			assertThat(discoveryIssues.stream().distinct()).containsExactly(issue);
		}

		@Test
		void topLevelClassEvaluatesToFalse() {
			var candidate = InvalidTopLevelNestedTestClass.class;
			assertThat(predicates.isAnnotatedWithNested).accepts(candidate);
			assertFalse(predicates.isValidNestedTestClass(candidate));
			assertThat(predicates.isAnnotatedWithNestedAndValid).rejects(candidate);

			var issue = DiscoveryIssue.builder(Severity.WARNING,
				("Top-level class '%s' must not be annotated with @Nested. ".formatted(candidate.getName())
						+ "It will be executed anyway for backward compatibility. "
						+ "You should remove the @Nested annotation to resolve this warning.")) //
					.source(ClassSource.from(candidate)) //
					.build();
			assertThat(discoveryIssues.stream().distinct()).containsExactly(issue);
		}

		@Test
		void privateNestedClassEvaluatesToFalse() {
			var candidate = TestCases.NestedClassesTestCase.PrivateInnerClass.class;
			assertThat(predicates.isAnnotatedWithNested).accepts(candidate);
			assertFalse(predicates.isValidNestedTestClass(candidate));
			assertThat(predicates.isAnnotatedWithNestedAndValid).rejects(candidate);

			var issue = DiscoveryIssue.builder(Severity.WARNING,
				"@Nested class '%s' must not be private. It will not be executed.".formatted(candidate.getName())) //
					.source(ClassSource.from(candidate)) //
					.build();
			assertThat(discoveryIssues.stream().distinct()).containsExactly(issue);
		}

		@Test
		void abstractInnerClassEvaluatesToFalse() {
			var candidate = TestCases.NestedClassesTestCase.AbstractInnerClass.class;
			assertThat(predicates.isAnnotatedWithNested).accepts(candidate);
			assertFalse(predicates.isValidNestedTestClass(candidate));
			assertThat(predicates.isAnnotatedWithNestedAndValid).rejects(candidate);
			assertThat(discoveryIssues).isEmpty();
		}

		@Test
		void localClassEvaluatesToFalse() {

			@Nested
			class LocalClass {
			}

			var candidate = LocalClass.class;

			assertThat(predicates.isAnnotatedWithNested).accepts(candidate);
			assertFalse(predicates.isValidNestedTestClass(candidate));
			assertThat(predicates.isAnnotatedWithNestedAndValid).rejects(candidate);

			var issue = DiscoveryIssue.builder(Severity.WARNING,
				"@Nested class '%s' must not be static. ".formatted(candidate.getName())
						+ "It will only be executed if discovered as a standalone test class. "
						+ "You should remove the annotation or make it non-static to resolve this warning.") //
					.source(ClassSource.from(candidate)) //
					.build();
			assertThat(discoveryIssues.stream().distinct()).containsExactly(issue);
		}
	}

	// -------------------------------------------------------------------------

	static class TestCases {

		@SuppressWarnings({ "JUnitMalformedDeclaration", "InnerClassMayBeStatic" })
		private class PrivateClassWithTestMethod {

			@Test
			void test() {
			}

		}

		@SuppressWarnings("InnerClassMayBeStatic")
		private class PrivateClassWithTestFactory {

			@TestFactory
			Collection<DynamicTest> factory() {
				return new ArrayList<>();
			}

		}

		@SuppressWarnings("InnerClassMayBeStatic")
		private class PrivateClassWithTestTemplate {

			@TestTemplate
			void template(int a) {
			}

		}

		@SuppressWarnings("InnerClassMayBeStatic")
		private class PrivateClassWithNestedTestClass {

			@Nested
			class InnerClass {

				@Test
				void first() {
				}

				@Test
				void second() {
				}

			}
		}

		// -------------------------------------------------------------------------

		@SuppressWarnings("JUnitMalformedDeclaration")
		static class StaticTestCase {

			@Test
			void test() {
			}
		}

		@SuppressWarnings("JUnitMalformedDeclaration")
		private static class PrivateStaticTestCase {

			@Test
			void test() {
			}
		}

		@SuppressWarnings("NewClassNamingConvention")
		static class OuterClass {

			@Nested
			class InnerClass {

				@Test
				void test() {
				}
			}

			// Intentionally commented out so that RecursiveInnerClass is NOT a candidate test class
			// @Nested
			@SuppressWarnings("InnerClassMayBeStatic")
			class RecursiveInnerClass extends OuterClass {
			}

		}

		private static class NestedClassesTestCase {

			@Nested
			class InnerClass {
			}

			@SuppressWarnings("JUnitMalformedDeclaration")
			@Nested
			static class StaticNestedClass {
			}

			@SuppressWarnings("JUnitMalformedDeclaration")
			@Nested
			private class PrivateInnerClass {
			}

			@Nested
			private abstract class AbstractInnerClass {
			}

		}
	}

}

// -----------------------------------------------------------------------------

abstract class AbstractClass {
	@SuppressWarnings("unused")
	@Test
	void test() {
	}
}

@SuppressWarnings("NewClassNamingConvention")
class ClassWithTestMethod {

	@Test
	void test() {
	}

}

@SuppressWarnings("NewClassNamingConvention")
class ClassWithTestFactory {

	@TestFactory
	Collection<DynamicTest> factory() {
		return new ArrayList<>();
	}

}

@SuppressWarnings("NewClassNamingConvention")
class ClassWithTestTemplate {

	@TestTemplate
	void template(int a) {
	}

}

@SuppressWarnings("NewClassNamingConvention")
class ClassWithNestedTestClass {

	@Nested
	class InnerClass {

		@Test
		void first() {
		}

		@Test
		void second() {
		}

	}
}

@SuppressWarnings("NewClassNamingConvention")
@Nested
class InvalidTopLevelNestedTestClass {
	@Test
	void test() {
	}
}
