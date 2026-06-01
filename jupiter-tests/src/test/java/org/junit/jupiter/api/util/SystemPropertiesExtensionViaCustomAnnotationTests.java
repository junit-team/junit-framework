/*
 * Copyright 2015-2026 the original author or authors.
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v2.0 which
 * accompanies this distribution and is available at
 *
 * https://www.eclipse.org/legal/epl-v20.html
 */

package org.junit.jupiter.api.util;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.parallel.ExecutionMode.SAME_THREAD;
import static org.junit.jupiter.api.util.testannotations.Property.A;
import static org.junit.jupiter.api.util.testannotations.Property.B;
import static org.junit.jupiter.api.util.testannotations.Property.C;
import static org.junit.jupiter.api.util.testannotations.Property.D;
import static org.junit.jupiter.api.util.testannotations.Property.E;
import static org.junit.jupiter.api.util.testannotations.Property.F;
import static org.junit.platform.engine.discovery.DiscoverySelectors.selectMethod;
import static org.junit.platform.testkit.engine.EventConditions.event;
import static org.junit.platform.testkit.engine.EventConditions.finishedSuccessfully;
import static org.junit.platform.testkit.engine.EventConditions.finishedWithFailure;
import static org.junit.platform.testkit.engine.EventConditions.test;
import static org.junit.platform.testkit.engine.TestExecutionResultConditions.instanceOf;
import static org.junit.platform.testkit.engine.TestExecutionResultConditions.message;

import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.Properties;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.ClassOrderer;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.MethodOrderer.OrderAnnotation;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestClassOrder;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestMethodOrder;
import org.junit.jupiter.api.extension.ExtensionConfigurationException;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.util.testannotations.ClearProp;
import org.junit.jupiter.api.util.testannotations.SetProp;
import org.junit.jupiter.engine.AbstractJupiterTestEngineTests;
import org.junit.platform.testkit.engine.EngineExecutionResults;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoSettings;

@DisplayName("Customized System Properties Extension")
class SystemPropertiesExtensionViaCustomAnnotationTests extends AbstractJupiterTestEngineTests {

	@BeforeAll
	static void globalSetUp() {
		System.setProperty(A.name(), "old A");
		System.setProperty(B.name(), "old B");
		System.setProperty(C.name(), "old C");

		System.clearProperty(D.name());
		System.clearProperty(E.name());
		System.clearProperty(F.name());
	}

	@AfterAll
	static void globalTearDown() {
		System.clearProperty(A.name());
		System.clearProperty(B.name());
		System.clearProperty(C.name());

		assertThat(System.getProperty("clear prop D")).isNull();
		assertThat(System.getProperty(E.name())).isNull();
		assertThat(System.getProperty(F.name())).isNull();
	}

	@Nested
	@DisplayName("with @ClearProp")
	@ClearProp(key = A)
	class ClearPropTests {

		@Test
		@DisplayName("should clear system property")
		@ClearProp(key = B)
		void shouldClearProp() {
			assertThat(System.getProperty(A.name())).isNull();
			assertThat(System.getProperty(B.name())).isNull();
			assertThat(System.getProperty(C.name())).isEqualTo("old C");

			assertThat(System.getProperty("clear prop D")).isNull();
			assertThat(System.getProperty(E.name())).isNull();
			assertThat(System.getProperty(F.name())).isNull();
		}

		@Test
		@DisplayName("should be repeatable")
		@ClearProp(key = B)
		@ClearProp(key = C)
		void shouldBeRepeatable() {
			assertThat(System.getProperty(A.name())).isNull();
			assertThat(System.getProperty(B.name())).isNull();
			assertThat(System.getProperty(C.name())).isNull();

			assertThat(System.getProperty(D.name())).isNull();
			assertThat(System.getProperty(E.name())).isNull();
			assertThat(System.getProperty(F.name())).isNull();
		}

	}

	@Nested
	@DisplayName("with @SetProp")
	@SetProp(key = A, value = "new A")
	class SetPropTests {

