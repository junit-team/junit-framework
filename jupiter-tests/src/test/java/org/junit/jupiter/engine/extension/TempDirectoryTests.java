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

import static java.lang.annotation.ElementType.ANNOTATION_TYPE;
import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.ElementType.PARAMETER;
import static java.lang.annotation.RetentionPolicy.RUNTIME;
import static java.util.Objects.requireNonNull;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;
import static org.junit.jupiter.api.Assumptions.assumeTrue;
import static org.junit.platform.engine.discovery.DiscoverySelectors.selectClass;
import static org.junit.platform.launcher.core.LauncherDiscoveryRequestBuilder.request;
import static org.junit.platform.testkit.engine.EventConditions.finishedWithFailure;
import static org.junit.platform.testkit.engine.TestExecutionResultConditions.cause;
import static org.junit.platform.testkit.engine.TestExecutionResultConditions.instanceOf;
import static org.junit.platform.testkit.engine.TestExecutionResultConditions.message;
import static org.junit.platform.testkit.engine.TestExecutionResultConditions.suppressed;

import java.io.File;
import java.io.IOException;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;
import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Field;
import java.lang.reflect.Parameter;
import java.nio.file.DirectoryNotEmptyException;
import java.nio.file.FileSystem;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

import com.github.marschall.memoryfilesystem.MemoryFileSystemBuilder;
import com.google.common.jimfs.Configuration;
import com.google.common.jimfs.Jimfs;

import org.assertj.core.api.Condition;
import org.jspecify.annotations.NullUnmarked;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.MethodOrderer.OrderAnnotation;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInfo;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestMethodOrder;
import org.junit.jupiter.api.TestReporter;
import org.junit.jupiter.api.condition.DisabledOnOs;
import org.junit.jupiter.api.condition.OS;
import org.junit.jupiter.api.extension.AnnotatedElementContext;
import org.junit.jupiter.api.extension.BeforeEachCallback;
import org.junit.jupiter.api.extension.ExtensionConfigurationException;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.ParameterResolutionException;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.api.io.TempDirFactory;
import org.junit.jupiter.api.io.TempDirFactory.Standard;
import org.junit.jupiter.engine.AbstractJupiterTestEngineTests;
import org.junit.jupiter.engine.Constants;
import org.junit.jupiter.engine.extension.TempDirectory.FileOperations;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.junit.platform.commons.PreconditionViolationException;
import org.junit.platform.engine.DiscoveryIssue;
import org.junit.platform.engine.DiscoveryIssue.Severity;
import org.junit.platform.engine.reporting.ReportEntry;
import org.junit.platform.testkit.engine.EngineExecutionResults;

/**
 * Integration tests for the new behavior of the {@link TempDirectory} extension
 * to create separate temp directories for each {@link TempDir} declaration.
 *
 * @since 5.8
 */
@DisplayName("TempDirectory extension (per declaration)")
class TempDirectoryTests extends AbstractJupiterTestEngineTests {

	private EngineExecutionResults executeTestsForClassWithDefaultFactory(Class<?> testClass,
			Class<? extends TempDirFactory> factoryClass) {
		return executeTests(request() //
				.selectors(selectClass(testClass)) //
				.configurationParameter(TempDir.DEFAULT_FACTORY_PROPERTY_NAME, factoryClass.getName()) //
				.build());
	}

	@BeforeEach
	@AfterEach
	void resetStaticVariables() {
		AllPossibleDeclarationLocationsTestCase.tempDirs.clear();
	}

	@SuppressWarnings("NullAway")
	@ParameterizedTest(name = "{0}")
	@EnumSource(TestInstance.Lifecycle.class)
	@DisplayName("resolves separate temp dirs for each annotation declaration")
	void resolvesSeparateTempDirsForEachAnnotationDeclaration(TestInstance.Lifecycle lifecycle) {
		var results = executeTests(request() //
				.selectors(selectClass(AllPossibleDeclarationLocationsTestCase.class)) //
				.configurationParameter(Constants.DEFAULT_TEST_INSTANCE_LIFECYCLE_PROPERTY_NAME,
					lifecycle.name()).build());

		results.containerEvents().assertStatistics(stats -> stats.started(2).succeeded(2));
		results.testEvents().assertStatistics(stats -> stats.started(2).succeeded(2));

		assertThat(AllPossibleDeclarationLocationsTestCase.tempDirs).hasSize(3);

		var classTempDirs = AllPossibleDeclarationLocationsTestCase.tempDirs.get("class");
		assertThat(classTempDirs) //
				.containsOnlyKeys("staticField1", "staticField2", "beforeAll1", "beforeAll2", "afterAll1", "afterAll2") //
				.values().hasSize(6).doesNotHaveDuplicates().allMatch(Files::notExists);

		var testATempDirs = AllPossibleDeclarationLocationsTestCase.tempDirs.get("testA");
		assertThat(testATempDirs) //
				.containsOnlyKeys("staticField1", "staticField2", "instanceFieldSetViaConstructorInjection",
					"instanceField1", "instanceField2", "beforeEach1", "beforeEach2", "test1", "test2", "afterEach1",
					"afterEach2") //
				.values().hasSize(11).doesNotHaveDuplicates().allMatch(Files::notExists);

		var testBTempDirs = AllPossibleDeclarationLocationsTestCase.tempDirs.get("testB");
		assertThat(testBTempDirs) //
				.containsOnlyKeys("staticField1", "staticField2", "instanceFieldSetViaConstructorInjection",
					"instanceField1", "instanceField2", "beforeEach1", "beforeEach2", "test1", "test2", "afterEach1",
					"afterEach2") //
				.values().hasSize(11).doesNotHaveDuplicates().allMatch(Files::notExists);

		assertThat(testATempDirs).containsEntry("staticField1", classTempDirs.get("staticField1"));
		assertThat(testBTempDirs).containsEntry("staticField1", classTempDirs.get("staticField1"));
		assertThat(testATempDirs).containsEntry("staticField2", classTempDirs.get("staticField2"));
		assertThat(testBTempDirs).containsEntry("staticField2", classTempDirs.get("staticField2"));

		switch (lifecycle) {
			case PER_CLASS -> assertThat(testATempDirs) //
					.containsEntry("instanceFieldSetViaConstructorInjection",
						testBTempDirs.get("instanceFieldSetViaConstructorInjection"));
			case PER_METHOD -> assertThat(testATempDirs) //
					.doesNotContainEntry("instanceFieldSetViaConstructorInjection",
						testBTempDirs.get("instanceFieldSetViaConstructorInjection"));
		}
		assertThat(testATempDirs).doesNotContainEntry("instanceField1", testBTempDirs.get("instanceField1"));
		assertThat(testATempDirs).doesNotContainEntry("instanceField2", testBTempDirs.get("instanceField2"));
		assertThat(testATempDirs).doesNotContainEntry("beforeEach1", testBTempDirs.get("beforeEach1"));
		assertThat(testATempDirs).doesNotContainEntry("beforeEach2", testBTempDirs.get("beforeEach2"));
		assertThat(testATempDirs).doesNotContainEntry("test1", testBTempDirs.get("test1"));
		assertThat(testATempDirs).doesNotContainEntry("test2", testBTempDirs.get("test2"));
		assertThat(testATempDirs).doesNotContainEntry("afterEach1", testBTempDirs.get("afterEach1"));
		assertThat(testATempDirs).doesNotContainEntry("afterEach2", testBTempDirs.get("afterEach2"));
	}

