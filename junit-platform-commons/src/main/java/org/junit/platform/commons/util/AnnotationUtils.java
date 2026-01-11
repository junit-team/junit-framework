/*
 * Copyright 2015-2026 the original author or authors.
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v2.0 which
 * accompanies this distribution and is available at
 *
 * https://www.eclipse.org/legal/epl-v20.html
 */

package org.junit.platform.commons.util;

import static java.util.Arrays.asList;
import static java.util.Objects.requireNonNull;
import static org.apiguardian.api.API.Status.INTERNAL;
import static org.junit.platform.commons.util.ReflectionUtils.isInnerClass;

import java.lang.annotation.Annotation;
import java.lang.annotation.Inherited;
import java.lang.annotation.Repeatable;
import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Constructor;
import java.lang.reflect.Executable;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Predicate;

import org.apiguardian.api.API;
import org.jspecify.annotations.Nullable;
import org.junit.platform.commons.JUnitException;
import org.junit.platform.commons.annotation.Contract;
import org.junit.platform.commons.util.ReflectionUtils.HierarchyTraversalMode;

/**
 * Collection of utilities for working with {@linkplain Annotation annotations}.
 *
 * <h2>DISCLAIMER</h2>
 *
 * <p>These utilities are intended solely for usage within the JUnit framework
 * itself. <strong>Any usage by external parties is not supported.</strong>
 * Use at your own risk!
 *
 * <p>Some utilities are published via the maintained {@code AnnotationSupport}
 * class.
 *
 * @since 1.0
 * @see Annotation
 * @see AnnotatedElement
 * @see org.junit.platform.commons.support.AnnotationSupport
 */
@API(status = INTERNAL, since = "1.0")
public final class AnnotationUtils {

	private AnnotationUtils() {
		/* no-op */
	}

	private static final ConcurrentHashMap<Class<? extends Annotation>, Boolean> repeatableAnnotationContainerCache = //
		new ConcurrentHashMap<>(16);

	/**
	 * Determine if an annotation of {@code annotationType} is either
	 * <em>present</em> or <em>meta-present</em> on the supplied optional
	 * {@code element}.
	 *
	 * @see #findAnnotation(Optional, Class)
	 * @see org.junit.platform.commons.support.AnnotationSupport#isAnnotated(Optional, Class)
	 */
	@SuppressWarnings("NullableOptional")
	@Contract("null, _ -> false")
	public static boolean isAnnotated(@Nullable Optional<? extends AnnotatedElement> element,
			Class<? extends Annotation> annotationType) {

		return findAnnotation(element, annotationType).isPresent();
	}

	/**
	 * @since 1.8
	 * @see #findAnnotation(Parameter, int, Class)
	 */
	public static boolean isAnnotated(Parameter parameter, int index, Class<? extends Annotation> annotationType) {
		return findAnnotation(parameter, index, annotationType).isPresent();
	}

	/**
	 * Determine if an annotation of {@code annotationType} is either
	 * <em>present</em> or <em>meta-present</em> on the supplied
	 * {@code element}.
	 *
	 * @param element the element on which to search for the annotation; may be
	 * {@code null}
	 * @param annotationType the annotation type to search for; never {@code null}
	 * @return {@code true} if the annotation is present or meta-present
	 * @see #findAnnotation(AnnotatedElement, Class)
	 * @see org.junit.platform.commons.support.AnnotationSupport#isAnnotated(AnnotatedElement, Class)
	 */
	@Contract("null, _ -> false")
	public static boolean isAnnotated(@Nullable AnnotatedElement element, Class<? extends Annotation> annotationType) {
		return findAnnotation(element, annotationType).isPresent();
	}

	/**
	 * @see org.junit.platform.commons.support.AnnotationSupport#findAnnotation(Optional, Class)
	 */
	@SuppressWarnings({ "OptionalAssignedToNull", "NullableOptional" })
	public static <A extends Annotation> Optional<A> findAnnotation(
			@Nullable Optional<? extends AnnotatedElement> element, Class<A> annotationType) {

		if (element == null || element.isEmpty()) {
			return Optional.empty();
		}

		return findAnnotation(element.get(), annotationType);
	}

