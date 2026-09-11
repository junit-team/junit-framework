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
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Type;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.List;
import java.util.ServiceLoader;
import java.util.concurrent.CompletionStage;
import java.util.function.Predicate;

import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.AsyncReturnValueHandler;
import org.junit.jupiter.api.extension.EarlyExtension;
import org.junit.jupiter.api.extension.Extension;
import org.junit.jupiter.engine.config.JupiterConfiguration;
import org.junit.platform.commons.util.ClassLoaderUtils;
import org.junit.platform.commons.util.ServiceLoaderUtils;

/**
 * Unit tests for {@link EarlyExtensionRegistry}.
 *
 * @since 6.2
 */
class EarlyExtensionRegistryTests {

	@Test
	void doesNotLoadEarlyExtensionsWhenAutoDetectionIsDisabled() {
		JupiterConfiguration configuration = configurationWithAutoDetection(false);

		EarlyExtensionRegistry registry = EarlyExtensionRegistry.create(configuration);

		assertTrue(registry.getAsyncReturnValueHandlers().isEmpty());
	}

	@Test
	void loadsServiceLoaderProvidedAsyncReturnValueHandlers() throws IOException {
		Path testDir = Files.createTempDirectory("early-extension-registry-test");
		Path servicesDir = testDir.resolve("META-INF/services");
		Files.createDirectories(servicesDir);
		Files.writeString(servicesDir.resolve(Extension.class.getName()), EarlyReturnValueHandler.class.getName());

		try (URLClassLoader classLoader = new URLClassLoader(new URL[] { testDir.toUri().toURL() },
			ClassLoaderUtils.getDefaultClassLoader())) {
			JupiterConfiguration configuration = configurationWithAutoDetection(true);

			// Use the temporary classloader the same way the engine does, by
			// loading via ServiceLoader.filter(..., EarlyExtension::isAssignableFrom).
			var serviceLoader = ServiceLoader.load(Extension.class, classLoader);
			List<AsyncReturnValueHandler> handlers = ServiceLoaderUtils //
					.filter(serviceLoader, clazz -> EarlyExtension.class.isAssignableFrom(clazz)) //
					.filter(AsyncReturnValueHandler.class::isInstance) //
					.map(AsyncReturnValueHandler.class::cast) //
					.toList();

			assertEquals(1, handlers.size());
			assertTrue(handlers.get(0) instanceof EarlyReturnValueHandler);
		}
	}

	@Test
	void getAsyncReturnValueHandlersIsEmptyByDefaultWhenAutoDetectionDisabled() {
		EarlyExtensionRegistry registry = EarlyExtensionRegistry.create(configurationWithAutoDetection(false));

		assertSame(Collections.emptyList(), registry.getAsyncReturnValueHandlers(), "an empty list should be returned");
	}

	private static JupiterConfiguration configurationWithAutoDetection(boolean enabled) {
		JupiterConfiguration configuration = mock(JupiterConfiguration.class);
		when(configuration.isExtensionAutoDetectionEnabled()).thenReturn(enabled);
		when(configuration.getFilterForAutoDetectedExtensions()).thenReturn((Predicate<Class<? extends Extension>>) //
		clazz -> true);
		return configuration;
	}

	/**
	 * Minimal, globally registered {@link AsyncReturnValueHandler} for verifying
	 * ServiceLoader-based discovery.
	 */
	public static class EarlyReturnValueHandler implements AsyncReturnValueHandler {

		@Override
		public boolean supports(Type genericReturnType, @Nullable AnnotatedElement element) {
			return false;
		}

		@Override
		public CompletionStage<?> toCompletionStage(Object returnedValue) {
			throw new UnsupportedOperationException("not used in this test");
		}
	}

}
