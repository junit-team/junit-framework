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

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.fail;
import static org.junit.jupiter.api.MediaType.TEXT_PLAIN;
import static org.junit.jupiter.api.MediaType.TEXT_PLAIN_UTF_8;
import static org.junit.jupiter.api.Named.named;
import static org.junit.jupiter.api.TestInstance.Lifecycle.PER_METHOD;
import static org.junit.platform.commons.test.PreconditionAssertions.assertPreconditionViolationFor;
import static org.junit.platform.commons.test.PreconditionAssertions.assertPreconditionViolationNotNullOrBlankFor;
import static org.junit.platform.launcher.core.OutputDirectoryCreators.dummyOutputDirectoryCreator;
import static org.junit.platform.launcher.core.OutputDirectoryCreators.hierarchicalOutputDirectoryCreator;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;

import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Named;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.Extension;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.ExtensionContext.Namespace;
import org.junit.jupiter.api.extension.PreInterruptCallback;
import org.junit.jupiter.api.function.ThrowingConsumer;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.api.parallel.ExecutionMode;
import org.junit.jupiter.engine.config.DefaultJupiterConfiguration;
import org.junit.jupiter.engine.config.JupiterConfiguration;
import org.junit.jupiter.engine.execution.DefaultTestInstances;
import org.junit.jupiter.engine.execution.LauncherStoreFacade;
import org.junit.jupiter.engine.extension.ExtensionRegistry;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import org.junit.platform.commons.support.ReflectionSupport;
import org.junit.platform.engine.ConfigurationParameters;
import org.junit.platform.engine.EngineExecutionListener;
import org.junit.platform.engine.TestDescriptor;
import org.junit.platform.engine.UniqueId;
import org.junit.platform.engine.reporting.FileEntry;
import org.junit.platform.engine.reporting.ReportEntry;
import org.junit.platform.engine.support.hierarchical.OpenTest4JAwareThrowableCollector;
import org.junit.platform.launcher.core.NamespacedHierarchicalStoreProviders;
import org.mockito.ArgumentCaptor;

/**
 * Unit tests for concrete implementations of {@link ExtensionContext}:
 * {@link JupiterEngineExtensionContext}, {@link ClassExtensionContext}, and
 * {@link MethodExtensionContext}.
 *
 * @since 5.0
 */
public class ExtensionContextTests {

	private static final ThrowingConsumer<Path> failingAction = __ -> fail("should not be called");

	private final JupiterConfiguration configuration = mock();
	private final ExtensionRegistry extensionRegistry = mock();
	private final LauncherStoreFacade launcherStoreFacade = new LauncherStoreFacade(
		NamespacedHierarchicalStoreProviders.dummyNamespacedHierarchicalStore());

	@BeforeEach
	void setUp() {
		when(configuration.getDefaultDisplayNameGenerator()).thenReturn(new DisplayNameGenerator.Standard());
		when(configuration.getDefaultExecutionMode()).thenReturn(ExecutionMode.SAME_THREAD);
		when(configuration.getDefaultClassesExecutionMode()).thenReturn(ExecutionMode.SAME_THREAD);
		when(configuration.getOutputDirectoryCreator()).thenReturn(dummyOutputDirectoryCreator());
	}

	@Test
	void fromJupiterEngineDescriptor() {
		var engineTestDescriptor = new JupiterEngineDescriptor(UniqueId.root("engine", "junit-jupiter"), configuration);

		try (var engineContext = new JupiterEngineExtensionContext(mock(), engineTestDescriptor, configuration,
			extensionRegistry, launcherStoreFacade)) {
			// @formatter:off
			assertAll("engineContext",
				() -> assertThat(engineContext.getElement()).isEmpty(),
				() -> assertThat(engineContext.getTestClass()).isEmpty(),
				() -> assertThat(engineContext.getTestInstance()).isEmpty(),
				() -> assertThat(engineContext.getTestMethod()).isEmpty(),
				() -> assertPreconditionViolationFor(engineContext::getRequiredTestClass),
				() -> assertPreconditionViolationFor(engineContext::getRequiredTestInstance),
				() -> assertPreconditionViolationFor(engineContext::getRequiredTestMethod),
				() -> assertThat(engineContext.getDisplayName()).isEqualTo(engineTestDescriptor.getDisplayName()),
				() -> assertThat(engineContext.getParent()).isEmpty(),
				() -> assertThat(engineContext.getRoot()).isSameAs(engineContext),
				() -> assertThat(engineContext.getExecutionMode()).isEqualTo(ExecutionMode.SAME_THREAD),
				() -> assertThat(engineContext.getExtensions(PreInterruptCallback.class)).isEmpty()
			);
		// @formatter:on
		}
	}

