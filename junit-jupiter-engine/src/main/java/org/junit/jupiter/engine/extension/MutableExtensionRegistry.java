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

import static java.util.Collections.emptyList;
import static java.util.Collections.emptySet;
import static org.apiguardian.api.API.Status.INTERNAL;

import java.lang.reflect.Field;
import java.lang.reflect.Member;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.ServiceLoader;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Stream;

import org.apiguardian.api.API;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.extension.Extension;
import org.junit.jupiter.engine.config.JupiterConfiguration;
import org.junit.platform.commons.logging.Logger;
import org.junit.platform.commons.logging.LoggerFactory;
import org.junit.platform.commons.support.ReflectionSupport;
import org.junit.platform.commons.util.ClassLoaderUtils;
import org.junit.platform.commons.util.Preconditions;
import org.junit.platform.commons.util.ServiceLoaderUtils;

/**
 * Default, mutable implementation of {@link ExtensionRegistry}.
 *
 * @since 5.5
 */
@API(status = INTERNAL, since = "5.5")
public class MutableExtensionRegistry implements ExtensionRegistry, ExtensionRegistrar {

	private static final Logger logger = LoggerFactory.getLogger(MutableExtensionRegistry.class);

	private static final List<Extension> DEFAULT_STATELESS_EXTENSIONS = List.of( //
		new DisabledCondition(), //
		new AutoCloseExtension(), //
		new TimeoutExtension(), //
		new RepeatedTestExtension(), //
		new TestInfoParameterResolver(), //
		new TestReporterParameterResolver() //
	);

	/**
	 * Factory for creating and populating a new root registry with the default
	 * extensions.
	 *
	 * <p>If the {@link org.junit.jupiter.engine.Constants#EXTENSIONS_AUTODETECTION_ENABLED_PROPERTY_NAME}
	 * configuration parameter has been set to {@code true}, extensions will be
	 * auto-detected using Java's {@link ServiceLoader} mechanism and automatically
	 * registered after the default extensions.
	 *
	 * <p>If the
	 * {@value org.junit.jupiter.engine.Constants#EXTENSIONS_TIMEOUT_THREAD_DUMP_ENABLED_PROPERTY_NAME}
	 * configuration parameter has been set to {@code true}, the
	 * {@link PreInterruptThreadDumpPrinter} will be registered.
	 *
	 * @param configuration configuration parameters used to retrieve the extension
	 * auto-detection flag; never {@code null}
	 * @return a new {@code ExtensionRegistry}; never {@code null}
	 */
	public static MutableExtensionRegistry createRegistryWithDefaultExtensions(JupiterConfiguration configuration) {
		MutableExtensionRegistry extensionRegistry = new MutableExtensionRegistry();

		DEFAULT_STATELESS_EXTENSIONS.forEach(extensionRegistry::registerDefaultExtension);

		extensionRegistry.registerDefaultExtension(new TempDirectory(configuration));

		if (configuration.isExtensionAutoDetectionEnabled()) {
			registerAutoDetectedExtensions(extensionRegistry, configuration);
		}

		if (configuration.isThreadDumpOnTimeoutEnabled()) {
			extensionRegistry.registerDefaultExtension(new PreInterruptThreadDumpPrinter());
		}

		return extensionRegistry;
	}

	private static void registerAutoDetectedExtensions(MutableExtensionRegistry extensionRegistry,
			JupiterConfiguration configuration) {

		Predicate<Class<? extends Extension>> filter = configuration.getFilterForAutoDetectedExtensions();
		List<Class<? extends Extension>> excludedExtensions = new ArrayList<>();

		ServiceLoader<Extension> serviceLoader = ServiceLoader.load(Extension.class,
			ClassLoaderUtils.getDefaultClassLoader());
		ServiceLoaderUtils.filter(serviceLoader, clazz -> {
			boolean included = filter.test(clazz);
			if (!included) {
				excludedExtensions.add(clazz);
			}
			return included;
		}) //
				.forEach(extensionRegistry::registerAutoDetectedExtension);

		logExcludedExtensions(excludedExtensions);
	}

	private static void logExcludedExtensions(List<Class<? extends Extension>> excludedExtensions) {
		if (!excludedExtensions.isEmpty()) {
			// @formatter:off
			List<String> excludeExtensionNames = excludedExtensions
					.stream()
					.map(Class::getName)
					.toList();
			// @formatter:on
			logger.config(() -> "Excluded auto-detected extensions due to configured includes/excludes: %s".formatted(
				excludeExtensionNames));
		}
	}

	/**
	 * Factory for creating and populating a new registry from a list of
	 * extension types and a parent registry.
	 *
	 * @param parentRegistry the parent registry
	 * @param extensionTypes the types of extensions to be registered in
	 * the new registry
	 * @return a new {@code ExtensionRegistry}; never {@code null}
	 */
	public static MutableExtensionRegistry createRegistryFrom(MutableExtensionRegistry parentRegistry,
			Stream<Class<? extends Extension>> extensionTypes) {

		Preconditions.notNull(parentRegistry, "parentRegistry must not be null");

		MutableExtensionRegistry registry = new MutableExtensionRegistry(parentRegistry);
		extensionTypes.forEach(registry::registerExtension);
		return registry;
	}

	private final Set<Class<? extends Extension>> registeredExtensionTypes;
	private final List<Entry> registeredExtensions;
	private final Map<Class<?>, LateInitExtensions> lateInitExtensions;

	private MutableExtensionRegistry() {
		this(emptySet(), emptyList());
	}

	private MutableExtensionRegistry(MutableExtensionRegistry parent) {
		this(parent.registeredExtensionTypes, parent.registeredExtensions);
	}

