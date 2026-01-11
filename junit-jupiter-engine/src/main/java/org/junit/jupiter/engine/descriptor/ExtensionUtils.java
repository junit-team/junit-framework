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

import static java.util.stream.Collectors.toList;
import static org.junit.platform.commons.support.AnnotationSupport.findAnnotation;
import static org.junit.platform.commons.support.AnnotationSupport.isAnnotated;
import static org.junit.platform.commons.util.AnnotationUtils.findRepeatableAnnotations;
import static org.junit.platform.commons.util.ReflectionUtils.HierarchyTraversalMode.TOP_DOWN;
import static org.junit.platform.commons.util.ReflectionUtils.getDeclaredConstructor;
import static org.junit.platform.commons.util.ReflectionUtils.streamFields;
import static org.junit.platform.commons.util.ReflectionUtils.tryToReadFieldValue;

import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Executable;
import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Predicate;
import java.util.stream.Stream;

import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.extension.Extension;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.junit.jupiter.engine.extension.ExtensionRegistrar;
import org.junit.jupiter.engine.extension.MutableExtensionRegistry;
import org.junit.platform.commons.PreconditionViolationException;
import org.junit.platform.commons.support.ModifierSupport;
import org.junit.platform.commons.util.Preconditions;
import org.junit.platform.commons.util.ReflectionUtils;

/**
 * Collection of utilities for working with extensions and the extension registry.
 *
 * @since 5.1
 * @see ExtensionRegistrar
 * @see MutableExtensionRegistry
 * @see ExtendWith
 * @see RegisterExtension
 */
final class ExtensionUtils {

	private ExtensionUtils() {
		/* no-op */
	}

	/**
	 * Populate a new {@link MutableExtensionRegistry} from extension types declared via
	 * {@link ExtendWith @ExtendWith} on the supplied {@link AnnotatedElement}.
	 *
	 * @param parentRegistry the parent extension registry to set in the newly
	 * created registry; never {@code null}
	 * @param annotatedElement the annotated element on which to search for
	 * declarations of {@code @ExtendWith}; never {@code null}
	 *
	 * @return the new extension registry; never {@code null}
	 * @since 5.0
	 */
	static MutableExtensionRegistry populateNewExtensionRegistryFromExtendWithAnnotation(
			MutableExtensionRegistry parentRegistry, AnnotatedElement annotatedElement) {

		Preconditions.notNull(parentRegistry, "Parent ExtensionRegistry must not be null");
		Preconditions.notNull(annotatedElement, "AnnotatedElement must not be null");

		return MutableExtensionRegistry.createRegistryFrom(parentRegistry,
			streamDeclarativeExtensionTypes(annotatedElement));
	}

	/**
	 * Register extensions using the supplied registrar from static fields in
	 * the supplied class that are annotated with {@link ExtendWith @ExtendWith}
	 * or {@link RegisterExtension @RegisterExtension}.
	 *
	 * <p>The extensions will be sorted according to {@link Order @Order} semantics
	 * prior to registration.
	 *
	 * @param registrar the registrar with which to register the extensions; never {@code null}
	 * @param clazz the class or interface in which to find the fields; never {@code null}
	 * @since 5.11
	 */
	static void registerExtensionsFromStaticFields(ExtensionRegistrar registrar, Class<?> clazz) {
		streamExtensionRegisteringFields(clazz, ModifierSupport::isStatic) //
				.forEach(field -> {
					List<Class<? extends Extension>> extensionTypes = streamDeclarativeExtensionTypes(field).collect(
						toList());
					boolean isExtendWithPresent = !extensionTypes.isEmpty();

					if (isExtendWithPresent) {
						extensionTypes.forEach(registrar::registerExtension);
					}
					if (isAnnotated(field, RegisterExtension.class)) {
						Extension extension = readAndValidateExtensionFromField(field, null, extensionTypes);
						registrar.registerExtension(extension, field);
					}
				});
	}

	/**
	 * Register extensions using the supplied registrar from instance fields in
	 * the supplied class that are annotated with {@link ExtendWith @ExtendWith}
	 * or {@link RegisterExtension @RegisterExtension}.
	 *
	 * <p>The extensions will be sorted according to {@link Order @Order} semantics
	 * prior to registration.
	 *
	 * @param registrar the registrar with which to register the extensions; never {@code null}
	 * @param clazz the class or interface in which to find the fields; never {@code null}
	 * @since 5.11
	 */
	static void registerExtensionsFromInstanceFields(ExtensionRegistrar registrar, Class<?> clazz) {
		streamExtensionRegisteringFields(clazz, ReflectionUtils::isNotStatic) //
				.forEach(field -> {
					List<Class<? extends Extension>> extensionTypes = streamDeclarativeExtensionTypes(field).collect(
						toList());
					boolean isExtendWithPresent = !extensionTypes.isEmpty();

					if (isExtendWithPresent) {
						extensionTypes.forEach(registrar::registerExtension);
					}
					if (isAnnotated(field, RegisterExtension.class)) {
						registrar.registerUninitializedExtension(clazz, field,
							instance -> readAndValidateExtensionFromField(field, instance, extensionTypes));
					}
				});
	}