	@Test
	@SuppressWarnings("resource")
	void fromClassTestDescriptor() {
		var nestedClassDescriptor = nestedClassDescriptor();
		var outerClassDescriptor = outerClassDescriptor(nestedClassDescriptor);
		var doublyNestedClassDescriptor = doublyNestedClassDescriptor();
		var methodTestDescriptor = nestedMethodDescriptor();
		nestedClassDescriptor.addChild(doublyNestedClassDescriptor);
		nestedClassDescriptor.addChild(methodTestDescriptor);

		var parentExtensionContext = mock(AbstractExtensionContext.class);

		var outerExtensionContext = new ClassExtensionContext(parentExtensionContext, mock(), outerClassDescriptor,
			PER_METHOD, configuration, extensionRegistry, launcherStoreFacade, mock());

		// @formatter:off
		assertAll("outerContext",
			() -> assertThat(outerExtensionContext.getElement()).contains(OuterClassTestCase.class),
			() -> assertThat(outerExtensionContext.getTestClass()).contains(OuterClassTestCase.class),
			() -> assertThat(outerExtensionContext.getTestInstance()).isEmpty(),
			() -> assertThat(outerExtensionContext.getTestMethod()).isEmpty(),
			() -> assertThat(outerExtensionContext.getRequiredTestClass()).isEqualTo(OuterClassTestCase.class),
			() -> assertPreconditionViolationFor(outerExtensionContext::getRequiredTestInstance),
			() -> assertPreconditionViolationFor(outerExtensionContext::getRequiredTestMethod),
			() -> assertThat(outerExtensionContext.getDisplayName()).isEqualTo(outerClassDescriptor.getDisplayName()),
			() -> assertThat(outerExtensionContext.getParent()).containsSame(parentExtensionContext),
			() -> assertThat(outerExtensionContext.getExecutionMode()).isEqualTo(ExecutionMode.SAME_THREAD),
			() -> assertThat(outerExtensionContext.getExtensions(PreInterruptCallback.class)).isEmpty(),
			() -> assertThat(outerExtensionContext.getEnclosingTestClasses()).isEmpty()
		);
		// @formatter:on

		var nestedExtensionContext = new ClassExtensionContext(outerExtensionContext, mock(), nestedClassDescriptor,
			PER_METHOD, configuration, extensionRegistry, launcherStoreFacade, mock());
		// @formatter:off
		assertAll("nestedContext",
			() -> assertThat(nestedExtensionContext.getParent()).containsSame(outerExtensionContext),
			() -> assertThat(nestedExtensionContext.getTestClass()).contains(OuterClassTestCase.NestedClass.class),
			() -> assertThat(nestedExtensionContext.getEnclosingTestClasses()).containsExactly(OuterClassTestCase.class)
		);
		// @formatter:on

		var doublyNestedExtensionContext = new ClassExtensionContext(nestedExtensionContext, mock(),
			doublyNestedClassDescriptor, PER_METHOD, configuration, extensionRegistry, launcherStoreFacade, mock());
		// @formatter:off
		assertAll("doublyNestedContext",
				() -> assertThat(doublyNestedExtensionContext.getParent()).containsSame(nestedExtensionContext),
				() -> assertThat(doublyNestedExtensionContext.getTestClass()).contains(OuterClassTestCase.NestedClass.DoublyNestedClass.class),
				() -> assertThat(doublyNestedExtensionContext.getEnclosingTestClasses()).containsExactly(OuterClassTestCase.class, OuterClassTestCase.NestedClass.class)
		);
		// @formatter:on

		var methodExtensionContext = new MethodExtensionContext(nestedExtensionContext, mock(), methodTestDescriptor,
			configuration, extensionRegistry, launcherStoreFacade, new OpenTest4JAwareThrowableCollector());
		// @formatter:off
		assertAll("methodContext",
				() -> assertThat(methodExtensionContext.getParent()).containsSame(nestedExtensionContext),
				() -> assertThat(methodExtensionContext.getTestClass()).contains(OuterClassTestCase.NestedClass.class),
				() -> assertThat(methodExtensionContext.getEnclosingTestClasses()).containsExactly(OuterClassTestCase.class)
		);
		// @formatter:on
	}