		@Test
		@DisplayName("should set system property to value")
		@SetProp(key = B, value = "new B")
		void shouldSetPropToValue() {
			assertThat(System.getProperty(A.name())).isEqualTo("new A");
			assertThat(System.getProperty(B.name())).isEqualTo("new B");
			assertThat(System.getProperty(C.name())).isEqualTo("old C");

			assertThat(System.getProperty("clear prop D")).isNull();
			assertThat(System.getProperty(E.name())).isNull();
			assertThat(System.getProperty(F.name())).isNull();
		}

		@Test
		@DisplayName("should be repeatable")
		@SetProp(key = B, value = "new B")
		@SetProp(key = D, value = "new D")
		void shouldBeRepeatable() {
			assertThat(System.getProperty(A.name())).isEqualTo("new A");
			assertThat(System.getProperty(B.name())).isEqualTo("new B");
			assertThat(System.getProperty(C.name())).isEqualTo("old C");

			assertThat(System.getProperty(D.name())).isEqualTo("new D");
			assertThat(System.getProperty(E.name())).isNull();
			assertThat(System.getProperty(F.name())).isNull();
		}

	}

	@Nested
	@DisplayName("with both @ClearProp and @SetProp")
	@ClearProp(key = A)
	@SetProp(key = D, value = "new D")
	class CombinedClearAndSetTests {

		@Test
		@DisplayName("should be combinable")
		@ClearProp(key = B)
		@SetProp(key = E, value = "new E")
		void clearAndSetPropShouldBeCombinable() {
			assertThat(System.getProperty(A.name())).isNull();
			assertThat(System.getProperty(B.name())).isNull();
			assertThat(System.getProperty(C.name())).isEqualTo("old C");

			assertThat(System.getProperty(D.name())).isEqualTo("new D");
			assertThat(System.getProperty(E.name())).isEqualTo("new E");
			assertThat(System.getProperty(F.name())).isNull();
		}

		@Test
		@DisplayName("method level should overwrite class level")
		@ClearProp(key = D)
		@SetProp(key = A, value = "new A")
		void methodLevelShouldOverwriteClassLevel() {
			assertThat(System.getProperty(A.name())).isEqualTo("new A");
			assertThat(System.getProperty(B.name())).isEqualTo("old B");
			assertThat(System.getProperty(C.name())).isEqualTo("old C");

			assertThat(System.getProperty("clear prop D")).isNull();
			assertThat(System.getProperty(E.name())).isNull();
			assertThat(System.getProperty(F.name())).isNull();
		}

		@Test
		@DisplayName("method level should not clash (in terms of duplicate entries) with class level")
		@SetProp(key = A, value = "new A")
		void methodLevelShouldNotClashWithClassLevel() {
			assertThat(System.getProperty(A.name())).isEqualTo("new A");
			assertThat(System.getProperty(B.name())).isEqualTo("old B");
			assertThat(System.getProperty(C.name())).isEqualTo("old C");
			assertThat(System.getProperty(D.name())).isEqualTo("new D");

			assertThat(System.getProperty(E.name())).isNull();
			assertThat(System.getProperty(F.name())).isNull();
		}

	}

	@Nested
	@DisplayName("with Set, Clear, and Restore")
	@WritesSystemProperty // Many of these tests write, many also access
	@Execution(SAME_THREAD) // Uses instance state
	@TestInstance(TestInstance.Lifecycle.PER_CLASS) // Uses instance state
	@TestClassOrder(ClassOrderer.OrderAnnotation.class)
	class CombinedClearSetRestoreTests {

		Properties initialState; // Stateful

		@BeforeAll
		void beforeAll() {
			initialState = System.getProperties();
		}

		@Nested
		@Order(1)
		@DisplayName("Set, Clear & Restore on class")
		@ClearProp(key = A)
		@SetProp(key = D, value = "new D")
		@RestoreSystemProperties
		@TestMethodOrder(OrderAnnotation.class)
		@TestInstance(TestInstance.Lifecycle.PER_CLASS) // Uses instance state
		class SetClearRestoreOnClass {