	@Test
	void supportsConstructorInjectionOnRecords() {
		executeTestsForClass(TempDirRecordTestCase.class).testEvents()//
				.assertStatistics(stats -> stats.started(1).succeeded(1));
	}

	@Test
	@DisplayName("does not prevent constructor parameter resolution")
	void tempDirectoryDoesNotPreventConstructorParameterResolution() {
		executeTestsForClass(TempDirectoryDoesNotPreventConstructorParameterResolutionTestCase.class).testEvents()//
				.assertStatistics(stats -> stats.started(1).succeeded(1));
	}

	@Test
	@DisplayName("does not prevent user from deleting the temp dir within a test")
	void tempDirectoryDoesNotPreventUserFromDeletingTempDir() {
		executeTestsForClass(UserTempDirectoryDeletionDoesNotCauseFailureTestCase.class).testEvents()//
				.assertStatistics(stats -> stats.started(1).succeeded(1));
	}

	@Test
	@DisplayName("is capable of removing a read-only file")
	void nonWritableFileDoesNotCauseFailure() {
		executeTestsForClass(NonWritableFileDoesNotCauseFailureTestCase.class).testEvents()//
				.assertStatistics(stats -> stats.started(1).succeeded(1));
	}

	@Test
	@DisplayName("is capable of removing non-executable, non-writable, or non-readable directories and folders")
	void nonMintPermissionsContentDoesNotCauseFailure() {
		executeTestsForClass(NonMintPermissionContentInTempDirectoryDoesNotCauseFailureTestCase.class).testEvents()//
				.assertStatistics(stats -> stats.started(13).succeeded(13));
	}

	@Test
	@DisplayName("is capable of removing a directory when its permissions have been changed")
	void nonMintPermissionsDoNotCauseFailure() {
		executeTestsForClass(NonMintTempDirectoryPermissionsDoNotCauseFailureTestCase.class).testEvents()//
				.assertStatistics(stats -> stats.started(42).succeeded(42));
	}

	@Test
	@DisabledOnOs(OS.WINDOWS)
	@DisplayName("is capable of removing a read-only file in a read-only dir")
	void readOnlyFileInReadOnlyDirDoesNotCauseFailure() {
		executeTestsForClass(ReadOnlyFileInReadOnlyDirDoesNotCauseFailureTestCase.class).testEvents()//
				.assertStatistics(stats -> stats.started(1).succeeded(1));
	}

	@Test
	@DisabledOnOs(OS.WINDOWS)
	@DisplayName("is capable of removing a read-only file in a dir in a read-only dir")
	void readOnlyFileInNestedReadOnlyDirDoesNotCauseFailure() {
		executeTestsForClass(ReadOnlyFileInDirInReadOnlyDirDoesNotCauseFailureTestCase.class).testEvents()//
				.assertStatistics(stats -> stats.started(1).succeeded(1));
	}

	@Test
	@DisplayName("can be used via instance field inside nested test classes")
	void canBeUsedViaInstanceFieldInsideNestedTestClasses() {
		executeTestsForClass(TempDirUsageInsideNestedClassesTestCase.class).testEvents()//
				.assertStatistics(stats -> stats.started(3).succeeded(3));
	}

	@Test
	@DisplayName("can be used via static field inside nested test classes")
	void canBeUsedViaStaticFieldInsideNestedTestClasses() {
		executeTestsForClass(StaticTempDirUsageInsideNestedClassTestCase.class).testEvents()//
				.assertStatistics(stats -> stats.started(2).succeeded(2));
	}

	@ParameterizedTest(name = "{0}")
	@ValueSource(classes = { UndeletableDirectoryTestCase.class, UndeletableFileTestCase.class })
	@DisplayName("only attempts to delete undeletable paths once")
	void onlyAttemptsToDeleteUndeletablePathsOnce(Class<?> testClass) {
		var results = executeTestsForClass(testClass);

		var tempDir = results.testEvents().reportingEntryPublished().stream().map(
			it -> it.getPayload(ReportEntry.class).orElseThrow()).map(
				it -> Path.of(it.getKeyValuePairs().get(UndeletableTestCase.TEMP_DIR))).findAny().orElseThrow();

		assertSingleFailedTest(results, //
			cause( //
				instanceOf(IOException.class), //
				message("Failed to delete temp directory " + tempDir.toAbsolutePath() + ". " + //
						"The following paths could not be deleted (see suppressed exceptions for details): <root>, undeletable"), //
				suppressed(0, instanceOf(DirectoryNotEmptyException.class)), //
				suppressed(1, instanceOf(IOException.class), message("Simulated failure")) //
			) //
		);
	}

	@Test
	void usingTheRemovedScopeConfigurationParameterProducesWarning() {
		var results = discoverTests(request() //
				.selectors(selectClass(AllPossibleDeclarationLocationsTestCase.class)) //
				.configurationParameter("junit.jupiter.tempdir.scope", "per_context"));

		assertThat(results.getDiscoveryIssues()) //
				.contains(DiscoveryIssue.create(Severity.WARNING, """
						The 'junit.jupiter.tempdir.scope' configuration parameter is no longer supported. \
						Please remove it from your configuration."""));
	}

	@Nested
	@DisplayName("reports failure")
	@TestMethodOrder(OrderAnnotation.class)
	class Failures {

		@Test
		@DisplayName("when @TempDir is used on static field of an unsupported type")
		@Order(20)
		void onlySupportsStaticFieldsOfTypePathAndFile() {
			var results = executeTestsForClass(AnnotationOnStaticFieldWithUnsupportedTypeTestCase.class);

			assertSingleFailedContainer(results, ExtensionConfigurationException.class,
				"Can only resolve @TempDir field of type java.nio.file.Path or java.io.File");
		}

		@Test
		@DisplayName("when @TempDir is used on instance field of an unsupported type")
		@Order(21)
		void onlySupportsInstanceFieldsOfTypePathAndFile() {
			var results = executeTestsForClass(AnnotationOnInstanceFieldWithUnsupportedTypeTestCase.class);

			assertSingleFailedTest(results, ExtensionConfigurationException.class,
				"Can only resolve @TempDir field of type java.nio.file.Path or java.io.File");
		}

		@Test
		@DisplayName("when @TempDir is used on parameter of an unsupported type")
		@Order(22)
		void onlySupportsParametersOfTypePathAndFile() {
			var results = executeTestsForClass(InvalidTestCase.class);

			// @formatter:off
			assertSingleFailedTest(results, instanceOf(ParameterResolutionException.class),
				message(m -> m.matches("Failed to resolve parameter \\[java.lang.String .+] in method \\[.+]: .+")),
				cause(
					instanceOf(ExtensionConfigurationException.class),
					message("Can only resolve @TempDir parameter of type java.nio.file.Path or java.io.File but was: java.lang.String")));
			// @formatter:on
		}

		@Test
		@DisplayName("when @TempDir factory does not return directory")
		@Order(32)
		void doesNotSupportTempDirFactoryNotReturningDirectory() {
			var results = executeTestsForClass(FactoryNotReturningDirectoryTestCase.class);

			// @formatter:off
			assertSingleFailedTest(results, instanceOf(ParameterResolutionException.class),
					message(m -> m.matches("Failed to resolve parameter \\[.+] in method \\[.+]: .+")),
					cause(
							instanceOf(ExtensionConfigurationException.class),
							message("Failed to create default temp directory"),
							cause(
									instanceOf(PreconditionViolationException.class),
									message("temp directory must be a directory")
							)
					));
			// @formatter:on
		}

