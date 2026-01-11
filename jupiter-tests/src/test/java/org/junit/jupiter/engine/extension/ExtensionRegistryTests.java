/*
 * Copyright 2015-2026 the original author or authors.
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v2.0 which
 * accompanies this distribution and is available at
 *
 * https://www.eclipse.org/legal/epl-v20.html
 */

package org.junit.jupiter.engine.extension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.engine.extension.MutableExtensionRegistry.createRegistryFrom;
import static org.junit.jupiter.engine.extension.MutableExtensionRegistry.createRegistryWithDefaultExtensions;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Predicate;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.BeforeEachCallback;
import org.junit.jupiter.api.extension.ExecutionCondition;
import org.junit.jupiter.api.extension.Extension;
import org.junit.jupiter.api.extension.InvocationInterceptor;
import org.junit.jupiter.api.extension.ParameterResolver;
import org.junit.jupiter.api.extension.TestTemplateInvocationContextProvider;
import org.junit.jupiter.engine.config.JupiterConfiguration;
import org.junit.platform.commons.util.ClassNamePatternFilterUtils;

/**
 * Tests for the {@link MutableExtensionRegistry}.
 *
 * @since 5.0
 */
class ExtensionRegistryTests {

	private static final int NUM_CORE_EXTENSIONS = 7;
	private static final int NUM_DEMO_EXTENSIONS = 2;

	private final JupiterConfiguration configuration = mock();

	private MutableExtensionRegistry registry = createRegistryWithDefaultExtensions(configuration);

	@Test
	void newRegistryWithoutParentHasDefaultExtensions() {
		List<Extension> extensions = registry.getExtensions(Extension.class);

		assertEquals(NUM_CORE_EXTENSIONS, extensions.size());
		assertDefaultGlobalExtensionsAreRegistered();
	}

	@Test
	void newRegistryWithoutParentHasDefaultExtensionsPlusAutodetectedExtensionsLoadedViaServiceLoader() {

		when(configuration.isExtensionAutoDetectionEnabled()).thenReturn(true);
		when(configuration.getFilterForAutoDetectedExtensions()).thenReturn(__ -> true);
		registry = createRegistryWithDefaultExtensions(configuration);

		List<Extension> extensions = registry.getExtensions(Extension.class);

		assertEquals(NUM_CORE_EXTENSIONS + NUM_DEMO_EXTENSIONS, extensions.size());
		assertDefaultGlobalExtensionsAreRegistered(4);

		assertExtensionRegistered(registry, ServiceLoaderExtension.class);
		assertEquals(4, countExtensions(registry, BeforeAllCallback.class));
	}

	@Test
	void registryIncludesAndExcludesSpecificAutoDetectedExtensions() {
		when(configuration.isExtensionAutoDetectionEnabled()).thenReturn(true);
		when(configuration.getFilterForAutoDetectedExtensions()).thenReturn(
			extensionFilter(ServiceLoaderExtension.class.getName(), ConfigLoaderExtension.class.getName()));
		registry = createRegistryWithDefaultExtensions(configuration);

		List<Extension> extensions = registry.getExtensions(Extension.class);

		assertEquals(NUM_CORE_EXTENSIONS + NUM_DEMO_EXTENSIONS - 1 /* exlcuded ConfigLoaderExtension */,
			extensions.size());
		assertDefaultGlobalExtensionsAreRegistered(3);

		assertExtensionRegistered(registry, ServiceLoaderExtension.class);
		assertEquals(3, countExtensions(registry, BeforeAllCallback.class));
	}

	@Test
	void registryIncludesAllAutoDetectedExtensionsAndExcludesNone() {
		when(configuration.isExtensionAutoDetectionEnabled()).thenReturn(true);
		when(configuration.getFilterForAutoDetectedExtensions()).thenReturn(extensionFilter("*", ""));
		registry = createRegistryWithDefaultExtensions(configuration);

		List<Extension> extensions = registry.getExtensions(Extension.class);

		assertEquals(NUM_CORE_EXTENSIONS + NUM_DEMO_EXTENSIONS, extensions.size());
		assertDefaultGlobalExtensionsAreRegistered(4);

		assertExtensionRegistered(registry, ServiceLoaderExtension.class);
		assertExtensionRegistered(registry, ConfigLoaderExtension.class);
		assertEquals(4, countExtensions(registry, BeforeAllCallback.class));
	}