			@AfterAll
			void afterAll() {
				System.setProperties(new Properties()); // Really blow it up after this class
			}

			@Test
			@Order(1)
			@DisplayName("Set, Clear on method w/ direct set Sys Prop")
			@ClearProp(key = B)
			@SetProp(key = E, value = "new E")
			void clearSetRestoreShouldBeCombinable() {
				assertThat(System.getProperties()).withFailMessage(
					"Restore should swap out the Sys Properties instance").isNotSameAs(initialState);

				// Direct modification - shouldn't be visible in next test
				System.setProperty("Restore", "Restore Me");
				System.getProperties().put("XYZ", this);

				assertThat(System.getProperty("Restore")).isEqualTo("Restore Me");
				assertThat(System.getProperties().get("XYZ")).isSameAs(this);

				// All the others
				assertThat(System.getProperty(A.name())).isNull();
				assertThat(System.getProperty(B.name())).isNull();
				assertThat(System.getProperty(C.name())).isEqualTo("old C");

				assertThat(System.getProperty(D.name())).isEqualTo("new D");
				assertThat(System.getProperty(E.name())).isEqualTo("new E");
				assertThat(System.getProperty(F.name())).isNull();
			}

			@Test
			@DisplayName("Restore from class should restore direct mods")
			@Order(2)
			void restoreShouldHaveRevertedDirectModification() {
				assertThat(System.getProperty("Restore")).isNull();
				assertThat(System.getProperties().get("XYZ")).isNull();
			}

		}

		@Nested
		@Order(2)
		@DisplayName("Prior nested class changes should be restored}")
		class priorNestedChangesRestored {

			@Test
			@DisplayName("Restore from class should restore direct mods")
			void restoreShouldHaveRevertedDirectModification() {
				assertThat(System.getProperties()).isSameAs(initialState);
			}

		}

		@Nested
		@Order(3)
		@DisplayName("Set & Clear on class, Restore on method")
		@ClearProp(key = A)
		@SetProp(key = D, value = "new D")
		@TestMethodOrder(OrderAnnotation.class)
		@TestInstance(TestInstance.Lifecycle.PER_CLASS) // Uses instance state
		class SetAndClearOnClass {

			Properties initialState; // Stateful

			@BeforeAll
			void beforeAll() {
				initialState = System.getProperties();
			}

			@Test
			@Order(1)
			@DisplayName("Set, Clear & Restore on method w/ direct set Sys Prop")
			@ClearProp(key = B)
			@SetProp(key = E, value = "new E")
			@RestoreSystemProperties
			void clearSetRestoreShouldBeCombinable() {
				assertThat(System.getProperties()).withFailMessage(
					"Restore should swap out the Sys Properties instance").isNotSameAs(initialState);

				// Direct modification - shouldn't be visible in the next test
				System.setProperty("Restore", "Restore Me");
				System.getProperties().put("XYZ", this);

				// All the others
				assertThat(System.getProperty(A.name())).isNull();
				assertThat(System.getProperty(B.name())).isNull();
				assertThat(System.getProperty(C.name())).isEqualTo("old C");

				assertThat(System.getProperty(D.name())).isEqualTo("new D");
				assertThat(System.getProperty(E.name())).isEqualTo("new E");
				assertThat(System.getProperty(F.name())).isNull();
			}

			@Test
			@DisplayName("Restore from prior method should restore direct mods")
			@Order(2)
			void restoreShouldHaveRevertedDirectModification() {
				assertThat(System.getProperty("Restore")).isNull();
				assertThat(System.getProperties().get("XYZ")).isNull();
				assertThat(System.getProperties()).isSameAs(initialState);
			}

		}

	}

	@Nested
	@DisplayName("@RestoreSystemProperties individual methods tests")
	@WritesSystemProperty // Many of these tests write, many also access
	class RestoreSystemPropertiesUnitTests {

		SystemPropertiesExtension spe;

		@BeforeEach
		void beforeEach() {
			spe = new SystemPropertiesExtension();
		}

