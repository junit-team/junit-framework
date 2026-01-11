/*
 * Copyright 2015-2026 the original author or authors.
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v2.0 which
 * accompanies this distribution and is available at
 *
 * https://www.eclipse.org/legal/epl-v20.html
 */

package org.junit.jupiter.engine.execution;

import static java.util.stream.Collectors.joining;
import static org.apiguardian.api.API.Status.INTERNAL;
import static org.junit.platform.commons.util.KotlinReflectionUtils.getKotlinSuspendingFunctionParameters;
import static org.junit.platform.commons.util.KotlinReflectionUtils.isKotlinSuspendingFunction;
import static org.junit.platform.commons.util.ReflectionUtils.isAssignableTo;

import java.lang.reflect.Constructor;
import java.lang.reflect.Executable;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.List;
import java.util.Optional;

import org.apiguardian.api.API;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.ParameterContext;
import org.junit.jupiter.api.extension.ParameterResolutionException;
import org.junit.jupiter.api.extension.ParameterResolver;
import org.junit.jupiter.engine.extension.ExtensionRegistry;
import org.junit.platform.commons.logging.Logger;
import org.junit.platform.commons.logging.LoggerFactory;
import org.junit.platform.commons.util.Preconditions;
import org.junit.platform.commons.util.StringUtils;
import org.junit.platform.commons.util.UnrecoverableExceptions;

/**
 * {@code ParameterResolutionUtils} provides support for dynamic resolution
 * of executable parameters via {@link ParameterResolver ParameterResolvers}.
 *
 * @since 5.9
 */
@API(status = INTERNAL, since = "5.9")
public class ParameterResolutionUtils {

	private static final Logger logger = LoggerFactory.getLogger(ParameterResolutionUtils.class);

	/**
	 * Resolve the array of parameters for the supplied method and target.
	 *
	 * @param method the method for which to resolve parameters
	 * @param target an {@code Optional} containing the target on which the
	 * executable will be invoked; never {@code null} but should be empty for
	 * static methods and constructors
	 * @param extensionContext the current {@code ExtensionContext}
	 * @param extensionRegistry the {@code ExtensionRegistry} to retrieve
	 * {@code ParameterResolvers} from
	 * @return the array of Objects to be used as parameters in the executable
	 * invocation; never {@code null} though potentially empty
	 */
	public static @Nullable Object[] resolveParameters(Method method, Optional<Object> target,
			ExtensionContext extensionContext, ExtensionRegistry extensionRegistry) {

		return resolveParameters(method, target, Optional.empty(), __ -> extensionContext, extensionRegistry,
			isKotlinSuspendingFunction(method) //
					? getKotlinSuspendingFunctionParameters(method) //
					: method.getParameters());
	}

	/**
	 * Resolve the array of parameters for the supplied executable, target, and
	 * outer instance.
	 *
	 * @param executable the executable for which to resolve parameters
	 * @param target an {@code Optional} containing the target on which the
	 * executable will be invoked; never {@code null} but should be empty for
	 * static methods and constructors
	 * @param outerInstance the outer instance that will be supplied as the
	 * first argument to a constructor for an inner class; should be {@code null}
	 * for methods and constructors for top-level or static classes
	 * @param extensionContext the current {@code ExtensionContext}
	 * @param extensionRegistry the {@code ExtensionRegistry} to retrieve
	 * {@code ParameterResolvers} from
	 * @return the array of Objects to be used as parameters in the executable
	 * invocation; never {@code null} though potentially empty
	 */
	public static @Nullable Object[] resolveParameters(Executable executable, Optional<Object> target,
			Optional<Object> outerInstance, ExtensionContext extensionContext, ExtensionRegistry extensionRegistry) {
		return resolveParameters(executable, target, outerInstance, __ -> extensionContext, extensionRegistry);
	}

	public static @Nullable Object[] resolveParameters(Executable executable, Optional<Object> target,
			Optional<Object> outerInstance, ExtensionContextSupplier extensionContext,
			ExtensionRegistry extensionRegistry) {

		return resolveParameters(executable, target, outerInstance, extensionContext, extensionRegistry,
			executable.getParameters());
	}