	@Test
	void registryIncludesSpecificAutoDetectedExtensionsAndExcludesAll() {
		when(configuration.isExtensionAutoDetectionEnabled()).thenReturn(true);
		when(configuration.getFilterForAutoDetectedExtensions()).thenReturn(
			extensionFilter(ServiceLoaderExtension.class.getName(), "*"));
		registry = createRegistryWithDefaultExtensions(configuration);

		List<Extension> extensions = registry.getExtensions(Extension.class);

		assertEquals(NUM_CORE_EXTENSIONS, extensions.size());
		assertDefaultGlobalExtensionsAreRegistered(2);

		assertExtensionNotRegistered(registry, ServiceLoaderExtension.class);
		assertEquals(2, countExtensions(registry, BeforeAllCallback.class));
	}

	@Test
	void registryIncludesAndExcludesSameAutoDetectedExtension() {
		when(configuration.isExtensionAutoDetectionEnabled()).thenReturn(true);
		when(configuration.getFilterForAutoDetectedExtensions()).thenReturn(
			extensionFilter(ServiceLoaderExtension.class.getName(), ServiceLoaderExtension.class.getName()));
		registry = createRegistryWithDefaultExtensions(configuration);

		List<Extension> extensions = registry.getExtensions(Extension.class);

		assertEquals(NUM_CORE_EXTENSIONS, extensions.size());
		assertDefaultGlobalExtensionsAreRegistered(2);

		assertExtensionNotRegistered(registry, ServiceLoaderExtension.class);
		assertEquals(2, countExtensions(registry, BeforeAllCallback.class));
	}

	@Test
	void registerExtensionByImplementingClass() {
		registry.registerExtension(MyExtension.class);

		assertExtensionRegistered(registry, MyExtension.class);

		registry.registerExtension(MyExtension.class);
		registry.registerExtension(MyExtension.class);
		registry.registerExtension(MyExtension.class);

		assertEquals(1, registry.getExtensions(MyExtension.class).size());
		assertExtensionRegistered(registry, MyExtension.class);
		assertEquals(1, countExtensions(registry, MyExtensionApi.class));

		registry.registerExtension(YourExtension.class);
		assertExtensionRegistered(registry, YourExtension.class);
		assertEquals(2, countExtensions(registry, MyExtensionApi.class));
	}

	@Test
	void registerExtensionThatImplementsMultipleExtensionApis() {
		registry.registerExtension(MultipleExtension.class);

		assertExtensionRegistered(registry, MultipleExtension.class);

		assertEquals(1, countExtensions(registry, MyExtensionApi.class));
		assertEquals(1, countExtensions(registry, AnotherExtensionApi.class));
	}

	@Test
	void extensionsAreInheritedFromParent() {
		MutableExtensionRegistry parent = registry;
		parent.registerExtension(MyExtension.class);

		MutableExtensionRegistry child = createRegistryFrom(parent, Stream.of(YourExtension.class));
		assertExtensionRegistered(child, MyExtension.class);
		assertExtensionRegistered(child, YourExtension.class);
		assertEquals(2, countExtensions(child, MyExtensionApi.class));

		ExtensionRegistry grandChild = createRegistryFrom(child, Stream.empty());
		assertExtensionRegistered(grandChild, MyExtension.class);
		assertExtensionRegistered(grandChild, YourExtension.class);
		assertEquals(2, countExtensions(grandChild, MyExtensionApi.class));
	}