	/**
	 * @since 1.8
	 * @see #findAnnotation(AnnotatedElement, Class)
	 */
	public static <A extends Annotation> Optional<A> findAnnotation(Parameter parameter, int index,
			Class<A> annotationType) {

		return findAnnotation(getEffectiveAnnotatedParameter(parameter, index), annotationType);
	}

	/**
	 * @see org.junit.platform.commons.support.AnnotationSupport#findAnnotation(AnnotatedElement, Class)
	 */
	public static <A extends Annotation> Optional<A> findAnnotation(@Nullable AnnotatedElement element,
			Class<A> annotationType) {
		Preconditions.notNull(annotationType, "annotationType must not be null");
		boolean inherited = annotationType.isAnnotationPresent(Inherited.class);
		return findAnnotation(element, annotationType, inherited, new HashSet<>());
	}

	private static <A extends Annotation> Optional<A> findAnnotation(@Nullable AnnotatedElement element,
			Class<A> annotationType, boolean inherited, Set<Annotation> visited) {

		Preconditions.notNull(annotationType, "annotationType must not be null");

		if (element == null) {
			return Optional.empty();
		}

		// Directly present?
		A annotation = element.getDeclaredAnnotation(annotationType);
		if (annotation != null) {
			return Optional.of(annotation);
		}

		// Meta-present on directly present annotations?
		Optional<A> directMetaAnnotation = findMetaAnnotation(annotationType, element.getDeclaredAnnotations(),
			inherited, visited);
		if (directMetaAnnotation.isPresent()) {
			return directMetaAnnotation;
		}

		if (element instanceof Class<?> clazz) {

			// Search on interfaces
			for (Class<?> ifc : clazz.getInterfaces()) {
				if (ifc != Annotation.class) {
					Optional<A> annotationOnInterface = findAnnotation(ifc, annotationType, inherited, visited);
					if (annotationOnInterface.isPresent()) {
						return annotationOnInterface;
					}
				}
			}

			// Indirectly present?
			// Search in class hierarchy
			if (inherited) {
				Class<?> superclass = clazz.getSuperclass();
				if (superclass != null && superclass != Object.class) {
					Optional<A> annotationOnSuperclass = findAnnotation(superclass, annotationType, inherited, visited);
					if (annotationOnSuperclass.isPresent()) {
						return annotationOnSuperclass;
					}
				}
			}
		}

		// Meta-present on indirectly present annotations?
		return findMetaAnnotation(annotationType, element.getAnnotations(), inherited, visited);
	}

	private static <A extends Annotation> Optional<A> findMetaAnnotation(Class<A> annotationType,
			Annotation[] candidates, boolean inherited, Set<Annotation> visited) {

		for (Annotation candidateAnnotation : candidates) {
			Class<? extends Annotation> candidateAnnotationType = candidateAnnotation.annotationType();
			if (!isInJavaLangAnnotationPackage(candidateAnnotationType) && visited.add(candidateAnnotation)) {
				Optional<A> metaAnnotation = findAnnotation(candidateAnnotationType, annotationType, inherited,
					visited);
				if (metaAnnotation.isPresent()) {
					return metaAnnotation;
				}
			}
		}
		return Optional.empty();
	}

	/**
	 * Find the first annotation of the specified type that is either
	 * <em>directly present</em>, <em>meta-present</em>, or <em>indirectly
	 * present</em> on the supplied class, optionally searching recursively
	 * through the enclosing class hierarchy if not found on the supplied class.
	 *
	 * <p>The enclosing class hierarchy will only be searched above an <em>inner
	 * class</em> (i.e., a non-static member class).
	 *
	 * @param <A> the annotation type
	 * @param clazz the class on which to search for the annotation; may be {@code null}
	 * @param annotationType the annotation type to search for; never {@code null}
	 * @param searchEnclosingClasses whether the enclosing class hierarchy should
	 * be searched
	 * @return an {@code Optional} containing the annotation; never {@code null} but
	 * potentially empty
	 * @since 1.8
	 * @see #findAnnotation(AnnotatedElement, Class)
	 */
	public static <A extends Annotation> Optional<A> findAnnotation(@Nullable Class<?> clazz, Class<A> annotationType,
			boolean searchEnclosingClasses) {

		Preconditions.notNull(annotationType, "annotationType must not be null");

		if (!searchEnclosingClasses) {
			return findAnnotation(clazz, annotationType);
		}

		Class<?> candidate = clazz;
		while (candidate != null) {
			Optional<A> annotation = findAnnotation(candidate, annotationType);
			if (annotation.isPresent()) {
				return annotation;
			}
			candidate = (isInnerClass(candidate) ? candidate.getEnclosingClass() : null);
		}
		return Optional.empty();
	}