		@Test
		@DisplayName("when default @TempDir factory does not return directory")
		@Order(33)
		void doesNotSupportCustomDefaultTempDirFactoryNotReturningDirectory() {
			var results = executeTestsForClassWithDefaultFactory(
				CustomDefaultFactoryNotReturningDirectoryTestCase.class, FactoryNotReturningDirectory.class);

			// @formatter:off
			assertSingleFailedTest(results, instanceOf(ParameterResolutionException.class),
					message(m -> m.matches("Failed to resolve parameter \\[.+] in method \\[.+]: .+")),
					cause(
							instanceOf(ExtensionConfigurationException.class),
							message("Failed to create default temp directory"),
							cause(
									instanceOf(PreconditionViolationException.class),
									message("temp directory must be a directory")
							)
					));
			// @formatter:on
		}

		private static class FactoryNotReturningDirectory implements TempDirFactory {

			@SuppressWarnings("DataFlowIssue")
			@Override
			public Path createTempDirectory(AnnotatedElementContext elementContext, ExtensionContext extensionContext) {
				return null;
			}
		}

		@Test
		@DisplayName("when @TempDir factory returns a non-default file system path for a File annotated element")
		@Order(34)
		void doesNotSupportNonDefaultFileSystemTempDirFactoryOnFileAnnotatedElement() {
			var results = executeTestsForClass(
				FactoryReturningNonDefaultFileSystemPathForFileAnnotatedElementTestCase.class);

			// @formatter:off
			assertSingleFailedTest(results, instanceOf(ParameterResolutionException.class),
					message(m -> m.matches("Failed to resolve parameter \\[.+] in method \\[.+]: .+")),
					cause(
							instanceOf(ExtensionConfigurationException.class),
							message("Failed to create default temp directory"),
							cause(
									instanceOf(PreconditionViolationException.class),
									message("temp directory with non-default file system cannot be injected into "
											+ File.class.getName() + " target")
							)
					));
			// @formatter:on
		}

	}

	@Nested
	@DisplayName("supports @TempDir")
	@TestMethodOrder(OrderAnnotation.class)
	class PrivateFields {

		@Test
		@DisplayName("on private static field")
		@Order(10)
		void supportsPrivateInstanceFields() {
			executeTestsForClass(AnnotationOnPrivateStaticFieldTestCase.class).testEvents()//
					.assertStatistics(stats -> stats.started(1).succeeded(1));
		}

		@Test
		@DisplayName("on private instance field")
		@Order(11)
		void supportsPrivateStaticFields() {
			executeTestsForClass(AnnotationOnPrivateInstanceFieldTestCase.class).testEvents()//
					.assertStatistics(stats -> stats.started(1).succeeded(1));
		}

	}

	@Nested
	@DisplayName("supports custom factory")
	class Factory {

		@Test
		@DisplayName("that uses test method name as temp dir name prefix")
		void supportsFactoryWithTestMethodNameAsPrefix() {
			executeTestsForClass(FactoryWithTestMethodNameAsPrefixTestCase.class).testEvents()//
					.assertStatistics(stats -> stats.started(1).succeeded(1));
		}

		@Test
		@DisplayName("that uses custom temp dir parent directory")
		void supportsFactoryWithCustomParentDirectory() {
			executeTestsForClass(FactoryWithCustomParentDirectoryTestCase.class).testEvents()//
					.assertStatistics(stats -> stats.started(1).succeeded(1));
		}

		@Test
		@DisplayName("that uses com.github.marschall:memoryfilesystem")
		void supportsFactoryWithMemoryFileSystem() {
			executeTestsForClass(FactoryWithMemoryFileSystemTestCase.class).testEvents()//
					.assertStatistics(stats -> stats.started(1).succeeded(1));
		}

		@Test
		@DisplayName("that uses com.google.jimfs:jimfs")
		void supportsFactoryWithJimfs() {
			executeTestsForClass(FactoryWithJimfsTestCase.class).testEvents()//
					.assertStatistics(stats -> stats.started(1).succeeded(1));
		}

		@Test
		@DisplayName("that uses annotated element name as temp dir name prefix")
		void supportsFactoryWithAnnotatedElementNameAsPrefix() {
			executeTestsForClass(FactoryWithAnnotatedElementNameAsPrefixTestCase.class).testEvents()//
					.assertStatistics(stats -> stats.started(1).succeeded(1));
		}

		@Test
		@DisplayName("that uses custom meta-annotation")
		void supportsFactoryWithCustomMetaAnnotation() {
			executeTestsForClass(FactoryWithCustomMetaAnnotationTestCase.class).testEvents()//
					.assertStatistics(stats -> stats.started(1).succeeded(1));
		}

	}

	@Nested
	@DisplayName("supports default factory")
	@TestMethodOrder(OrderAnnotation.class)
	class DefaultFactory {

		@Test
		@DisplayName("set to Jupiter's default")
		void supportsStandardDefaultFactory() {
			executeTestsForClassWithDefaultFactory(StandardDefaultFactoryTestCase.class, Standard.class) //
					.testEvents()//
					.assertStatistics(stats -> stats.started(1).succeeded(1));
		}

		@Test
		@DisplayName("set to custom factory")
		void supportsCustomDefaultFactory() {
			executeTestsForClassWithDefaultFactory(CustomDefaultFactoryTestCase.class, Factory.class) //
					.testEvents()//
					.assertStatistics(stats -> stats.started(1).succeeded(1));
		}

		@Test
		@DisplayName("set to custom factory together with declaration of Jupiter's default")
		void supportsCustomDefaultFactoryWithStandardFactoryOnDeclaration() {
			executeTestsForClassWithDefaultFactory( //
				CustomDefaultFactoryWithStandardDeclarationTestCase.class, Factory.class) //
						.testEvents()//
						.assertStatistics(stats -> stats.started(1).succeeded(1));
		}

		private static class Factory implements TempDirFactory {

			private boolean closed;

			@Override
			public Path createTempDirectory(AnnotatedElementContext elementContext, ExtensionContext extensionContext)
					throws Exception {
				return Files.createTempDirectory("custom");
			}

			@Override
			public void close() {
				if (closed) {
					throw new IllegalStateException("already closed");
				}
				closed = true;
			}
		}

	}

	private static void assertSingleFailedContainer(EngineExecutionResults results, Class<? extends Throwable> clazz,
			String message) {

		assertSingleFailedContainer(results, instanceOf(clazz), message(actual -> actual.contains(message)));
	}

	@SafeVarargs
	@SuppressWarnings("varargs")
	private static void assertSingleFailedContainer(EngineExecutionResults results,
			Condition<Throwable>... conditions) {

		results.containerEvents()//
				.assertStatistics(stats -> stats.started(2).failed(1).succeeded(1))//
				.assertThatEvents().haveExactly(1, finishedWithFailure(conditions));
	}

	private static void assertSingleFailedTest(EngineExecutionResults results, Class<? extends Throwable> clazz,
			String message) {

		assertSingleFailedTest(results, instanceOf(clazz), message(actual -> actual.contains(message)));
	}

