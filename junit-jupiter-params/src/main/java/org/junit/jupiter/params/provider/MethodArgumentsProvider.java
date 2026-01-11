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
import static org.junit.platform.commons.support.AnnotationSupport.isAnnotated;
import static org.junit.platform.commons.util.CollectionUtils.isConvertibleToStream;

import java.lang.reflect.Method;
import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.stream.Stream;

import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestFactory;
import org.junit.jupiter.api.TestTemplate;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.params.support.ParameterDeclarations;
import org.junit.platform.commons.PreconditionViolationException;
import org.junit.platform.commons.util.ClassLoaderUtils;
import org.junit.platform.commons.util.CollectionUtils;
import org.junit.platform.commons.util.Preconditions;
import org.junit.platform.commons.util.ReflectionUtils;
import org.junit.platform.commons.util.StringUtils;

/**
 * @since 5.0
 */
class MethodArgumentsProvider extends AnnotationBasedArgumentsProvider<MethodSource> {

	private static final Predicate<Method> isFactoryMethod = //
		method -> isConvertibleToStream(method.getReturnType()) && !isTestMethod(method);

	@Override
	protected Stream<? extends Arguments> provideArguments(ParameterDeclarations parameters, ExtensionContext context,
			MethodSource methodSource) {
		Class<?> testClass = context.getRequiredTestClass();
		Optional<Method> testMethod = context.getTestMethod();
		Object testInstance = context.getTestInstance().orElse(null);
		String[] methodNames = methodSource.value();
		// @formatter:off
		return stream(methodNames)
				.map(factoryMethodName -> findFactoryMethod(testClass, testMethod, factoryMethodName))
				.map(factoryMethod -> validateFactoryMethod(factoryMethod, testInstance))
				.map(factoryMethod -> Preconditions.notNull(context.getExecutableInvoker().invoke(factoryMethod, testInstance), () -> "@MethodSource-referenced method [%s] must not return null".formatted(factoryMethod.toGenericString())))
				.flatMap(CollectionUtils::toStream)
				.map(ArgumentsUtils::toArguments);
		// @formatter:on
	}

	private static Method findFactoryMethod(Class<?> testClass, Optional<Method> testMethod, String factoryMethodName) {
		String originalFactoryMethodName = factoryMethodName;

		// If the user did not provide a factory method name, find a "default" local
		// factory method with the same name as the parameterized test method.
		if (StringUtils.isBlank(factoryMethodName)) {
			Preconditions.condition(testMethod.isPresent(),
				"You must specify a method name when using @MethodSource with @ParameterizedClass");
			factoryMethodName = testMethod.get().getName();
			return findFactoryMethodBySimpleName(testClass, testMethod, factoryMethodName);
		}

		// Convert local factory method name to fully qualified method name.
		if (!looksLikeAFullyQualifiedMethodName(factoryMethodName)) {
			factoryMethodName = testClass.getName() + "#" + factoryMethodName;
		}

		// Find factory method using fully qualified name.
		Method factoryMethod = findFactoryMethodByFullyQualifiedName(testClass, testMethod, factoryMethodName);

		// Ensure factory method has a valid return type and is not a test method.
		Preconditions.condition(isFactoryMethod.test(factoryMethod),
			() -> "Could not find valid factory method [%s] for test class [%s] but found the following invalid candidate: %s".formatted(
				originalFactoryMethodName, testClass.getName(), factoryMethod));

		return factoryMethod;
	}

	private static boolean looksLikeAFullyQualifiedMethodName(String factoryMethodName) {
		if (factoryMethodName.contains("#")) {
			return true;
		}
		int indexOfFirstDot = factoryMethodName.indexOf('.');
		if (indexOfFirstDot == -1) {
			return false;
		}
		int indexOfLastOpeningParenthesis = factoryMethodName.lastIndexOf('(');
		if (indexOfLastOpeningParenthesis > 0) {
			// Exclude simple/local method names with parameters
			return indexOfFirstDot < indexOfLastOpeningParenthesis;
		}
		// If we get this far, we conclude the supplied factory method name "looks"
		// like it was intended to be a fully qualified method name, even if the
		// syntax is invalid. We do this in order to provide better diagnostics for
		// the user when a fully qualified method name is in fact invalid.
		return true;
	}