	/**
	 * @since 1.5
	 * @see org.junit.platform.commons.support.AnnotationSupport#findRepeatableAnnotations(Optional, Class)
	 */
	@SuppressWarnings({ "OptionalAssignedToNull", "NullableOptional" })
	public static <A extends Annotation> List<A> findRepeatableAnnotations(
			@Nullable Optional<? extends AnnotatedElement> element, Class<A> annotationType) {

		if (element == null || element.isEmpty()) {
			return Collections.emptyList();
		}

		return findRepeatableAnnotations(element.get(), annotationType);
	}

	/**
	 * @since 1.8
	 * @see #findRepeatableAnnotations(AnnotatedElement, Class)
	 */
	public static <A extends Annotation> List<A> findRepeatableAnnotations(Parameter parameter, int index,
			Class<A> annotationType) {

		return findRepeatableAnnotations(getEffectiveAnnotatedParameter(parameter, index), annotationType);
	}

	/**
	 * @see org.junit.platform.commons.support.AnnotationSupport#findRepeatableAnnotations(AnnotatedElement, Class)
	 */
	public static <A extends Annotation> List<A> findRepeatableAnnotations(@Nullable AnnotatedElement element,
			Class<A> annotationType) {

		Preconditions.notNull(annotationType, "annotationType must not be null");
		Repeatable repeatable = annotationType.getAnnotation(Repeatable.class);
		Preconditions.notNull(repeatable, () -> annotationType.getName() + " must be @Repeatable");
		Class<? extends Annotation> containerType = repeatable.value();
		boolean inherited = containerType.isAnnotationPresent(Inherited.class);

		// Short circuit the search algorithm.
		if (element == null) {
			return Collections.emptyList();
		}

		// We use a LinkedHashSet because the search algorithm may discover
		// duplicates, but we need to maintain the original order.
		Set<A> found = new LinkedHashSet<>(16);
		findRepeatableAnnotations(element, annotationType, containerType, inherited, found, new HashSet<>(16));
		// unmodifiable since returned from public, non-internal method(s)
		return List.copyOf(found);
	}

	private static <A extends Annotation> void findRepeatableAnnotations(AnnotatedElement element,
			Class<A> annotationType, Class<? extends Annotation> containerType, boolean inherited, Set<A> found,
			Set<Annotation> visited) {

		if (element instanceof Class<?> clazz) {

			// Recurse first in order to support top-down semantics for inherited, repeatable annotations.
			if (inherited) {
				Class<?> superclass = clazz.getSuperclass();
				if (superclass != null && superclass != Object.class) {
					findRepeatableAnnotations(superclass, annotationType, containerType, inherited, found, visited);
				}
			}

			// Search on interfaces
			for (Class<?> ifc : clazz.getInterfaces()) {
				if (ifc != Annotation.class) {
					findRepeatableAnnotations(ifc, annotationType, containerType, inherited, found, visited);
				}
			}
		}

		// Find annotations that are directly present or meta-present on directly present annotations.
		findRepeatableAnnotations(element.getDeclaredAnnotations(), annotationType, containerType, inherited, found,
			visited);

		// Find annotations that are indirectly present or meta-present on indirectly present annotations.
		findRepeatableAnnotations(element.getAnnotations(), annotationType, containerType, inherited, found, visited);
	}