	@Test
	void ExtensionContext_With_ExtensionRegistry_getExtensions() {
		var classTestDescriptor = nestedClassDescriptor();
		try (var ctx = new ClassExtensionContext(mock(AbstractExtensionContext.class), mock(), classTestDescriptor,
			PER_METHOD, configuration, extensionRegistry, launcherStoreFacade, mock())) {

			Extension ext = mock();
			when(extensionRegistry.getExtensions(Extension.class)).thenReturn(List.of(ext));

			assertThat(ctx.getExtensions(Extension.class)).isEqualTo(List.of(ext));
		}
	}

	@Test
	@SuppressWarnings("resource")
	void tagsCanBeRetrievedInExtensionContext() {
		var nestedClassDescriptor = nestedClassDescriptor();
		var outerClassDescriptor = outerClassDescriptor(nestedClassDescriptor);
		var methodTestDescriptor = methodDescriptor();
		outerClassDescriptor.addChild(methodTestDescriptor);

		var rootExtensionContext = new JupiterEngineExtensionContext(mock(), mock(), configuration, extensionRegistry,
			launcherStoreFacade);
		assertThat(rootExtensionContext.getRoot()).isSameAs(rootExtensionContext);

		var outerExtensionContext = new ClassExtensionContext(rootExtensionContext, mock(), outerClassDescriptor,
			PER_METHOD, configuration, extensionRegistry, launcherStoreFacade, mock());

		assertThat(outerExtensionContext.getTags()).containsExactly("outer-tag");
		assertThat(outerExtensionContext.getRoot()).isSameAs(rootExtensionContext);

		var nestedExtensionContext = new ClassExtensionContext(outerExtensionContext, mock(), nestedClassDescriptor,
			PER_METHOD, configuration, extensionRegistry, launcherStoreFacade, mock());
		assertThat(nestedExtensionContext.getTags()).containsExactlyInAnyOrder("outer-tag", "nested-tag");
		assertThat(nestedExtensionContext.getRoot()).isSameAs(rootExtensionContext);

		var methodExtensionContext = new MethodExtensionContext(outerExtensionContext, mock(), methodTestDescriptor,
			configuration, extensionRegistry, launcherStoreFacade, new OpenTest4JAwareThrowableCollector());
		methodExtensionContext.setTestInstances(DefaultTestInstances.of(new OuterClassTestCase()));
		assertThat(methodExtensionContext.getTags()).containsExactlyInAnyOrder("outer-tag", "method-tag");
		assertThat(methodExtensionContext.getRoot()).isSameAs(rootExtensionContext);
	}