		@Nested
		@DisplayName("Attributes of RestoreSystemProperties Annotation")
		class BasicAttributesOfRestoreSystemProperties {

			@Test
			@DisplayName("Restore annotation has correct markers")
			void restoreHasCorrectMarkers() {
				assertThat(RestoreSystemProperties.class).hasAnnotations(Inherited.class, WritesSystemProperty.class);
			}

			@Test
			@DisplayName("Restore annotation has correct retention")
			void restoreHasCorrectRetention() {
				assertThat(RestoreSystemProperties.class.getAnnotation(Retention.class).value()).isEqualTo(
					RetentionPolicy.RUNTIME);
			}

			@Test
			@DisplayName("Restore annotation has correct targets")
			void restoreHasCorrectTargets() {
				assertThat(RestoreSystemProperties.class.getAnnotation(Target.class).value()).containsExactlyInAnyOrder(
					ElementType.METHOD, ElementType.TYPE);
			}

		}

		@Nested
		@DisplayName("RestorableContext Workflow Tests")
		@MockitoSettings
		class RestorableContextWorkflowTests {

			@Mock
			ExtensionContext context;

			@Test
			@DisplayName("Workflow of RestorableContext")
			void workflowOfRestorableContexts() {
				Properties initialState = System.getProperties(); //This is a live reference

				try {
					Properties returnedFromPrepareToEnter = spe.prepareToEnterRestorableContext(context);
					Properties postPrepareToEnterSysProps = System.getProperties();
					spe.prepareToExitRestorableContext(initialState);
					Properties postPrepareToExitSysProps = System.getProperties();

					assertThat(returnedFromPrepareToEnter) //
							.withFailMessage(
								"prepareToEnterRestorableContext should return actual original or deep copy") //
							.isSameAs(initialState);

					assertThat(returnedFromPrepareToEnter) //
							.withFailMessage("prepareToEnterRestorableContext should replace the actual Sys Props") //
							.isNotSameAs(postPrepareToEnterSysProps);

					assertThat(postPrepareToEnterSysProps).isEqualTo(initialState);

					assertThat(postPrepareToExitSysProps).isSameAs(initialState);

				}
				finally {
					System.setProperties(initialState); // Ensure complete recovery
				}
			}

		}

	}

	@Nested
	@DisplayName("with nested classes")
	@ClearProp(key = A)
	@SetProp(key = B, value = "new B")
	class NestedSystemPropertyTests {

		@Nested
		@TestMethodOrder(OrderAnnotation.class)
		@DisplayName("without SystemProperty annotations")
		class NestedClass {

			@Test
			@Order(1)
			@ReadsSystemProperty
			@DisplayName("system properties should be set from enclosed class when they are not provided in nested")
			void shouldSetPropFromEnclosedClass() {
				assertThat(System.getProperty(A.name())).isNull();
				assertThat(System.getProperty(B.name())).isEqualTo("new B");
			}

			@Test
			@Order(2)
			@ReadsSystemProperty
			@DisplayName("system properties should be set from enclosed class after restore")
			void shouldSetPropFromEnclosedClassAfterRestore() {
				assertThat(System.getProperty(A.name())).isNull();
				assertThat(System.getProperty(B.name())).isEqualTo("new B");
			}

		}

		@Nested
		@SetProp(key = B, value = "newer B")
		@DisplayName("with @SetProp annotation")
		class AnnotatedNestedClass {

			@Test
			@ReadsSystemProperty
			@DisplayName("system property should be set from nested class when it is provided")
			void shouldSetPropFromNestedClass() {
				assertThat(System.getProperty(B.name())).isEqualTo("newer B");
			}

			@Test
			@SetProp(key = B, value = "newest B")
			@DisplayName("system property should be set from method when it is provided")
			void shouldSetPropFromMethodOfNestedClass() {
				assertThat(System.getProperty(B.name())).isEqualTo("newest B");
			}

		}

	}