	@SafeVarargs
	@SuppressWarnings("varargs")
	private static void assertSingleFailedTest(EngineExecutionResults results, Condition<Throwable>... conditions) {
		results.testEvents().assertStatistics(stats -> stats.started(1).failed(1).succeeded(0));
		results.testEvents().assertThatEvents().haveExactly(1, finishedWithFailure(conditions));
	}

	// -------------------------------------------------------------------------

	@SuppressWarnings("JUnitMalformedDeclaration")
	static class AnnotationOnPrivateInstanceFieldTestCase {

		@SuppressWarnings("unused")
		@TempDir
		private Path tempDir;

		@Test
		void test() {
			assertTrue(Files.exists(tempDir));
		}

	}

	@SuppressWarnings("JUnitMalformedDeclaration")
	static class AnnotationOnPrivateStaticFieldTestCase {

		@SuppressWarnings("unused")
		@TempDir
		private static Path tempDir;

		@Test
		void test() {
			assertTrue(Files.exists(tempDir));
		}

	}

	@SuppressWarnings("JUnitMalformedDeclaration")
	static class AnnotationOnStaticFieldWithUnsupportedTypeTestCase {

		@SuppressWarnings("unused")
		@TempDir
		static String tempDir;

		@Test
		void test1() {
		}

	}

	@SuppressWarnings("JUnitMalformedDeclaration")
	static class AnnotationOnInstanceFieldWithUnsupportedTypeTestCase {

		@SuppressWarnings("unused")
		@TempDir
		String tempDir;

		@Test
		void test1() {
		}

	}

	@SuppressWarnings("JUnitMalformedDeclaration")
	static class InvalidTestCase {

		@Test
		void wrongParameterType(@SuppressWarnings("unused") @TempDir String ignored) {
			fail("this should never be called");
		}
	}

	@Nested
	@DisplayName("resolves java.io.File injection type")
	class FileAndPathInjection {

		@TempDir
		File fileTempDir;

		@TempDir
		Path pathTempDir;

		@Test
		@DisplayName("and injected File and Path do not reference the same temp directory")
		void checkFile(@TempDir File tempDir, @TempDir Path ref) {
			assertFileAndPathAreNotEqual(tempDir, ref);
			assertFileAndPathAreNotEqual(this.fileTempDir, this.pathTempDir);
		}

		private static void assertFileAndPathAreNotEqual(File tempDir, Path ref) {
			Path path = tempDir.toPath();
			assertNotEquals(ref.toAbsolutePath(), path.toAbsolutePath());
			assertTrue(Files.exists(path));
		}

	}

	// https://github.com/junit-team/junit-framework/issues/1748
	@SuppressWarnings("JUnitMalformedDeclaration")
	static class TempDirectoryDoesNotPreventConstructorParameterResolutionTestCase {

		@TempDir
		Path tempDir;

		TempDirectoryDoesNotPreventConstructorParameterResolutionTestCase(TestInfo testInfo) {
			assertNotNull(testInfo);
		}

		@Test
		void test() {
			assertNotNull(tempDir);
		}

	}

	// https://github.com/junit-team/junit-framework/issues/1801
	@SuppressWarnings("JUnitMalformedDeclaration")
	static class UserTempDirectoryDeletionDoesNotCauseFailureTestCase {

		@Test
		void deleteTempDir(@TempDir Path tempDir) throws IOException {
			Files.delete(tempDir);
			assertThat(tempDir).doesNotExist();
		}

	}

	// https://github.com/junit-team/junit-framework/issues/2046
	@SuppressWarnings("JUnitMalformedDeclaration")
	static class NonWritableFileDoesNotCauseFailureTestCase {

		@Test
		void createReadonlyFile(@TempDir Path tempDir) throws IOException {
			// Removal of setWritable(false) files might fail (e.g. for Windows)
			// The test verifies that @TempDir is capable of removing of such files
			var path = Files.write(tempDir.resolve("test.txt"), new byte[0]);
			assumeTrue(path.toFile().setWritable(false),
				() -> "Unable to set file " + path + " readonly via .toFile().setWritable(false)");
		}

	}

	// https://github.com/junit-team/junit-framework/issues/2171
	@SuppressWarnings("JUnitMalformedDeclaration")
	static class ReadOnlyFileInReadOnlyDirDoesNotCauseFailureTestCase {

		@Test
		void createReadOnlyFileInReadOnlyDir(@TempDir File tempDir) throws IOException {
			File file = tempDir.toPath().resolve("file").toFile();
			assumeTrue(file.createNewFile());
			assumeTrue(tempDir.setReadOnly());
			assumeTrue(file.setReadOnly());
		}

	}

	// https://github.com/junit-team/junit-framework/issues/2171
	@SuppressWarnings("JUnitMalformedDeclaration")
	static class ReadOnlyFileInDirInReadOnlyDirDoesNotCauseFailureTestCase {

		@Test
		void createReadOnlyFileInReadOnlyDir(@TempDir File tempDir) throws IOException {
			File file = tempDir.toPath().resolve("dir").resolve("file").toFile();
			assumeTrue(file.getParentFile().mkdirs());
			assumeTrue(file.createNewFile());
			assumeTrue(tempDir.setReadOnly());
			assumeTrue(file.getParentFile().setReadOnly());
			assumeTrue(file.setReadOnly());
		}

	}

	// https://github.com/junit-team/junit-framework/issues/2609
	@SuppressWarnings({ "ResultOfMethodCallIgnored", "JUnitMalformedDeclaration" })
	static class NonMintPermissionContentInTempDirectoryDoesNotCauseFailureTestCase {

		@Test
		void createFile(@TempDir Path tempDir) throws IOException {
			Files.createFile(tempDir.resolve("test-file.txt")).toFile();
		}

		@Test
		void createFolder(@TempDir Path tempDir) throws IOException {
			Files.createFile(tempDir.resolve("test-file.txt")).toFile();
		}

		@Test
		void createNonWritableFile(@TempDir Path tempDir) throws IOException {
			Files.createFile(tempDir.resolve("test-file.txt")).toFile().setWritable(false);
		}

		@Test
		void createNonReadableFile(@TempDir Path tempDir) throws IOException {
			Files.createFile(tempDir.resolve("test-file.txt")).toFile().setReadable(false);
		}

		@Test
		void createNonWritableDirectory(@TempDir Path tempDir) throws IOException {
			Files.createDirectory(tempDir.resolve("test-sub-dir")).toFile().setWritable(false);
		}

		@Test
		void createNonReadableDirectory(@TempDir Path tempDir) throws IOException {
			Files.createDirectory(tempDir.resolve("test-sub-dir")).toFile().setReadable(false);
		}

		@Test
		void createNonExecutableDirectory(@TempDir Path tempDir) throws IOException {
			Files.createDirectory(tempDir.resolve("test-sub-dir")).toFile().setExecutable(false);
		}

		@Test
		void createNonEmptyNonWritableDirectory(@TempDir Path tempDir) throws IOException {
			Path subDir = Files.createDirectory(tempDir.resolve("test-sub-dir"));
			Files.createFile(tempDir.resolve("test-sub-dir/test-file.txt"));
			subDir.toFile().setWritable(false);
		}