	// package-private for testing
	static Method findFactoryMethodByFullyQualifiedName(Class<?> testClass, Optional<Method> testMethod,
			String fullyQualifiedMethodName) {
		String[] methodParts = ReflectionUtils.parseFullyQualifiedMethodName(fullyQualifiedMethodName);
		String className = methodParts[0];
		String methodName = methodParts[1];
		String methodParameters = methodParts[2];
		ClassLoader classLoader = ClassLoaderUtils.getClassLoader(testClass);
		Class<?> clazz = ReflectionUtils.loadRequiredClass(className, classLoader);

		// Attempt to find an exact match first.
		Method factoryMethod = ReflectionUtils.findMethod(clazz, methodName, methodParameters).orElse(null);
		if (factoryMethod != null) {
			return factoryMethod;
		}

		boolean explicitParameterListSpecified = //
			StringUtils.isNotBlank(methodParameters) || fullyQualifiedMethodName.endsWith("()");

		// If we didn't find an exact match but an explicit parameter list was specified,
		// that's a user configuration error.
		Preconditions.condition(!explicitParameterListSpecified,
			() -> "Could not find factory method [%s(%s)] in class [%s]".formatted(methodName, methodParameters,
				className));

		// Otherwise, fall back to the same lenient search semantics that are used
		// to locate a "default" local factory method.
		return findFactoryMethodBySimpleName(clazz, testMethod, methodName);
	}

	/**
	 * Find the factory method by searching for all methods in the given {@code clazz}
	 * with the desired {@code factoryMethodName} which have return types that can be
	 * converted to a {@link Stream}, ignoring the {@code testMethod} itself as well
	 * as any {@code @Test}, {@code @TestTemplate}, or {@code @TestFactory} methods
	 * with the same name.
	 * @return the single factory method matching the search criteria
	 * @throws PreconditionViolationException if the factory method was not found or
	 * multiple competing factory methods with the same name were found
	 */
	private static Method findFactoryMethodBySimpleName(Class<?> clazz, Optional<Method> testMethod,
			String factoryMethodName) {
		Predicate<Method> isCandidate = candidate -> factoryMethodName.equals(candidate.getName())
				&& !candidate.equals(testMethod.orElse(null));
		List<Method> candidates = ReflectionUtils.findMethods(clazz, isCandidate);

		List<Method> factoryMethods = candidates.stream().filter(isFactoryMethod).toList();

		Preconditions.notEmpty(factoryMethods, () -> {
			if (candidates.isEmpty()) {
				// Report that we didn't find anything.
				return "Could not find factory method [%s] in class [%s]".formatted(factoryMethodName, clazz.getName());
			}
			// If we didn't find the factory method using the isFactoryMethod Predicate, perhaps
			// the specified factory method has an invalid return type or is a test method.
			// In that case, we report the invalid candidates that were found.
			return "Could not find valid factory method [%s] in class [%s] but found the following invalid candidates: %s".formatted(
				factoryMethodName, clazz.getName(), candidates);
		});
		Preconditions.condition(factoryMethods.size() == 1,
			() -> "%d factory methods named [%s] were found in class [%s]: %s".formatted(factoryMethods.size(),
				factoryMethodName, clazz.getName(), factoryMethods));
		return factoryMethods.get(0);
	}

	private static boolean isTestMethod(Method candidate) {
		return isAnnotated(candidate, Test.class) || isAnnotated(candidate, TestTemplate.class)
				|| isAnnotated(candidate, TestFactory.class);
	}

	private static Method validateFactoryMethod(Method factoryMethod, @Nullable Object testInstance) {
		Preconditions.condition(
			factoryMethod.getDeclaringClass().isInstance(testInstance) || ReflectionUtils.isStatic(factoryMethod),
			() -> """
					Method '%s' must be static: local factory methods must be static \
					unless the PER_CLASS @TestInstance lifecycle mode is used; \
					external factory methods must always be static.""".formatted(factoryMethod.toGenericString()));
		return factoryMethod;
	}

}