	private static @Nullable Object[] resolveParameters(Executable executable, Optional<Object> target,
			Optional<Object> outerInstance, ExtensionContextSupplier extensionContext,
			ExtensionRegistry extensionRegistry, Parameter[] parameters) {

		Preconditions.notNull(target, "target must not be null");

		@Nullable
		Object[] values = new Object[parameters.length];
		int start = 0;

		// Ensure that the outer instance is resolved as the first parameter if
		// the executable is a constructor for an inner class.
		if (outerInstance.isPresent()) {
			values[0] = outerInstance.get();
			start = 1;
		}

		// Resolve remaining parameters dynamically
		for (int i = start; i < parameters.length; i++) {
			ParameterContext parameterContext = new DefaultParameterContext(parameters[i], i, target);
			values[i] = resolveParameter(parameterContext, executable, extensionContext, extensionRegistry);
		}
		return values;
	}

	private static @Nullable Object resolveParameter(ParameterContext parameterContext, Executable executable,
			ExtensionContextSupplier extensionContext, ExtensionRegistry extensionRegistry) {

		try {
			// @formatter:off
			List<ParameterResolver> matchingResolvers = extensionRegistry.stream(ParameterResolver.class)
					.filter(resolver -> resolver.supportsParameter(parameterContext, extensionContext.get(resolver)))
					.toList();
			// @formatter:on

			if (matchingResolvers.isEmpty()) {
				throw new ParameterResolutionException(
					"No ParameterResolver registered for parameter [%s] in %s [%s].".formatted(
						parameterContext.getParameter(), asLabel(executable), executable.toGenericString()));
			}

			if (matchingResolvers.size() > 1) {
				// @formatter:off
				String resolvers = matchingResolvers.stream()
						.map(StringUtils::defaultToString)
						.collect(joining(", "));
				// @formatter:on
				throw new ParameterResolutionException(
					"Discovered multiple competing ParameterResolvers for parameter [%s] in %s [%s]: %s".formatted(
						parameterContext.getParameter(), asLabel(executable), executable.toGenericString(), resolvers));
			}

			ParameterResolver resolver = matchingResolvers.get(0);
			Object value = resolver.resolveParameter(parameterContext, extensionContext.get(resolver));
			validateResolvedType(parameterContext.getParameter(), value, executable, resolver);

			logger.trace(
				() -> "ParameterResolver [%s] resolved a value of type [%s] for parameter [%s] in %s [%s].".formatted(
					resolver.getClass().getName(), (value != null ? value.getClass().getTypeName() : null),
					parameterContext.getParameter(), asLabel(executable), executable.toGenericString()));

			return value;
		}
		catch (ParameterResolutionException ex) {
			throw ex;
		}
		catch (Throwable throwable) {
			UnrecoverableExceptions.rethrowIfUnrecoverable(throwable);

			String message = "Failed to resolve parameter [%s] in %s [%s]".formatted(parameterContext.getParameter(),
				asLabel(executable), executable.toGenericString());

			if (StringUtils.isNotBlank(throwable.getMessage())) {
				message += ": " + throwable.getMessage();
			}

			throw new ParameterResolutionException(message, throwable);
		}
	}

	private static void validateResolvedType(Parameter parameter, @Nullable Object value, Executable executable,
			ParameterResolver resolver) {

		Class<?> type = parameter.getType();

		// Note: null is permissible as a resolved value but only for non-primitive types.
		if (!isAssignableTo(value, type)) {
			String message;
			if (value == null && type.isPrimitive()) {
				message = """
						ParameterResolver [%s] resolved a null value for parameter [%s] \
						in %s [%s], but a primitive of type [%s] is required.""".formatted(
					resolver.getClass().getName(), parameter, asLabel(executable), executable.toGenericString(),
					type.getName());
			}
			else {
				message = """
						ParameterResolver [%s] resolved a value of type [%s] for parameter [%s] \
						in %s [%s], but a value assignment compatible with [%s] is required.""".formatted(
					resolver.getClass().getName(), (value != null ? value.getClass().getTypeName() : null), parameter,
					asLabel(executable), executable.toGenericString(), type.getTypeName());
			}

			throw new ParameterResolutionException(message);
		}
	}

	private static String asLabel(Executable executable) {
		return executable instanceof Constructor ? "constructor" : "method";
	}

	private ParameterResolutionUtils() {
	}

}