	/**
	 * @since 5.11
	 */
	private static Extension readAndValidateExtensionFromField(Field field, @Nullable Object instance,
			List<Class<? extends Extension>> declarativeExtensionTypes) {
		Object value = tryToReadFieldValue(field, instance) //
				.getOrThrow(e -> new PreconditionViolationException(
					"Failed to read @RegisterExtension field [%s]".formatted(field), e));

		Preconditions.condition(value instanceof Extension,
			() -> "Failed to register extension via @RegisterExtension field [%s]: field value's type [%s] must implement an [%s] API.".formatted(
				field, (value != null ? value.getClass().getName() : null), Extension.class.getName()));

		declarativeExtensionTypes.forEach(extensionType -> {
			Class<?> valueType = value.getClass();
			Preconditions.condition(!extensionType.equals(valueType), () -> """
					Failed to register extension via field [%s]. \
					The field registers an extension of type [%s] via @RegisterExtension and @ExtendWith, \
					but only one registration of a given extension type is permitted.""".formatted(field,
				valueType.getName()));
		});

		return (Extension) value;
	}

	/**
	 * Register extensions using the supplied registrar from parameters in the
	 * declared constructor of the supplied class that are annotated with
	 * {@link ExtendWith @ExtendWith}.
	 *
	 * @param registrar the registrar with which to register the extensions; never {@code null}
	 * @param clazz the class in which to find the declared constructor; never {@code null}
	 * @since 5.8
	 */
	static void registerExtensionsFromConstructorParameters(ExtensionRegistrar registrar, Class<?> clazz) {
		registerExtensionsFromExecutableParameters(registrar, getDeclaredConstructor(clazz));
	}

	/**
	 * Register extensions using the supplied registrar from parameters in the
	 * supplied {@link Executable} (i.e., a {@link java.lang.reflect.Constructor}
	 * or {@link java.lang.reflect.Method}) that are annotated with
	 * {@link ExtendWith @ExtendWith}.
	 *
	 * @param registrar the registrar with which to register the extensions; never {@code null}
	 * @param executable the constructor or method whose parameters should be searched; never {@code null}
	 * @since 5.8
	 */
	static void registerExtensionsFromExecutableParameters(ExtensionRegistrar registrar, Executable executable) {
		Preconditions.notNull(registrar, "ExtensionRegistrar must not be null");
		Preconditions.notNull(executable, "Executable must not be null");

		AtomicInteger index = new AtomicInteger();

		// @formatter:off
		Arrays.stream(executable.getParameters())
				.map(parameter -> findRepeatableAnnotations(parameter, index.getAndIncrement(), ExtendWith.class))
				.flatMap(ExtensionUtils::streamDeclarativeExtensionTypes)
				.forEach(registrar::registerExtension);
		// @formatter:on
	}

	/**
	 * @since 5.11
	 */
	private static Stream<Field> streamExtensionRegisteringFields(Class<?> clazz, Predicate<Field> predicate) {
		return streamFields(clazz, predicate.and(registersExtension), TOP_DOWN)//
				.sorted(orderComparator);
	}

	/**
	 * @since 5.11
	 */
	private static Stream<Class<? extends Extension>> streamDeclarativeExtensionTypes(
			AnnotatedElement annotatedElement) {
		return streamDeclarativeExtensionTypes(findRepeatableAnnotations(annotatedElement, ExtendWith.class));
	}

	/**
	 * @since 5.11
	 */
	private static Stream<Class<? extends Extension>> streamDeclarativeExtensionTypes(
			List<ExtendWith> extendWithAnnotations) {
		return extendWithAnnotations.stream().map(ExtendWith::value).flatMap(Arrays::stream);
	}

	/**
	 * @since 5.4
	 */
	private static final Comparator<Field> orderComparator = //
		Comparator.comparingInt(ExtensionUtils::getOrder);

	/**
	 * @since 5.4
	 */
	private static int getOrder(Field field) {
		return findAnnotation(field, Order.class).map(Order::value).orElse(Order.DEFAULT);
	}

	/**
	 * {@link Predicate} which determines if a {@link Field} registers an extension via
	 * {@link RegisterExtension @RegisterExtension} or {@link ExtendWith @ExtendWith}.
	 *
	 * @since 5.11.3
	 */
	private static final Predicate<Field> registersExtension = //
		field -> isAnnotated(field, RegisterExtension.class)
				|| !findRepeatableAnnotations(field, ExtendWith.class).isEmpty();

}