	private MutableExtensionRegistry(Set<Class<? extends Extension>> registeredExtensionTypes,
			List<Entry> registeredExtensions) {
		this.registeredExtensionTypes = new LinkedHashSet<>(registeredExtensionTypes);
		this.registeredExtensions = new ArrayList<>(registeredExtensions.size());
		this.lateInitExtensions = new LinkedHashMap<>();
		registeredExtensions.forEach(entry -> {
			Entry newEntry = entry;
			if (entry instanceof LateInitEntry lateInitEntry) {
				newEntry = lateInitEntry.getExtension() //
						.map(Entry::of) //
						.orElseGet(() -> getLateInitExtensions(lateInitEntry.getTestClass()).add(lateInitEntry.copy()));
			}
			this.registeredExtensions.add(newEntry);
		});
	}

	@Override
	public <E extends Extension> Stream<E> stream(Class<E> extensionType) {
		return this.registeredExtensions.stream() //
				.map(p -> p.getExtension().orElse(null)) //
				.filter(extensionType::isInstance) //
				.map(extensionType::cast);
	}

	@Override
	public void registerExtension(Class<? extends Extension> extensionType) {
		if (!isAlreadyRegistered(extensionType)) {
			registerLocalExtension(ReflectionSupport.newInstance(extensionType));
		}
	}

	/**
	 * Determine if the supplied type is already registered in this registry or in a
	 * parent registry.
	 */
	private boolean isAlreadyRegistered(Class<? extends Extension> extensionType) {
		return this.registeredExtensionTypes.contains(extensionType);
	}

	@Override
	public void registerExtension(Extension extension, Object source) {
		Preconditions.notNull(source, "source must not be null");
		registerExtension("local", extension, source);
	}

	@Override
	public void registerSyntheticExtension(Extension extension, Object source) {
		registerExtension("synthetic", extension, source);
	}

	@Override
	public void registerUninitializedExtension(Class<?> testClass, Field source,
			Function<Object, ? extends Extension> initializer) {
		Preconditions.notNull(testClass, "testClass must not be null");
		Preconditions.notNull(source, "source must not be null");
		Preconditions.notNull(initializer, "initializer must not be null");

		logger.trace(() -> "Registering local extension (late-init) for [%s]%s".formatted(source.getType().getName(),
			buildSourceInfo(source)));

		LateInitEntry entry = getLateInitExtensions(testClass) //
				.add(new LateInitEntry(testClass, initializer));
		this.registeredExtensions.add(entry);
	}

	@Override
	public void initializeExtensions(Class<?> testClass, Object testInstance) {
		Preconditions.notNull(testClass, "testClass must not be null");
		Preconditions.notNull(testInstance, "testInstance must not be null");

		LateInitExtensions extensions = lateInitExtensions.remove(testClass);
		if (extensions != null) {
			extensions.initialize(testInstance);
		}
	}

	private LateInitExtensions getLateInitExtensions(Class<?> testClass) {
		return this.lateInitExtensions.computeIfAbsent(testClass, __ -> new LateInitExtensions());
	}

	private void registerDefaultExtension(Extension extension) {
		registerExtension("default", extension);
	}

	private void registerAutoDetectedExtension(Extension extension) {
		registerExtension("auto-detected", extension);
	}

	private void registerLocalExtension(Extension extension) {
		registerExtension("local", extension);
	}

	private void registerExtension(String category, Extension extension) {
		registerExtension(category, extension, null);
	}

	private void registerExtension(String category, Extension extension, @Nullable Object source) {
		Preconditions.notBlank(category, "category must not be null or blank");
		Preconditions.notNull(extension, "extension must not be null");

		logger.trace(() -> "Registering %s extension [%s]%s".formatted(category, extension, buildSourceInfo(source)));

		this.registeredExtensions.add(Entry.of(extension));
		this.registeredExtensionTypes.add(extension.getClass());
	}

	private String buildSourceInfo(@Nullable Object source) {
		if (source == null) {
			return "";
		}
		if (source instanceof Member member) {
			Object type = (member instanceof Method ? "method" : "field");
			source = "%s %s.%s".formatted(type, member.getDeclaringClass().getName(), member.getName());
		}
		return " from source [" + source + "]";
	}

	private interface Entry {

		static Entry of(Extension extension) {
			Optional<Extension> value = Optional.of(extension);
			return () -> value;
		}

		Optional<Extension> getExtension();
	}

	private static class LateInitEntry implements Entry {

		private final Class<?> testClass;
		private final Function<Object, ? extends Extension> initializer;

		@SuppressWarnings("OptionalUsedAsFieldOrParameterType")
		private Optional<Extension> extension = Optional.empty();

		LateInitEntry(Class<?> testClass, Function<Object, ? extends Extension> initializer) {
			this.testClass = testClass;
			this.initializer = initializer;
		}

		@Override
		public Optional<Extension> getExtension() {
			return extension;
		}

		private Class<?> getTestClass() {
			return testClass;
		}

		void initialize(Object testInstance) {
			Preconditions.condition(extension.isEmpty(), "Extension already initialized");
			extension = Optional.of(initializer.apply(testInstance));
		}

		LateInitEntry copy() {
			Preconditions.condition(extension.isEmpty(), "Extension already initialized");
			return new LateInitEntry(testClass, initializer);
		}
	}

	private static class LateInitExtensions {

		private final List<LateInitEntry> entries = new ArrayList<>();

		LateInitEntry add(LateInitEntry entry) {
			entries.add(entry);
			return entry;
		}

		void initialize(Object testInstance) {
			entries.forEach(entry -> entry.initialize(testInstance));
		}

	}

}
