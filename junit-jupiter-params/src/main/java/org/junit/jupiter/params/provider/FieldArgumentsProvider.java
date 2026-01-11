/*
 * Copyright 2015-2026 the original author or authors.
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v2.0 which
 * accompanies this distribution and is available at
 *
 * https://www.eclipse.org/legal/epl-v20.html
 */

package org.junit.jupiter.params.provider;

import static java.util.Arrays.stream;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.Iterator;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.stream.BaseStream;
import java.util.stream.Stream;

import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.params.support.ParameterDeclarations;
import org.junit.platform.commons.JUnitException;
import org.junit.platform.commons.support.ModifierSupport;
import org.junit.platform.commons.support.ReflectionSupport;
import org.junit.platform.commons.util.ClassLoaderUtils;
import org.junit.platform.commons.util.CollectionUtils;
import org.junit.platform.commons.util.Preconditions;
import org.junit.platform.commons.util.ReflectionUtils;
import org.junit.platform.commons.util.ReflectionUtils.HierarchyTraversalMode;

/**
 * {@link ArgumentsProvider} for {@link FieldSource @FieldSource}.
 *
 * @since 5.11
 */
class FieldArgumentsProvider extends AnnotationBasedArgumentsProvider<FieldSource> {

	@Override
	protected Stream<? extends Arguments> provideArguments(ParameterDeclarations parameters, ExtensionContext context,
			FieldSource fieldSource) {
		Class<?> testClass = context.getRequiredTestClass();
		Object testInstance = context.getTestInstance().orElse(null);
		String[] fieldNames = fieldSource.value();
		if (fieldNames.length == 0) {
			Optional<Method> testMethod = context.getTestMethod();
			Preconditions.condition(testMethod.isPresent(),
				"You must specify a field name when using @FieldSource with @ParameterizedClass");
			fieldNames = new String[] { testMethod.get().getName() };
		}
		// @formatter:off
		return stream(fieldNames)
				.map(fieldName -> findField(testClass, fieldName))
				.map(field -> validateField(field, testInstance))
				.map(field -> readField(field, testInstance))
				.flatMap(fieldValue -> {
					if (fieldValue instanceof Supplier<?> supplier) {
						fieldValue = supplier.get();
					}
					return CollectionUtils.toStream(fieldValue);
				})
				.map(ArgumentsUtils::toArguments);
		// @formatter:on
	}

	// package-private for testing
	static Field findField(Class<?> testClass, String fieldName) {
		Preconditions.notBlank(fieldName, "Field name must not be blank");
		fieldName = fieldName.strip();

		Class<?> clazz = testClass;
		if (fieldName.contains("#") || fieldName.contains(".")) {
			String[] fieldParts = ReflectionUtils.parseFullyQualifiedFieldName(fieldName);
			String className = fieldParts[0];
			fieldName = fieldParts[1];
			ClassLoader classLoader = ClassLoaderUtils.getClassLoader(testClass);
			clazz = ReflectionUtils.loadRequiredClass(className, classLoader);
		}

		Class<?> resolvedClass = clazz;
		String resolvedFieldName = fieldName;
		Predicate<Field> nameMatches = field -> field.getName().equals(resolvedFieldName);
		Field field = ReflectionUtils.streamFields(resolvedClass, nameMatches, HierarchyTraversalMode.BOTTOM_UP)//
				.findFirst()//
				.orElse(null);

		return Preconditions.notNull(field,
			() -> "Could not find field named [%s] in class [%s]".formatted(resolvedFieldName,
				resolvedClass.getName()));
	}

	private static Field validateField(Field field, @Nullable Object testInstance) {
		Preconditions.condition(field.getDeclaringClass().isInstance(testInstance) || ModifierSupport.isStatic(field),
			() -> """
					Field '%s' must be static: local @FieldSource fields must be static \
					unless the PER_CLASS @TestInstance lifecycle mode is used; \
					external @FieldSource fields must always be static.""".formatted(field.toGenericString()));
		return field;
	}

	private static Object readField(Field field, @Nullable Object testInstance) {
		Object value = ReflectionSupport.tryToReadFieldValue(field, testInstance).getOrThrow(
			cause -> new JUnitException("Could not read field [%s]".formatted(field.getName()), cause));

		String fieldName = field.getName();
		String declaringClass = field.getDeclaringClass().getName();

		Preconditions.notNull(value,
			() -> "The value of field [%s] in class [%s] must not be null".formatted(fieldName, declaringClass));

		Preconditions.condition(!(value instanceof BaseStream),
			() -> "The value of field [%s] in class [%s] must not be a stream".formatted(fieldName, declaringClass));

		Preconditions.condition(!(value instanceof Iterator),
			() -> "The value of field [%s] in class [%s] must not be an Iterator".formatted(fieldName, declaringClass));

		Preconditions.condition(isConvertibleToStream(field, value),
			() -> "The value of field [%s] in class [%s] must be convertible to a Stream".formatted(fieldName,
				declaringClass));

		return value;
	}

	/**
	 * Determine if the supplied value can be converted into a {@code Stream} or
	 * if the declared type of the supplied field is a {@link Supplier} of a type
	 * that can be converted into a {@code Stream}.
	 */
	private static boolean isConvertibleToStream(Field field, Object value) {
		// Check actual value type.
		if (CollectionUtils.isConvertibleToStream(value.getClass())) {
			return true;
		}

		// Check declared type T of Supplier<T>.
		if (Supplier.class.isAssignableFrom(field.getType())) {
			Type genericType = field.getGenericType();
			if (genericType instanceof ParameterizedType parameterizedType) {
				Type[] typeArguments = parameterizedType.getActualTypeArguments();
				if (typeArguments.length == 1) {
					Type type = typeArguments[0];
					// Handle cases such as Supplier<IntStream>
					if (type instanceof Class<?> clazz) {
						return CollectionUtils.isConvertibleToStream(clazz);
					}
					// Handle cases such as Supplier<Stream<String>>
					if (type instanceof ParameterizedType innerParameterizedType) {
						Type rawType = innerParameterizedType.getRawType();
						if (rawType instanceof Class<?> clazz) {
							return CollectionUtils.isConvertibleToStream(clazz);
						}
					}
				}
			}
		}
		return false;
	}

}