	@Test
	@SuppressWarnings("resource")
	void fromMethodTestDescriptor() {
		var methodTestDescriptor = methodDescriptor();
		var classTestDescriptor = outerClassDescriptor(methodTestDescriptor);
		var engineDescriptor = new JupiterEngineDescriptor(UniqueId.forEngine("junit-jupiter"), configuration);
		engineDescriptor.addChild(classTestDescriptor);

		Object testInstance = new OuterClassTestCase();
		var testMethod = methodTestDescriptor.getTestMethod();

		var engineExtensionContext = new JupiterEngineExtensionContext(mock(), engineDescriptor, configuration,
			extensionRegistry, launcherStoreFacade);
		var classExtensionContext = new ClassExtensionContext(engineExtensionContext, mock(), classTestDescriptor,
			PER_METHOD, configuration, extensionRegistry, launcherStoreFacade, mock());
		var methodExtensionContext = new MethodExtensionContext(classExtensionContext, mock(), methodTestDescriptor,
			configuration, extensionRegistry, launcherStoreFacade, new OpenTest4JAwareThrowableCollector());
		methodExtensionContext.setTestInstances(DefaultTestInstances.of(testInstance));

		// @formatter:off
		assertAll("methodContext",
			() -> assertThat(methodExtensionContext.getElement()).contains(testMethod),
			() -> assertThat(methodExtensionContext.getTestClass()).contains(OuterClassTestCase.class),
			() -> assertThat(methodExtensionContext.getEnclosingTestClasses()).isEmpty(),
			() -> assertThat(methodExtensionContext.getTestInstance()).contains(testInstance),
			() -> assertThat(methodExtensionContext.getTestMethod()).contains(testMethod),
			() -> assertThat(methodExtensionContext.getRequiredTestClass()).isEqualTo(OuterClassTestCase.class),
			() -> assertThat(methodExtensionContext.getRequiredTestInstance()).isEqualTo(testInstance),
			() -> assertThat(methodExtensionContext.getRequiredTestMethod()).isEqualTo(testMethod),
			() -> assertThat(methodExtensionContext.getDisplayName()).isEqualTo(methodTestDescriptor.getDisplayName()),
			() -> assertThat(methodExtensionContext.getParent()).contains(classExtensionContext),
			() -> assertThat(methodExtensionContext.getRoot()).isSameAs(engineExtensionContext),
			() -> assertThat(methodExtensionContext.getExecutionMode()).isEqualTo(ExecutionMode.SAME_THREAD)
		);
		// @formatter:on
	}

	@Test
	@SuppressWarnings("resource")
	void reportEntriesArePublishedToExecutionListener() {
		var classTestDescriptor = outerClassDescriptor(mock());
		var engineExecutionListener = spy(EngineExecutionListener.class);
		ExtensionContext extensionContext = new ClassExtensionContext(mock(AbstractExtensionContext.class),
			engineExecutionListener, classTestDescriptor, PER_METHOD, configuration, extensionRegistry,
			launcherStoreFacade, mock());

		var map1 = Map.of("key", "value");
		var map2 = Map.of("other key", "other value");

		extensionContext.publishReportEntry(map1);
		extensionContext.publishReportEntry(map2);
		extensionContext.publishReportEntry("3rd key", "third value");
		extensionContext.publishReportEntry("status message");

		var entryCaptor = ArgumentCaptor.forClass(ReportEntry.class);
		verify(engineExecutionListener, times(4)) //
				.reportingEntryPublished(eq(classTestDescriptor), entryCaptor.capture());

		var reportEntry1 = entryCaptor.getAllValues().get(0);
		var reportEntry2 = entryCaptor.getAllValues().get(1);
		var reportEntry3 = entryCaptor.getAllValues().get(2);
		var reportEntry4 = entryCaptor.getAllValues().get(3);

		assertEquals(map1, reportEntry1.getKeyValuePairs());
		assertEquals(map2, reportEntry2.getKeyValuePairs());
		assertEquals("third value", reportEntry3.getKeyValuePairs().get("3rd key"));
		assertEquals("status message", reportEntry4.getKeyValuePairs().get("value"));
	}

	@Test
	void fileEntriesArePublishedToExecutionListener(@TempDir Path tempDir) {
		var engineExecutionListener = mock(EngineExecutionListener.class);
		var classTestDescriptor = outerClassDescriptor(null);
		var extensionContext = createExtensionContextForFilePublishing(tempDir, engineExecutionListener,
			classTestDescriptor);

		extensionContext.publishFile("test1.txt", TEXT_PLAIN_UTF_8, file -> Files.writeString(file, "Test 1"));
		extensionContext.publishDirectory("test2", dir -> {
			Files.writeString(dir.resolve("nested1.txt"), "Nested content 1");
			Files.writeString(dir.resolve("nested2.txt"), "Nested content 2");
		});

		var entryCaptor = ArgumentCaptor.forClass(FileEntry.class);
		verify(engineExecutionListener, times(2)) //
				.fileEntryPublished(eq(classTestDescriptor), entryCaptor.capture());
		var fileEntries = entryCaptor.getAllValues();

		var fileEntry1 = fileEntries.getFirst();
		assertThat(fileEntry1.getPath()).isEqualTo(tempDir.resolve("OuterClass/test1.txt"));
		assertThat(fileEntry1.getMediaType()).contains(TEXT_PLAIN_UTF_8.toString());

		var fileEntry2 = fileEntries.get(1);
		assertThat(fileEntry2.getPath()).isEqualTo(tempDir.resolve("OuterClass/test2"));
		assertThat(fileEntry2.getMediaType()).isEmpty();
		assertThat(fileEntry2.getPath().resolve("nested1.txt")).usingCharset(UTF_8).hasContent("Nested content 1");
		assertThat(fileEntry2.getPath().resolve("nested2.txt")).usingCharset(UTF_8).hasContent("Nested content 2");
	}