		@Test
		void createNonEmptyNonReadableDirectory(@TempDir Path tempDir) throws IOException {
			Path subDir = Files.createDirectory(tempDir.resolve("test-sub-dir"));
			Files.createFile(tempDir.resolve("test-sub-dir/test-file.txt"));
			subDir.toFile().setReadable(false);
		}

		@Test
		void createNonEmptyNonExecutableDirectory(@TempDir Path tempDir) throws IOException {
			Path subDir = Files.createDirectory(tempDir.resolve("test-sub-dir"));
			Files.createFile(tempDir.resolve("test-sub-dir/test-file.txt"));
			subDir.toFile().setExecutable(false);
		}

		@Test
		void createNonEmptyDirectory(@TempDir Path tempDir) throws IOException {
			Files.createDirectory(tempDir.resolve("test-sub-dir"));
			Files.createFile(tempDir.resolve("test-sub-dir/test-file.txt"));
		}

		@Test
		void createNonEmptyDirectoryWithNonWritableFile(@TempDir Path tempDir) throws IOException {
			Files.createDirectory(tempDir.resolve("test-sub-dir"));
			Files.createFile(tempDir.resolve("test-sub-dir/test-file.txt")).toFile().setWritable(false);
		}

		@Test
		void createNonEmptyDirectoryWithNonReadableFile(@TempDir Path tempDir) throws IOException {
			Files.createDirectory(tempDir.resolve("test-sub-dir"));
			Files.createFile(tempDir.resolve("test-sub-dir/test-file.txt")).toFile().setReadable(false);
		}
	}

	// https://github.com/junit-team/junit-framework/issues/2609
	@SuppressWarnings("ResultOfMethodCallIgnored")
	static class NonMintTempDirectoryPermissionsDoNotCauseFailureTestCase {

		@Nested
		class NonWritable {

			@Test
			void makeEmptyTempDirectoryNonWritable(@TempDir Path tempDir) {
				tempDir.toFile().setWritable(false);
			}

			@Test
			void makeTempDirectoryWithFileNonWritable(@TempDir Path tempDir) throws IOException {
				Files.createFile(tempDir.resolve("test-file.txt"));
				tempDir.toFile().setWritable(false);
			}

			@Test
			void makeTempDirectoryWithEmptyFolderNonWritable(@TempDir Path tempDir) throws IOException {
				Files.createDirectory(tempDir.resolve("test-sub-dir"));
				tempDir.toFile().setWritable(false);
			}

			@Test
			void makeTempDirectoryWithNonWritableFileNonWritable(@TempDir Path tempDir) throws IOException {
				Files.createFile(tempDir.resolve("test-file.txt")).toFile().setWritable(false);
				tempDir.toFile().setWritable(false);
			}

			@Test
			void makeTempDirectoryWithNonReadableFileNonWritable(@TempDir Path tempDir) throws IOException {
				Files.createFile(tempDir.resolve("test-file.txt")).toFile().setReadable(false);
				tempDir.toFile().setWritable(false);
			}

			@Test
			void makeTempDirectoryWithNonWritableFolderNonWritable(@TempDir Path tempDir) throws IOException {
				Files.createDirectory(tempDir.resolve("test-sub-dir")).toFile().setWritable(false);
				tempDir.toFile().setWritable(false);
			}

			@Test
			void makeTempDirectoryWithNonReadableFolderNonWritable(@TempDir Path tempDir) throws IOException {
				Files.createDirectory(tempDir.resolve("test-sub-dir")).toFile().setReadable(false);
				tempDir.toFile().setWritable(false);
			}

			@Test
			void makeTempDirectoryWithNonExecutableFolderNonWritable(@TempDir Path tempDir) throws IOException {
				Files.createDirectory(tempDir.resolve("test-sub-dir")).toFile().setExecutable(false);
				tempDir.toFile().setWritable(false);
			}

			@Test
			void makeTempDirectoryWithNonEmptyNonReadableFolderNonWritable(@TempDir Path tempDir) throws IOException {
				Path subDir = Files.createDirectory(tempDir.resolve("test-sub-dir"));
				Files.createFile(tempDir.resolve("test-sub-dir/test-file.txt"));
				subDir.toFile().setReadable(false);
				tempDir.toFile().setWritable(false);
			}

			@Test
			void makeTempDirectoryWithNonEmptyNonWritableFolderNonWritable(@TempDir Path tempDir) throws IOException {
				Path subDir = Files.createDirectory(tempDir.resolve("test-sub-dir"));
				Files.createFile(tempDir.resolve("test-sub-dir/test-file.txt"));
				subDir.toFile().setWritable(false);
				tempDir.toFile().setWritable(false);
			}

			@Test
			void makeTempDirectoryWithNonEmptyNonExecutableFolderNonWritable(@TempDir Path tempDir) throws IOException {
				Path subDir = Files.createDirectory(tempDir.resolve("test-sub-dir"));
				Files.createFile(tempDir.resolve("test-sub-dir/test-file.txt"));
				subDir.toFile().setExecutable(false);
				tempDir.toFile().setWritable(false);
			}

			@Test
			void makeTempDirectoryWithNonEmptyFolderNonWritable(@TempDir Path tempDir) throws IOException {
				Files.createDirectory(tempDir.resolve("test-sub-dir"));
				Files.createFile(tempDir.resolve("test-sub-dir/test-file.txt"));
				tempDir.toFile().setWritable(false);
			}

			@Test
			void makeTempDirectoryWithNonEmptyFolderContainingNonWritableFileNonWritable(@TempDir Path tempDir)
					throws IOException {
				Files.createDirectory(tempDir.resolve("test-sub-dir"));
				Files.createFile(tempDir.resolve("test-sub-dir/test-file.txt")).toFile().setWritable(false);
				tempDir.toFile().setWritable(false);
			}

			@Test
			void makeTempDirectoryWithNonEmptyFolderContainingNonReadableFileNonWritable(@TempDir Path tempDir)
					throws IOException {
				Files.createDirectory(tempDir.resolve("test-sub-dir"));
				Files.createFile(tempDir.resolve("test-sub-dir/test-file.txt")).toFile().setReadable(false);
				tempDir.toFile().setWritable(false);
			}
		}

		@Nested
		class NonReadable {

			@Test
			void makeEmptyTempDirectoryNonReadable(@TempDir Path tempDir) {
				tempDir.toFile().setReadable(false);
			}

			@Test
			void makeTempDirectoryWithFileNonReadable(@TempDir Path tempDir) throws IOException {
				Files.createFile(tempDir.resolve("test-file.txt"));
				tempDir.toFile().setReadable(false);
			}

			@Test
			void makeTempDirectoryWithEmptyFolderNonReadable(@TempDir Path tempDir) throws IOException {
				Files.createDirectory(tempDir.resolve("test-sub-dir"));
				tempDir.toFile().setReadable(false);
			}

			@Test
			void makeTempDirectoryWithNonWritableFileNonReadable(@TempDir Path tempDir) throws IOException {
				Files.createFile(tempDir.resolve("test-file.txt")).toFile().setWritable(false);
				tempDir.toFile().setReadable(false);
			}

			@Test
			void makeTempDirectoryWithNonReadableFileNonReadable(@TempDir Path tempDir) throws IOException {
				Files.createFile(tempDir.resolve("test-file.txt")).toFile().setReadable(false);
				tempDir.toFile().setReadable(false);
			}