	@SuppressWarnings({ "unchecked", "GetClassOnAnnotation" })
	private static <A extends Annotation> void findRepeatableAnnotations(Annotation[] candidates,
			Class<A> annotationType, Class<? extends Annotation> containerType, boolean inherited, Set<A> found,
			Set<Annotation> visited) {

		for (Annotation candidate : candidates) {
			Class<? extends Annotation> candidateAnnotationType = candidate.annotationType();
			if (!isInJavaLangAnnotationPackage(candidateAnnotationType) && visited.add(candidate)) {
				// Exact match?
				if (candidateAnnotationType.equals(annotationType)) {
					found.add(annotationType.cast(candidate));
				}
				// Container?
				else if (candidateAnnotationType.equals(containerType)) {
					// Note: it's not a legitimate containing annotation type if it doesn't declare
					// a 'value' attribute that returns an array of the contained annotation type.
					// See https://docs.oracle.com/javase/specs/jls/se8/html/jls-9.html#jls-9.6.3
					Method method = ReflectionUtils.tryToGetMethod(containerType, "value").getOrThrow(
						cause -> new JUnitException(
							"Container annotation type '%s' must declare a 'value' attribute of type %s[].".formatted(
								containerType, annotationType),
							cause));

					Annotation[] containedAnnotations = (Annotation[]) ReflectionUtils.invokeMethod(method, candidate);
					found.addAll((Collection<? extends A>) asList(requireNonNull(containedAnnotations)));
				}
				// Nested container annotation?
				else if (isRepeatableAnnotationContainer(candidateAnnotationType)) {
					Method method = ReflectionUtils.tryToGetMethod(candidateAnnotationType, "value").toOptional().get();
					Annotation[] containedAnnotations = (Annotation[]) ReflectionUtils.invokeMethod(method, candidate);

					for (Annotation containedAnnotation : requireNonNull(containedAnnotations)) {
						findRepeatableAnnotations(containedAnnotation.getClass(), annotationType, containerType,
							inherited, found, visited);
					}
				}
				// Otherwise search recursively through the meta-annotation hierarchy...
				else {
					findRepeatableAnnotations(candidateAnnotationType, annotationType, containerType, inherited, found,
						visited);
				}
			}
		}
	}

	/**
	 * Determine if the supplied annotation type is a container for a repeatable
	 * annotation.
	 *
	 * @since 1.5
	 */
	private static boolean isRepeatableAnnotationContainer(Class<? extends Annotation> candidateContainerType) {
		return repeatableAnnotationContainerCache.computeIfAbsent(candidateContainerType, candidate -> {
			// @formatter:off
			Repeatable repeatable = Arrays.stream(candidate.getMethods())
					.filter(attribute -> "value".equals(attribute.getName()) && attribute.getReturnType().isArray())
					.findFirst()
					.map(attribute -> attribute.getReturnType().getComponentType().getAnnotation(Repeatable.class))
					.orElse(null);
			// @formatter:on

			return repeatable != null && candidate.equals(repeatable.value());
		});
	}

	/**
	 * Due to a bug in {@code javac} on JDK versions prior to JDK 9, looking up
	 * annotations directly on a {@link Parameter} will fail for inner class
	 * constructors.
	 *
	 * <h3>Bug in {@code javac} on JDK versions prior to JDK 9</h3>
	 *
	 * <p>The parameter annotations array in the compiled byte code for the user's
	 * class excludes an entry for the implicit <em>enclosing instance</em>
	 * parameter for an inner class constructor.
	 *
	 * <h3>Workaround</h3>
	 *
	 * <p>This method provides a workaround for this off-by-one error by helping
	 * JUnit maintainers and extension authors to access annotations on the preceding
	 * {@link Parameter} object (i.e., {@code index - 1}). If the supplied
	 * {@code index} is zero in such situations this method will return {@code null}
	 * since the parameter for the implicit <em>enclosing instance</em> will never
	 * be annotated.
	 *
	 * <h4>WARNING</h4>
	 *
	 * <p>The {@code AnnotatedElement} returned by this method should never be cast and
	 * treated as a {@code Parameter} since the metadata (e.g., {@link Parameter#getName()},
	 * {@link Parameter#getType()}, etc.) will not match those for the declared parameter
	 * at the given index in an inner class constructor for code compiled with JDK 8.
	 *
	 * @return the supplied {@code Parameter}, or the <em>effective</em> {@code Parameter}
	 * if the aforementioned bug is detected, or {@code null} if the bug is detected and
	 * the supplied {@code index} is {@code 0}
	 * @since 1.8
	 */
	private static @Nullable AnnotatedElement getEffectiveAnnotatedParameter(Parameter parameter, int index) {
		Preconditions.notNull(parameter, "Parameter must not be null");
		Executable executable = parameter.getDeclaringExecutable();

		if (executable instanceof Constructor && isInnerClass(executable.getDeclaringClass())
				&& executable.getParameterAnnotations().length == executable.getParameterCount() - 1) {

			if (index == 0) {
				return null;
			}

			return executable.getParameters()[index - 1];
		}

		return parameter;
	}