	@ParameterizedTest
	@NullAndEmptySource
	@ValueSource(strings = { " ", " \t " })
	@SuppressWarnings("DataFlowIssue") // publishFile() parameters are not @Nullable
	void failsWhenAttemptingToPublishFileWithNullOrBlankName(@Nullable String name, @TempDir Path tempDir) {
		var extensionContext = createExtensionContextForFilePublishing(tempDir);

		assertPreconditionViolationNotNullOrBlankFor("name",
			() -> extensionContext.publishFile(name, TEXT_PLAIN, failingAction));
	}

	@ParameterizedTest
	@NullAndEmptySource
	@ValueSource(strings = { " ", " \t " })
	@SuppressWarnings("DataFlowIssue") // publishDirectory() parameters are not @Nullable
	void failsWhenAttemptingToPublishDirectoryWithNullOrBlankName(@Nullable String name, @TempDir Path tempDir) {
		var extensionContext = createExtensionContextForFilePublishing(tempDir);

		assertPreconditionViolationNotNullOrBlankFor("name",
			() -> extensionContext.publishDirectory(name, failingAction));
	}

	@Test
	void failsWhenAttemptingToPublishFileWithPathSeparators(@TempDir Path tempDir) {
		var extensionContext = createExtensionContextForFilePublishing(tempDir);
		var name = "test" + File.separator + "subDir";

		assertPreconditionViolationFor(() -> extensionContext.publishFile(name, TEXT_PLAIN, failingAction))//
				.withMessage("name must not contain path separators: " + name);
	}

	@Test
	void failsWhenAttemptingToPublishDirectoryWithPathSeparators(@TempDir Path tempDir) {
		var extensionContext = createExtensionContextForFilePublishing(tempDir);
		var name = "test" + File.separator + "subDir";

		assertPreconditionViolationFor(() -> extensionContext.publishDirectory(name, failingAction))//
				.withMessage("name must not contain path separators: " + name);
	}

	@Test
	void failsWhenAttemptingToPublishMissingFiles(@TempDir Path tempDir) {
		var extensionContext = createExtensionContextForFilePublishing(tempDir);

		assertPreconditionViolationFor(() -> extensionContext.publishFile("test", TEXT_PLAIN, Files::deleteIfExists)) //
				.withMessage("Published path must exist: " + tempDir.resolve("OuterClass").resolve("test"));
	}

	@Test
	void failsWhenAttemptingToPublishMissingDirectory(@TempDir Path tempDir) {
		var extensionContext = createExtensionContextForFilePublishing(tempDir);

		assertPreconditionViolationFor(() -> extensionContext.publishDirectory("test", Files::delete)) //
				.withMessage("Published path must exist: " + tempDir.resolve("OuterClass").resolve("test"));
	}

	@Test
	void failsWhenAttemptingToPublishDirectoriesAsRegularFiles(@TempDir Path tempDir) {
		var extensionContext = createExtensionContextForFilePublishing(tempDir);

		assertPreconditionViolationFor(() -> extensionContext.publishFile("test", TEXT_PLAIN, Files::createDirectory))//
				.withMessage("Published path must be a regular file: " + tempDir.resolve("OuterClass").resolve("test"));
	}

	@Test
	void failsWhenAttemptingToPublishRegularFilesAsDirectories(@TempDir Path tempDir) {
		var extensionContext = createExtensionContextForFilePublishing(tempDir);

		assertPreconditionViolationFor(() -> extensionContext.publishDirectory("test", dir -> {
			Files.delete(dir);
			Files.createFile(dir);
		})).withMessage("Published path must be a directory: " + tempDir.resolve("OuterClass").resolve("test"));
	}