			@Test
			void makeTempDirectoryWithNonWritableFolderNonReadable(@TempDir Path tempDir) throws IOException {
				Files.createDirectory(tempDir.resolve("test-sub-dir")).toFile().setWritable(false);
				tempDir.toFile().setReadable(false);
			}

			@Test
			void makeTempDirectoryWithNonReadableFolderNonReadable(@TempDir Path tempDir) throws IOException {
				Files.createDirectory(tempDir.resolve("test-sub-dir")).toFile().setReadable(false);
				tempDir.toFile().setReadable(false);
			}

			@Test
			void makeTempDirectoryWithNonExecutableFolderNonReadable(@TempDir Path tempDir) throws IOException {
				Files.createDirectory(tempDir.resolve("test-sub-dir")).toFile().setExecutable(false);
				tempDir.toFile().setReadable(false);
			}

			@Test
			void makeTempDirectoryWithNonEmptyNonWritableFolderNonReadable(@TempDir Path tempDir) throws IOException {
				Path subDir = Files.createDirectory(tempDir.resolve("test-sub-dir"));
				Files.createFile(tempDir.resolve("test-sub-dir/test-file.txt"));
				subDir.toFile().setWritable(false);
				tempDir.toFile().setReadable(false);
			}

			@Test
			void makeTempDirectoryWithNonEmptyNonReadableFolderNonReadable(@TempDir Path tempDir) throws IOException {
				Path subDir = Files.createDirectory(tempDir.resolve("test-sub-dir"));
				Files.createFile(tempDir.resolve("test-sub-dir/test-file.txt"));
				subDir.toFile().setReadable(false);
				tempDir.toFile().setReadable(false);
			}

			@Test
			void makeTempDirectoryWithNonEmptyNonExecutableFolderNonReadable(@TempDir Path tempDir) throws IOException {
				Path subDir = Files.createDirectory(tempDir.resolve("test-sub-dir"));
				Files.createFile(tempDir.resolve("test-sub-dir/test-file.txt"));
				subDir.toFile().setExecutable(false);
				tempDir.toFile().setReadable(false);
			}

			@Test
			void makeTempDirectoryWithNonEmptyFolderNonReadable(@TempDir Path tempDir) throws IOException {
				Files.createDirectory(tempDir.resolve("test-sub-dir"));
				Files.createFile(tempDir.resolve("test-sub-dir/test-file.txt"));
				tempDir.toFile().setReadable(false);
			}

			@Test
			void makeTempDirectoryWithNonEmptyFolderContainingNonWritableFileNonReadable(@TempDir Path tempDir)
					throws IOException {
				Files.createDirectory(tempDir.resolve("test-sub-dir"));
				Files.createFile(tempDir.resolve("test-sub-dir/test-file.txt")).toFile().setWritable(false);
				tempDir.toFile().setReadable(false);
			}

			@Test
			void makeTempDirectoryWithNonEmptyFolderContainingNonReadableFileNonReadable(@TempDir Path tempDir)
					throws IOException {
				Files.createDirectory(tempDir.resolve("test-sub-dir"));
				Files.createFile(tempDir.resolve("test-sub-dir/test-file.txt")).toFile().setReadable(false);
				tempDir.toFile().setReadable(false);
			}
		}

		@Nested
		class NonExecutable {

			@Test
			void makeEmptyTempDirectoryNonExecutable(@TempDir Path tempDir) {
				tempDir.toFile().setExecutable(false);
			}

			@Test
			void makeTempDirectoryWithFileNonExecutable(@TempDir Path tempDir) throws IOException {
				Files.createFile(tempDir.resolve("test-file.txt"));
				tempDir.toFile().setExecutable(false);
			}

			@Test
			void makeTempDirectoryWithEmptyFolderNonExecutable(@TempDir Path tempDir) throws IOException {
				Files.createDirectory(tempDir.resolve("test-sub-dir"));
				tempDir.toFile().setExecutable(false);
			}

			@Test
			void makeTempDirectoryWithNonWritableFileNonExecutable(@TempDir Path tempDir) throws IOException {
				Files.createFile(tempDir.resolve("test-file.txt")).toFile().setWritable(false);
				tempDir.toFile().setExecutable(false);
			}

			@Test
			void makeTempDirectoryWithNonReadableFileNonExecutable(@TempDir Path tempDir) throws IOException {
				Files.createFile(tempDir.resolve("test-file.txt")).toFile().setReadable(false);
				tempDir.toFile().setExecutable(false);
			}

			@Test
			void makeTempDirectoryWithNonWritableFolderNonExecutable(@TempDir Path tempDir) throws IOException {
				Files.createDirectory(tempDir.resolve("test-sub-dir")).toFile().setWritable(false);
				tempDir.toFile().setExecutable(false);
			}

			@Test
			void makeTempDirectoryWithNonReadableFolderNonExecutable(@TempDir Path tempDir) throws IOException {
				Files.createDirectory(tempDir.resolve("test-sub-dir")).toFile().setReadable(false);
				tempDir.toFile().setExecutable(false);
			}

			@Test
			void makeTempDirectoryWithNonExecutableFolderNonExecutable(@TempDir Path tempDir) throws IOException {
				Files.createDirectory(tempDir.resolve("test-sub-dir")).toFile().setExecutable(false);
				tempDir.toFile().setExecutable(false);
			}

			@Test
			void makeTempDirectoryWithNonEmptyNonWritableFolderNonExecutable(@TempDir Path tempDir) throws IOException {
				Path subDir = Files.createDirectory(tempDir.resolve("test-sub-dir"));
				Files.createFile(tempDir.resolve("test-sub-dir/test-file.txt"));
				subDir.toFile().setWritable(false);
				tempDir.toFile().setExecutable(false);
			}

			@Test
			void makeTempDirectoryWithNonEmptyNonReadableFolderNonExecutable(@TempDir Path tempDir) throws IOException {
				Path subDir = Files.createDirectory(tempDir.resolve("test-sub-dir"));
				Files.createFile(tempDir.resolve("test-sub-dir/test-file.txt"));
				subDir.toFile().setReadable(false);
				tempDir.toFile().setExecutable(false);
			}

			@Test
			void makeTempDirectoryWithNonEmptyNonExecutableFolderNonExecutable(@TempDir Path tempDir)
					throws IOException {
				Path subDir = Files.createDirectory(tempDir.resolve("test-sub-dir"));
				Files.createFile(tempDir.resolve("test-sub-dir/test-file.txt"));
				subDir.toFile().setExecutable(false);
				tempDir.toFile().setExecutable(false);
			}

			@Test
			void makeTempDirectoryWithNonEmptyFolderNonExecutable(@TempDir Path tempDir) throws IOException {
				Files.createDirectory(tempDir.resolve("test-sub-dir"));
				Files.createFile(tempDir.resolve("test-sub-dir/test-file.txt"));
				tempDir.toFile().setExecutable(false);
			}

			@Test
			void makeTempDirectoryWithNonEmptyFolderContainingNonWritableFileNonExecutable(@TempDir Path tempDir)
					throws IOException {
				Files.createDirectory(tempDir.resolve("test-sub-dir"));
				Files.createFile(tempDir.resolve("test-sub-dir/test-file.txt")).toFile().setWritable(false);
				tempDir.toFile().setExecutable(false);
			}