	@Nested
	@SetProp(key = A, value = "new A")
	@TestInstance(TestInstance.Lifecycle.PER_CLASS)
	class ResettingSystemPropertyTests {

		@Nested
		@SetProp(key = A, value = "newer A")
		@TestInstance(TestInstance.Lifecycle.PER_CLASS)
		class ResettingSystemPropertyAfterEachNestedTests {

			@BeforeEach
			void changeShouldBeVisible() {
				// We already see "newest A" because BeforeEachCallBack is invoked before @BeforeEach
				// See https://junit.org/junit5/docs/current/user-guide/#extensions-execution-order-overview
				assertThat(System.getProperty(A.name())).isEqualTo("newest A");
			}

			@Test
			@SetProp(key = A, value = "newest A")
			void setForTestMethod() {
				assertThat(System.getProperty(A.name())).isEqualTo("newest A");
			}

			@AfterEach
			@ReadsSystemProperty
			void resetAfterTestMethodExecution() {
				// We still see "newest A" because AfterEachCallBack is invoked after @AfterEach
				// See https://junit.org/junit5/docs/current/user-guide/#extensions-execution-order-overview
				assertThat(System.getProperty(A.name())).isEqualTo("newest A");
			}

		}

		@Nested
		@SetProp(key = A, value = "newer A")
		@TestInstance(TestInstance.Lifecycle.PER_CLASS)
		class ResettingSystemPropertyAfterAllNestedTests {

			@BeforeAll
			void changeShouldBeVisible() {
				assertThat(System.getProperty(A.name())).isEqualTo("newer A");
			}

			@Test
			@SetProp(key = A, value = "newest A")
			void setForTestMethod() {
				assertThat(System.getProperty(A.name())).isEqualTo("newest A");
			}

			@AfterAll
			@ReadsSystemProperty
			void resetAfterTestMethodExecution() {
				assertThat(System.getProperty(A.name())).isEqualTo("newer A");
			}

		}

		@AfterAll
		@ReadsSystemProperty
		void resetAfterTestContainerExecution() {
			assertThat(System.getProperty(A.name())).isEqualTo("new A");
		}

	}

	@Nested
	@DisplayName("with incorrect configuration")
	class ConfigurationFailureTests {

		@Test
		@DisplayName("should fail when clear and set same system property")
		void shouldFailWhenClearAndSetSameSystemProperty() {
			EngineExecutionResults results = executeTests(selectMethod(MethodLevelInitializationFailureTestCases.class,
				"shouldFailWhenClearAndSetSameSystemProperty"));
			results.testEvents().assertThatEvents().haveAtMost(1,
				finishedWithFailure(instanceOf(ExtensionConfigurationException.class),
					message(it -> it.contains("@DefaultTimeZone not configured correctly."))));
		}

		@Test
		@DisplayName("should not fail when clear same system property twice")
		void shouldNotFailWhenClearSameSystemPropertyTwice() {
			EngineExecutionResults results = executeTests(selectMethod(MethodLevelInitializationFailureTestCases.class,
				"shouldFailWhenClearSameSystemPropertyTwice"));

			results.testEvents().assertThatEvents().haveExactly(1, event(test(), finishedSuccessfully()));
		}

		@Test
		@DisplayName("should fail when set same system property twice")
		void shouldFailWhenSetSameSystemPropertyTwice() {
			EngineExecutionResults results = executeTests(selectMethod(MethodLevelInitializationFailureTestCases.class,
				"shouldFailWhenSetSameSystemPropertyTwice"));
			results.testEvents().assertThatEvents().haveAtMost(1,
				finishedWithFailure(instanceOf(ExtensionConfigurationException.class)));
		}

	}

	static class MethodLevelInitializationFailureTestCases {

		@Test
		@DisplayName("clearing and setting the same property")
		@ClearProp(key = A)
		@SetProp(key = A, value = "new A")
		void shouldFailWhenClearAndSetSameSystemProperty() {
		}

		@Test
		@ClearProp(key = A)
		@ClearProp(key = A)
		void shouldFailWhenClearSameSystemPropertyTwice() {
		}