	@Test
	void allowsPublishingToTheSameDirectoryTwice(@TempDir Path tempDir) {
		var extensionContext = createExtensionContextForFilePublishing(tempDir);

		extensionContext.publishDirectory("test",
			dir -> Files.writeString(dir.resolve("nested1.txt"), "Nested content 1"));
		extensionContext.publishDirectory("test",
			dir -> Files.writeString(dir.resolve("nested2.txt"), "Nested content 2"));

		assertThat(tempDir.resolve("OuterClass/test/nested1.txt")).hasContent("Nested content 1");
		assertThat(tempDir.resolve("OuterClass/test/nested2.txt")).hasContent("Nested content 2");
	}

	private ExtensionContext createExtensionContextForFilePublishing(Path tempDir) {
		return createExtensionContextForFilePublishing(tempDir, mock(EngineExecutionListener.class),
			outerClassDescriptor(null));
	}

	private ExtensionContext createExtensionContextForFilePublishing(Path tempDir,
			EngineExecutionListener engineExecutionListener, ClassTestDescriptor classTestDescriptor) {
		when(configuration.getOutputDirectoryCreator()) //
				.thenReturn(hierarchicalOutputDirectoryCreator(tempDir));
		return new ClassExtensionContext(mock(AbstractExtensionContext.class), engineExecutionListener,
			classTestDescriptor, PER_METHOD, configuration, extensionRegistry, launcherStoreFacade, mock());
	}

	@Test
	@SuppressWarnings({ "resource", "deprecation" })
	void usingStore() {
		var methodTestDescriptor = methodDescriptor();
		var classTestDescriptor = outerClassDescriptor(methodTestDescriptor);
		ExtensionContext parentContext = new ClassExtensionContext(mock(AbstractExtensionContext.class), mock(),
			classTestDescriptor, PER_METHOD, configuration, extensionRegistry, launcherStoreFacade, mock());
		var childContext = new MethodExtensionContext(parentContext, mock(), methodTestDescriptor, configuration,
			extensionRegistry, launcherStoreFacade, new OpenTest4JAwareThrowableCollector());
		childContext.setTestInstances(DefaultTestInstances.of(new OuterClassTestCase()));

		var childStore = childContext.getStore(Namespace.GLOBAL);
		var parentStore = parentContext.getStore(Namespace.GLOBAL);

		final Object key1 = "key 1";
		final var value1 = "a value";
		childStore.put(key1, value1);
		assertEquals(value1, childStore.get(key1));
		assertEquals(value1, childStore.remove(key1));
		assertNull(childStore.get(key1));

		childStore.put(key1, value1);
		assertEquals(value1, childStore.get(key1));
		assertEquals(value1, childStore.remove(key1, String.class));
		assertNull(childStore.get(key1));

		final Object key2 = "key 2";
		final var value2 = "other value";
		assertEquals(value2, childStore.computeIfAbsent(key2, key -> value2));
		assertEquals(value2, childStore.computeIfAbsent(key2, key -> "a different value", String.class));
		assertEquals(value2, childStore.getOrComputeIfAbsent(key2, key -> "a different value"));
		assertEquals(value2, childStore.getOrComputeIfAbsent(key2, key -> "a different value", String.class));
		assertEquals(value2, childStore.get(key2));
		assertEquals(value2, childStore.get(key2, String.class));

		final Object parentKey = "parent key";
		final var parentValue = "parent value";
		parentStore.put(parentKey, parentValue);
		assertEquals(parentValue, childStore.computeIfAbsent(parentKey, k -> "a different value"));
		assertEquals(parentValue, childStore.getOrComputeIfAbsent(parentKey, k -> "a different value"));
		assertEquals(parentValue, childStore.get(parentKey));
	}