	/**
	 * @see org.junit.platform.commons.support.AnnotationSupport#findPublicAnnotatedFields(Class, Class, Class)
	 */
	public static List<Field> findPublicAnnotatedFields(Class<?> clazz, Class<?> fieldType,
			Class<? extends Annotation> annotationType) {

		Preconditions.notNull(clazz, "Class must not be null");
		Preconditions.notNull(fieldType, "fieldType must not be null");
		Preconditions.notNull(annotationType, "annotationType must not be null");

		// @formatter:off
		return Arrays.stream(clazz.getFields())
				.filter(field -> fieldType.isAssignableFrom(field.getType()) && isAnnotated(field, annotationType))
				.toList();
		// @formatter:on
	}

	/**
	 * @see org.junit.platform.commons.support.AnnotationSupport#findAnnotatedFields(Class, Class, Predicate)
	 * @see #findAnnotatedFields(Class, Class, Predicate, HierarchyTraversalMode)
	 */
	public static List<Field> findAnnotatedFields(Class<?> clazz, Class<? extends Annotation> annotationType,
			Predicate<Field> predicate) {

		return findAnnotatedFields(clazz, annotationType, predicate, HierarchyTraversalMode.TOP_DOWN);
	}

	/**
	 * Find all {@linkplain Field fields} of the supplied class or interface
	 * that are annotated or <em>meta-annotated</em> with the specified
	 * {@code annotationType} and match the specified {@code predicate}.
	 *
	 * @param clazz the class or interface in which to find the fields; never {@code null}
	 * @param annotationType the annotation type to search for; never {@code null}
	 * @param predicate the field filter; never {@code null}
	 * @param traversalMode the hierarchy traversal mode; never {@code null}
	 * @return the list of all such fields found; neither {@code null} nor mutable
	 */
	public static List<Field> findAnnotatedFields(Class<?> clazz, Class<? extends Annotation> annotationType,
			Predicate<Field> predicate, HierarchyTraversalMode traversalMode) {

		Preconditions.notNull(clazz, "Class must not be null");
		Preconditions.notNull(annotationType, "annotationType must not be null");
		Preconditions.notNull(predicate, "Predicate must not be null");

		Predicate<Field> annotated = field -> isAnnotated(field, annotationType);

		return ReflectionUtils.findFields(clazz, annotated.and(predicate), traversalMode);
	}

	/**
	 * @see org.junit.platform.commons.support.AnnotationSupport#findAnnotatedMethods(Class, Class, org.junit.platform.commons.support.HierarchyTraversalMode)
	 */
	public static List<Method> findAnnotatedMethods(Class<?> clazz, Class<? extends Annotation> annotationType,
			HierarchyTraversalMode traversalMode) {

		Preconditions.notNull(clazz, "Class must not be null");
		Preconditions.notNull(annotationType, "annotationType must not be null");

		return ReflectionUtils.findMethods(clazz, method -> isAnnotated(method, annotationType), traversalMode);
	}

	private static boolean isInJavaLangAnnotationPackage(Class<? extends Annotation> annotationType) {
		return (annotationType != null && annotationType.getName().startsWith("java.lang.annotation"));
	}

}