	@Test
	void registeringSameExtensionImplementationInParentAndChildDoesNotResultInDuplicate() {
		MutableExtensionRegistry parent = registry;
		parent.registerExtension(MyExtension.class);
		assertEquals(1, countExtensions(parent, MyExtensionApi.class));

		MutableExtensionRegistry child = createRegistryFrom(parent, Stream.of(MyExtension.class, YourExtension.class));
		assertExtensionRegistered(child, MyExtension.class);
		assertExtensionRegistered(child, YourExtension.class);
		assertEquals(2, countExtensions(child, MyExtensionApi.class));

		ExtensionRegistry grandChild = createRegistryFrom(child, Stream.of(MyExtension.class, YourExtension.class));
		assertExtensionRegistered(grandChild, MyExtension.class);
		assertExtensionRegistered(grandChild, YourExtension.class);
		assertEquals(2, countExtensions(grandChild, MyExtensionApi.class));
	}

	@Test
	void canStreamOverRegisteredExtension() {
		registry.registerExtension(MyExtension.class);

		AtomicBoolean hasRun = new AtomicBoolean(false);

		registry.getExtensions(MyExtensionApi.class).forEach(extension -> {
			assertEquals(MyExtension.class.getName(), extension.getClass().getName());
			hasRun.set(true);
		});

		assertTrue(hasRun.get());
	}

	private long countExtensions(ExtensionRegistry registry, Class<? extends Extension> extensionType) {
		return registry.stream(extensionType).count();
	}

	private void assertExtensionRegistered(ExtensionRegistry registry, Class<? extends Extension> extensionType) {
		assertFalse(registry.getExtensions(extensionType).isEmpty(),
			() -> extensionType.getSimpleName() + " should be present");
	}

	private void assertExtensionNotRegistered(ExtensionRegistry registry, Class<? extends Extension> extensionType) {
		assertTrue(registry.getExtensions(extensionType).isEmpty(),
			() -> extensionType.getSimpleName() + " should not be present");
	}

	private void assertDefaultGlobalExtensionsAreRegistered() {
		assertDefaultGlobalExtensionsAreRegistered(2);
	}

	private void assertDefaultGlobalExtensionsAreRegistered(long bacCount) {
		assertExtensionRegistered(registry, DisabledCondition.class);
		assertExtensionRegistered(registry, TempDirectory.class);
		assertExtensionRegistered(registry, TimeoutExtension.class);
		assertExtensionRegistered(registry, RepeatedTestExtension.class);
		assertExtensionRegistered(registry, TestInfoParameterResolver.class);
		assertExtensionRegistered(registry, TestReporterParameterResolver.class);

		assertEquals(bacCount, countExtensions(registry, BeforeAllCallback.class));
		assertEquals(2, countExtensions(registry, BeforeEachCallback.class));
		assertEquals(3, countExtensions(registry, ParameterResolver.class));
		assertEquals(1, countExtensions(registry, ExecutionCondition.class));
		assertEquals(1, countExtensions(registry, TestTemplateInvocationContextProvider.class));
		assertEquals(1, countExtensions(registry, InvocationInterceptor.class));
	}

	private static Predicate<Class<? extends Extension>> extensionFilter(String includes, String excludes) {
		var nameFilter = ClassNamePatternFilterUtils.includeMatchingClassNames(includes) //
				.and(ClassNamePatternFilterUtils.excludeMatchingClassNames(excludes));
		return clazz -> nameFilter.test(clazz.getName());
	}

	// -------------------------------------------------------------------------

	interface MyExtensionApi extends Extension {

		void doNothing(String test);
	}

	interface AnotherExtensionApi extends Extension {

		void doMore();
	}

	static class MyExtension implements MyExtensionApi {

		@Override
		public void doNothing(String test) {
		}
	}

	static class YourExtension implements MyExtensionApi {

		@Override
		public void doNothing(String test) {
		}
	}

	static class MultipleExtension implements MyExtensionApi, AnotherExtensionApi {

		@Override
		public void doNothing(String test) {
		}

		@Override
		public void doMore() {
		}
	}

}