			@Test
			void makeTempDirectoryWithNonEmptyFolderContainingNonReadableFileNonExecutable(@TempDir Path tempDir)
					throws IOException {
				Files.createDirectory(tempDir.resolve("test-sub-dir"));
				Files.createFile(tempDir.resolve("test-sub-dir/test-file.txt")).toFile().setReadable(false);
				tempDir.toFile().setExecutable(false);
			}
		}
	}

	// https://github.com/junit-team/junit-framework/issues/2079
	@SuppressWarnings("JUnitMalformedDeclaration")
	static class TempDirUsageInsideNestedClassesTestCase {

		@TempDir
		File tempDir;

		@Test
		void topLevel() {
			assertNotNull(tempDir);
			assertTrue(tempDir.exists());
		}

		@Nested
		class NestedTestClass {

			@Test
			void nested() {
				assertNotNull(tempDir);
				assertTrue(tempDir.exists());
			}

			@Nested
			class EvenDeeperNestedTestClass {

				@Test
				void deeplyNested() {
					assertNotNull(tempDir);
					assertTrue(tempDir.exists());
				}
			}
		}
	}

	@SuppressWarnings("JUnitMalformedDeclaration")
	@NullUnmarked
	static class StaticTempDirUsageInsideNestedClassTestCase {

		@TempDir
		static File tempDir;

		static File initialTempDir;

		@Test
		void topLevel() {
			assertNotNull(tempDir);
			assertTrue(tempDir.exists());
			initialTempDir = tempDir;
		}

		@Nested
		class NestedTestClass {

			@Test
			void nested() {
				assertNotNull(tempDir);
				assertTrue(tempDir.exists());
				assertSame(initialTempDir, tempDir);
			}
		}
	}

	@SuppressWarnings("JUnitMalformedDeclaration")
	@DisplayName("class")
	static class AllPossibleDeclarationLocationsTestCase {

		static final Map<String, Map<String, Path>> tempDirs = new HashMap<>();

		@TempDir
		static Path staticField1;

		@TempDir
		static Path staticField2;

		final Path instanceFieldSetViaConstructorInjection;

		@TempDir
		Path instanceField1;

		@TempDir
		Path instanceField2;

		@BeforeAll
		static void beforeAll(@TempDir Path param1, @TempDir Path param2, TestInfo testInfo) {
			getTempDirs(testInfo).putAll(Map.of( //
				"staticField1", staticField1, //
				"staticField2", staticField2, //
				"beforeAll1", param1, //
				"beforeAll2", param2 //
			));
			assertAllTempDirsExist(testInfo);
		}

		AllPossibleDeclarationLocationsTestCase(@TempDir Path tempDir) {
			this.instanceFieldSetViaConstructorInjection = tempDir;
		}

		@BeforeEach
		void beforeEach(@TempDir Path param1, @TempDir Path param2, TestInfo testInfo) {
			getTempDirs(testInfo).putAll(Map.of( //
				"staticField1", staticField1, //
				"staticField2", staticField2, //
				"instanceFieldSetViaConstructorInjection", instanceFieldSetViaConstructorInjection, //
				"instanceField1", instanceField1, //
				"instanceField2", instanceField2, //
				"beforeEach1", param1, //
				"beforeEach2", param2 //
			));
			assertAllTempDirsExist(testInfo);
		}

		@Test
		@DisplayName("testA")
		void testA(@TempDir Path param1, @TempDir Path param2, TestInfo testInfo) {
			getTempDirs(testInfo).putAll(Map.of( //
				"test1", param1, //
				"test2", param2 //
			));
			assertAllTempDirsExist(testInfo);
		}

		@Test
		@DisplayName("testB")
		void testB(@TempDir Path param1, @TempDir Path param2, TestInfo testInfo) {
			getTempDirs(testInfo).putAll(Map.of( //
				"test1", param1, //
				"test2", param2 //
			));
			assertAllTempDirsExist(testInfo);
		}

		@AfterEach
		void afterEach(@TempDir Path param1, @TempDir Path param2, TestInfo testInfo) {
			getTempDirs(testInfo).putAll(Map.of( //
				"afterEach1", param1, //
				"afterEach2", param2 //
			));
			assertAllTempDirsExist(testInfo);
		}

		@AfterAll
		static void afterAll(@TempDir Path param1, @TempDir Path param2, TestInfo testInfo) {
			getTempDirs(testInfo).putAll(Map.of( //
				"afterAll1", param1, //
				"afterAll2", param2 //
			));
			assertAllTempDirsExist(testInfo);
		}

		private static Map<String, Path> getTempDirs(TestInfo testInfo) {
			return tempDirs.computeIfAbsent(testInfo.getDisplayName(), __ -> new LinkedHashMap<>());
		}

		private static void assertAllTempDirsExist(TestInfo testInfo) {
			assertAll(getTempDirs(testInfo).values().stream().map(tempDir -> () -> assertTrue(Files.exists(tempDir))));
		}
	}

	static class UndeletableTestCase {

		static final Path UNDELETABLE_PATH = Path.of("undeletable");
		static final String TEMP_DIR = "TEMP_DIR";

		@RegisterExtension
		BeforeEachCallback injector = context -> context //
				.getStore(TempDirectory.NAMESPACE) //
				.put(TempDirectory.FILE_OPERATIONS_KEY, (FileOperations) path -> {
					if (path.endsWith(UNDELETABLE_PATH)) {
						throw new IOException("Simulated failure");
					}
					else {
						Files.delete(path);
					}
				});

		@TempDir
		Path tempDir;

		@BeforeEach
		void reportTempDir(TestReporter reporter) {
			reporter.publishEntry(TEMP_DIR, tempDir.toString());
		}
	}

	@SuppressWarnings("JUnitMalformedDeclaration")
	static class UndeletableDirectoryTestCase extends UndeletableTestCase {
		@Test
		void test() throws Exception {
			Files.createDirectory(tempDir.resolve(UNDELETABLE_PATH));
		}
	}

	@SuppressWarnings("JUnitMalformedDeclaration")
	static class UndeletableFileTestCase extends UndeletableTestCase {
		@Test
		void test() throws Exception {
			Files.createFile(tempDir.resolve(UNDELETABLE_PATH));
		}
	}

	@SuppressWarnings("JUnitMalformedDeclaration")
	static class FactoryWithTestMethodNameAsPrefixTestCase {

		@Test
		void test(@TempDir(factory = Factory.class) Path tempDir) {
			assertTrue(Files.exists(tempDir));
			assertThat(tempDir.getFileName()).asString().startsWith("test");
		}

		private static class Factory implements TempDirFactory {

			@Override
			public Path createTempDirectory(AnnotatedElementContext elementContext, ExtensionContext extensionContext)
					throws Exception {
				return Files.createTempDirectory(extensionContext.getRequiredTestMethod().getName());
			}
		}

	}

	// https://github.com/junit-team/junit-framework/issues/2088
	@SuppressWarnings("JUnitMalformedDeclaration")
	static class FactoryWithCustomParentDirectoryTestCase {

		@Test
		void test(@TempDir(factory = Factory.class) Path tempDir) {
			assertThat(tempDir).exists().hasParent(Factory.parent);
			assertThat(tempDir.getFileName()).asString().startsWith("prefix");
		}

		private static class Factory implements TempDirFactory {

			@Nullable
			private static Path parent;

			private Factory() throws IOException {
				parent = Files.createTempDirectory("parent");
			}