		@Test
		@SetProp(key = A, value = "new A")
		@SetProp(key = A, value = "new B")
		void shouldFailWhenSetSameSystemPropertyTwice() {
		}

	}

	@Nested
	@DisplayName("Clear and Set with inheritance")
	class InheritanceClearAndSetTests extends InheritanceClearAndSetBaseTest {

		@Test
		@DisplayName("should inherit clear and set annotations")
		void shouldInheritClearAndSetProperty() {
			assertThat(System.getProperty(A.name())).isNull();
			assertThat(System.getProperty(B.name())).isNull();
			assertThat(System.getProperty(D.name())).isEqualTo("new D");
			assertThat(System.getProperty(E.name())).isEqualTo("new E");
		}

	}

	@Nested
	@DisplayName("Clear, Set, and Restore with inheritance")
	@TestMethodOrder(OrderAnnotation.class)
	@TestClassOrder(ClassOrderer.OrderAnnotation.class)
	@Execution(SAME_THREAD) // Uses instance state
	@TestInstance(TestInstance.Lifecycle.PER_CLASS) // Uses instance state
	class InheritanceClearSetRestoreTests extends InheritanceClearSetRestoreBaseTest {

		Properties initialState; // Stateful

		@BeforeAll
		void beforeAll() {
			initialState = System.getProperties();
		}

		@Test
		@Order(1)
		@DisplayName("should inherit clear and set annotations")
		void shouldInheritClearSetRestore() {
			// Direct modification - shouldn't be visible in the next test
			System.setProperty("Restore", "Restore Me");
			System.getProperties().put("XYZ", this);

			assertThat(System.getProperty(A.name())).isNull(); // The rest are checked elsewhere
		}

		@Test
		@Order(2)
		@DisplayName("Restore from class should restore direct mods")
		void restoreShouldHaveRevertedDirectModification() {
			assertThat(System.getProperty("Restore")).isNull();
			assertThat(System.getProperties().get("XYZ")).isNull();
			assertThat(System.getProperties()) //
					.withFailMessage("Restore should swap out the Sys Properties instance") //
					.isNotSameAs(initialState);
			assertThat(System.getProperties()).isEqualTo(initialState);
		}

		@Nested
		@Order(1)
		@DisplayName("Set props to ensure inherited restore")
		@TestMethodOrder(OrderAnnotation.class)
		@TestInstance(TestInstance.Lifecycle.PER_CLASS)
		class SetSomeValuesToRestore {

			@AfterAll
			void afterAll() {
				System.setProperty("RestoreAll", "Restore Me"); // This should also be restored
			}

			@Test
			@Order(1)
			@DisplayName("Inherit values and restore behavior")
			void shouldInheritInNestedClass() {
				assertThat(System.getProperty(A.name())).isNull();

				// Shouldn't be visible in the next test
				System.setProperty("Restore", "Restore Me");
			}

			@Test
			@Order(2)
			@DisplayName("Verify restore behavior bt methods")
			void verifyRestoreBetweenMethods() {
				assertThat(System.getProperty("Restore")).isNull();
			}

		}

		@Nested
		@Order(2)
		@DisplayName("Verify props are restored")
		@TestInstance(TestInstance.Lifecycle.PER_CLASS)
		class VerifyValuesAreRestored {

			@Test
			@DisplayName("Inherit values and restore behavior")
			void shouldInheritInNestedClass() {
				assertThat(System.getProperty("RestoreAll")).isNull(); // Should be restored
			}

		}

	}

	@ClearProp(key = A)
	@ClearProp(key = B)
	@SetProp(key = D, value = "new D")
	@SetProp(key = E, value = "new E")
	static class InheritanceClearAndSetBaseTest {

	}

	@ClearProp(key = A)
	@ClearProp(key = B)
	@SetProp(key = D, value = "new D")
	@SetProp(key = E, value = "new E")
	@RestoreSystemProperties
	static class InheritanceClearSetRestoreBaseTest {
	}

}