	@ParameterizedTest
	@MethodSource("extensionContextFactories")
	void configurationParameter(Function<JupiterConfiguration, ? extends ExtensionContext> extensionContextFactory) {

		var key = "123";
		var expected = Optional.of(key);

		ConfigurationParameters configurationParameters = mock();
		when(configurationParameters.get("123")).thenReturn(expected);
		JupiterConfiguration echo = new DefaultJupiterConfiguration(configurationParameters,
			dummyOutputDirectoryCreator(), mock());

		var context = extensionContextFactory.apply(echo);

		assertEquals(expected, context.getConfigurationParameter(key));
	}

	static List<Named<Function<JupiterConfiguration, ? extends ExtensionContext>>> extensionContextFactories() {
		ExtensionRegistry extensionRegistry = mock();
		LauncherStoreFacade launcherStoreFacade = mock();
		var testClass = ExtensionContextTests.class;
		return List.of( //
			named("engine", (JupiterConfiguration configuration) -> {
				var engineUniqueId = UniqueId.parse("[engine:junit-jupiter]");
				var engineDescriptor = new JupiterEngineDescriptor(engineUniqueId, configuration);
				return new JupiterEngineExtensionContext(mock(), engineDescriptor, configuration, extensionRegistry,
					launcherStoreFacade);
			}), //
			named("class", (JupiterConfiguration configuration) -> {
				var classUniqueId = UniqueId.parse("[engine:junit-jupiter]/[class:MyClass]");
				var classTestDescriptor = new ClassTestDescriptor(classUniqueId, testClass, configuration);
				return new ClassExtensionContext(mock(AbstractExtensionContext.class), mock(), classTestDescriptor,
					PER_METHOD, configuration, extensionRegistry, launcherStoreFacade, mock());
			}), //
			named("method", (JupiterConfiguration configuration) -> {
				var method = ReflectionSupport.findMethod(testClass, "extensionContextFactories").orElseThrow();
				var methodUniqueId = UniqueId.parse("[engine:junit-jupiter]/[class:MyClass]/[method:myMethod]");
				var methodTestDescriptor = new TestMethodTestDescriptor(methodUniqueId, testClass, method, List::of,
					configuration);
				return new MethodExtensionContext(mock(AbstractExtensionContext.class), mock(), methodTestDescriptor,
					configuration, extensionRegistry, launcherStoreFacade, mock());
			}) //
		);
	}

	private NestedClassTestDescriptor nestedClassDescriptor() {
		return new NestedClassTestDescriptor(UniqueId.root("nested-class", "NestedClass"),
			OuterClassTestCase.NestedClass.class, List::of, configuration);
	}

	private NestedClassTestDescriptor doublyNestedClassDescriptor() {
		return new NestedClassTestDescriptor(UniqueId.root("nested-class", "DoublyNestedClass"),
			OuterClassTestCase.NestedClass.DoublyNestedClass.class, List::of, configuration);
	}

	private ClassTestDescriptor outerClassDescriptor(@Nullable TestDescriptor child) {
		var classTestDescriptor = new ClassTestDescriptor(UniqueId.root("class", "OuterClass"),
			OuterClassTestCase.class, configuration);
		if (child != null) {
			classTestDescriptor.addChild(child);
		}
		return classTestDescriptor;
	}

	private TestMethodTestDescriptor methodDescriptor() {
		try {
			return new TestMethodTestDescriptor(UniqueId.root("method", "aMethod"), OuterClassTestCase.class,
				OuterClassTestCase.class.getDeclaredMethod("aMethod"), List::of, configuration);
		}
		catch (NoSuchMethodException e) {
			throw new RuntimeException(e);
		}
	}

	private TestMethodTestDescriptor nestedMethodDescriptor() {
		try {
			return new TestMethodTestDescriptor(UniqueId.root("method", "nestedMethod"),
				OuterClassTestCase.NestedClass.class, BaseNestedTestCase.class.getDeclaredMethod("nestedMethod"),
				List::of, configuration);
		}
		catch (NoSuchMethodException e) {
			throw new RuntimeException(e);
		}
	}

	static abstract class BaseNestedTestCase {
		@Test
		void nestedMethod() {
		}

		@Nested
		class DoublyNestedClass {
		}
	}

	@Tag("outer-tag")
	static class OuterClassTestCase {

		@Tag("nested-tag")
		@Nested
		class NestedClass extends BaseNestedTestCase {
		}

		@Tag("method-tag")
		void aMethod() {
		}
	}

}