			@Override
			public Path createTempDirectory(AnnotatedElementContext elementContext, ExtensionContext extensionContext)
					throws Exception {
				return Files.createTempDirectory(requireNonNull(parent), "prefix");
			}
		}

	}

	@SuppressWarnings("JUnitMalformedDeclaration")
	static class FactoryWithMemoryFileSystemTestCase {

		@Test
		void test(@TempDir(factory = Factory.class) Path tempDir) {
			assertThat(tempDir).exists().hasFileSystem(Factory.fileSystem);
			assertThat(tempDir.getFileName()).asString().startsWith("prefix");
		}

		private static class Factory implements TempDirFactory {

			@Nullable
			private static FileSystem fileSystem;

			@Override
			public Path createTempDirectory(AnnotatedElementContext elementContext, ExtensionContext extensionContext)
					throws Exception {
				fileSystem = MemoryFileSystemBuilder.newEmpty().build();
				return Files.createTempDirectory(fileSystem.getPath("/"), "prefix");
			}

			@Override
			public void close() throws IOException {
				requireNonNull(fileSystem).close();
				fileSystem = null;
			}
		}

	}

	@SuppressWarnings("JUnitMalformedDeclaration")
	static class FactoryWithJimfsTestCase {

		@Test
		void test(@TempDir(factory = Factory.class) Path tempDir) {
			assertThat(tempDir).exists().hasFileSystem(Factory.fileSystem);
			assertThat(tempDir.getFileName()).asString().startsWith("prefix");
		}

		private static class Factory implements TempDirFactory {

			@Nullable
			private static FileSystem fileSystem;

			@Override
			public Path createTempDirectory(AnnotatedElementContext elementContext, ExtensionContext extensionContext)
					throws Exception {
				fileSystem = Jimfs.newFileSystem(Configuration.unix());
				return Files.createTempDirectory(fileSystem.getPath("/"), "prefix");
			}

			@Override
			public void close() throws IOException {
				requireNonNull(fileSystem).close();
				fileSystem = null;
			}
		}

	}

	@SuppressWarnings("JUnitMalformedDeclaration")
	static class FactoryWithAnnotatedElementNameAsPrefixTestCase {

		@TempDir(factory = Factory.class)
		private Path tempDir1;

		@Test
		void test(@TempDir(factory = Factory.class) Path tempDir2) {
			assertThat(tempDir1.getFileName()).asString().startsWith("tempDir1");
			assertThat(tempDir2.getFileName()).asString().startsWith("tempDir2");
		}

		private static class Factory implements TempDirFactory {

			@Override
			public Path createTempDirectory(AnnotatedElementContext elementContext, ExtensionContext extensionContext)
					throws Exception {
				return Files.createTempDirectory(getName(elementContext.getAnnotatedElement()));
			}

			private static String getName(AnnotatedElement element) {
				return element instanceof Field field ? field.getName() : ((Parameter) element).getName();
			}

		}

	}

	@SuppressWarnings("JUnitMalformedDeclaration")
	@NullUnmarked
	static class FactoryWithCustomMetaAnnotationTestCase {

		@TempDirForField
		private Path tempDir1;

		@Test
		void test(@TempDirForParameter Path tempDir2) {
			assertThat(tempDir1.getFileName()).asString().startsWith("field");
			assertThat(tempDir2.getFileName()).asString().startsWith("parameter");
		}

		@Target(ANNOTATION_TYPE)
		@Retention(RUNTIME)
		@TempDir(factory = FactoryWithCustomMetaAnnotationTestCase.Factory.class)
		private @interface TempDirWithPrefix {

			String value();

		}

		@Target(FIELD)
		@Retention(RUNTIME)
		@TempDirWithPrefix("field")
		private @interface TempDirForField {
		}

		@Target(PARAMETER)
		@Retention(RUNTIME)
		@TempDirWithPrefix("parameter")
		private @interface TempDirForParameter {
		}

		private static class Factory implements TempDirFactory {

			@Override
			public Path createTempDirectory(AnnotatedElementContext elementContext, ExtensionContext extensionContext)
					throws Exception {
				String prefix = elementContext.findAnnotation(TempDirWithPrefix.class) //
						.map(TempDirWithPrefix::value).orElseThrow();
				return Files.createTempDirectory(prefix);
			}

		}

	}

	@SuppressWarnings("JUnitMalformedDeclaration")
	static class FactoryNotReturningDirectoryTestCase {

		@Test
		void test(@SuppressWarnings("unused") @TempDir(factory = Factory.class) Path tempDir) {
			// never called
		}

		@SuppressWarnings("DataFlowIssue")
		private static class Factory implements TempDirFactory {

			@Override
			public Path createTempDirectory(AnnotatedElementContext elementContext, ExtensionContext extensionContext) {
				return null;
			}
		}

	}

	@SuppressWarnings("JUnitMalformedDeclaration")
	static class FactoryReturningNonDefaultFileSystemPathForFileAnnotatedElementTestCase {

		@Test
		void test(@SuppressWarnings("unused") @TempDir(factory = Factory.class) File tempDir) {
			// never called
		}

		private static class Factory implements TempDirFactory {

			private final FileSystem fileSystem = Jimfs.newFileSystem(Configuration.unix());

			@Override
			public Path createTempDirectory(AnnotatedElementContext elementContext, ExtensionContext extensionContext)
					throws Exception {
				return Files.createTempDirectory(fileSystem.getPath("/"), "prefix");
			}

			@Override
			public void close() throws IOException {
				fileSystem.close();
			}
		}

	}

	@SuppressWarnings("JUnitMalformedDeclaration")
	static class StandardDefaultFactoryTestCase {

		@Test
		void test(@TempDir Path tempDir1, @TempDir Path tempDir2) {
			assertNotSame(tempDir1, tempDir2);
			assertThat(tempDir1.getFileName()).asString().startsWith("junit");
			assertThat(tempDir2.getFileName()).asString().startsWith("junit");
		}

	}

	@SuppressWarnings("JUnitMalformedDeclaration")
	static class CustomDefaultFactoryTestCase {

		@Test
		void test(@TempDir Path tempDir1, @TempDir Path tempDir2) {
			assertNotSame(tempDir1, tempDir2);
			assertThat(tempDir1.getFileName()).asString().startsWith("custom");
			assertThat(tempDir2.getFileName()).asString().startsWith("custom");
		}

	}

	@SuppressWarnings("JUnitMalformedDeclaration")
	static class CustomDefaultFactoryWithStandardDeclarationTestCase {

		@Test
		void test(@TempDir Path tempDir1, @TempDir(factory = Standard.class) Path tempDir2) {
			assertNotSame(tempDir1, tempDir2);
			assertThat(tempDir1.getFileName()).asString().startsWith("custom");
			assertThat(tempDir2.getFileName()).asString().startsWith("junit");
		}

	}

	@SuppressWarnings("JUnitMalformedDeclaration")
	static class CustomDefaultFactoryNotReturningDirectoryTestCase {

		@Test
		void test(@SuppressWarnings("unused") @TempDir Path tempDir) {
			// never called
		}

	}

	@SuppressWarnings("JUnitMalformedDeclaration")
	record TempDirRecordTestCase(@TempDir Path tempDir) {
		@Test
		void shouldExists() {
			assertTrue(Files.exists(tempDir));
		}
	}

}
