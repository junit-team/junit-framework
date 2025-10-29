/*
 * Copyright 2015-2025 the original author or authors.
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v2.0 which
 * accompanies this distribution and is available at
 *
 * https://www.eclipse.org/legal/epl-v20.html
 */

package org.junit.platform.engine.discovery;

import static org.apiguardian.api.API.Status.INTERNAL;
import static org.apiguardian.api.API.Status.MAINTAINED;
import static org.apiguardian.api.API.Status.STABLE;

import java.lang.reflect.Method;
import java.util.Objects;
import java.util.Optional;

import org.apiguardian.api.API;
import org.jspecify.annotations.Nullable;
import org.junit.platform.commons.JUnitException;
import org.junit.platform.commons.PreconditionViolationException;
import org.junit.platform.commons.function.Try;
import org.junit.platform.commons.support.ReflectionSupport;
import org.junit.platform.commons.util.ClassUtils;
import org.junit.platform.commons.util.ReflectionUtils;
import org.junit.platform.commons.util.ToStringBuilder;
import org.junit.platform.engine.DiscoverySelector;
import org.junit.platform.engine.DiscoverySelectorIdentifier;

/**
 * A {@link DiscoverySelector} that selects a {@link Method} or a combination of
 * class name, method name, and parameter types so that
 * {@link org.junit.platform.engine.TestEngine TestEngines} can discover tests
 * or containers based on methods.
 *
 * <p>If a Java {@link Method} is provided, the selector will return that
 * {@linkplain #getJavaMethod() method} and its method name, class name, and
 * parameter types accordingly. If a {@link Class} and method name, a class name
 * and method name, or a <em>fully qualified method name</em> is provided,
 * this selector will only attempt to lazily load the class, method, or parameter
 * types if {@link #getJavaClass()}, {@link #getJavaMethod()}, or
 * {@link #getParameterTypes()} is invoked.
 *
 * <p>In this context, a Java {@code Method} means anything that can be referenced
 * as a {@link Method} on the JVM &mdash; for example, methods from Java classes
 * or methods from other JVM languages such Groovy, Scala, etc.
 *
 * @since 1.0
 * @see DiscoverySelectors#selectMethod(String)
 * @see DiscoverySelectors#selectMethod(String, String)
 * @see DiscoverySelectors#selectMethod(String, String, String)
 * @see DiscoverySelectors#selectMethod(Class, String)
 * @see DiscoverySelectors#selectMethod(Class, String, String)
 * @see DiscoverySelectors#selectMethod(Class, Method)
 * @see org.junit.platform.engine.support.descriptor.MethodSource
 */
@API(status = STABLE, since = "1.0")
public final class MethodSelector implements DiscoverySelector {

	private final @Nullable ClassLoader classLoader;
	private final String className;
	private final String methodName;
	private final String parameterTypeNames;

	private volatile @Nullable Class<?> javaClass;

	private volatile @Nullable Method javaMethod;

	private volatile Class<?> @Nullable [] parameterTypes;

	/**
	 * @since 1.10
	 */
	MethodSelector(@Nullable ClassLoader classLoader, String className, String methodName, String parameterTypeNames) {
		this.classLoader = classLoader;
		this.className = className;
		this.methodName = methodName;
		this.parameterTypeNames = parameterTypeNames;
	}

	MethodSelector(Class<?> javaClass, String methodName, String parameterTypeNames) {
		this(javaClass.getClassLoader(), javaClass.getName(), methodName, parameterTypeNames);
		this.javaClass = javaClass;
	}

	/**
	 * @since 1.10
	 */
	MethodSelector(@Nullable ClassLoader classLoader, String className, String methodName, Class<?>... parameterTypes) {
		this(classLoader, className, methodName, ClassUtils.nullSafeToString(Class::getTypeName, parameterTypes));
		this.parameterTypes = parameterTypes.clone();
	}

	/**
	 * @since 1.10
	 */
	MethodSelector(Class<?> javaClass, String methodName, Class<?>... parameterTypes) {
		this(javaClass.getClassLoader(), javaClass.getName(), methodName,
			ClassUtils.nullSafeToString(Class::getTypeName, parameterTypes));
		this.javaClass = javaClass;
		this.parameterTypes = parameterTypes.clone();
	}

	MethodSelector(Class<?> javaClass, Method method) {
		this(javaClass, method, method.getParameterTypes());
	}

	private MethodSelector(Class<?> javaClass, Method method, Class<?>... parameterTypes) {
		this(javaClass.getClassLoader(), javaClass.getName(), method.getName(),
			ClassUtils.nullSafeToString(Class::getTypeName, parameterTypes));
		this.javaClass = javaClass;
		this.javaMethod = method;
		this.parameterTypes = parameterTypes;
	}

	/**
	 * Get the {@link ClassLoader} used to load the specified class.
	 *
	 * @return the {@code ClassLoader}; potentially {@code null}
	 * @since 1.10
	 */
	@API(status = MAINTAINED, since = "1.13.3")
	public @Nullable ClassLoader getClassLoader() {
		return this.classLoader;
	}

	/**
	 * Get the selected class name.
	 */
	public String getClassName() {
		return this.className;
	}

	/**
	 * Get the selected method name.
	 */
	public String getMethodName() {
		return this.methodName;
	}

	/**
	 * Get the names of parameter types for the selected method as a {@link String},
	 * typically a comma-separated list of primitive types, fully qualified class
	 * names, or array types.
	 *
	 * <p>Note: the names of parameter types are provided as a single string instead
	 * of a collection in order to allow this selector to be used in a generic
	 * fashion by various test engines. It is therefore the responsibility of
	 * the caller of this method to determine how to parse the returned string.
	 *
	 * @return the names of parameter types supplied to this {@code MethodSelector}
	 * via a constructor or deduced from a {@code Method} or parameter types supplied
	 * via a constructor; never {@code null} but potentially an empty string
	 * @since 1.10
	 * @see #getParameterTypes()
	 */
	@API(status = STABLE, since = "1.10")
	public String getParameterTypeNames() {
		return this.parameterTypeNames;
	}

	/**
	 * Get the {@link Class} in which the selected {@linkplain #getJavaMethod
	 * method} is declared, or a subclass thereof.
	 *
	 * <p>If the {@link Class} was not provided, but only the name, this method
	 * attempts to lazily load the {@code Class} based on its name and throws a
	 * {@link PreconditionViolationException} if the class cannot be loaded.
	 *
	 * @see #getJavaMethod()
	 */
	public Class<?> getJavaClass() {
		return lazyLoadJavaClass();
	}

	/**
	 * Get the selected {@link Method}.
	 *
	 * <p>If the {@link Method} was not provided, but only the name, this method
	 * attempts to lazily load the {@code Method} based on its name and throws a
	 * {@link PreconditionViolationException} if the method cannot be loaded.
	 *
	 * @see #getJavaClass()
	 */
	public Method getJavaMethod() {
		return lazyLoadJavaMethod();
	}

	/**
	 * Get the parameter types for the selected method.
	 *
	 * <p>If the parameter types were not provided as {@link Class} references
	 * (or could not be deduced as {@code Class} references in the constructor),
	 * this method attempts to lazily load the class reference for each parameter
	 * type based on its name and throws a {@link JUnitException} if the class
	 * cannot be loaded.
	 *
	 * @return the method's parameter types; never {@code null} but potentially
	 * an empty array if the selected method does not declare parameters
	 * @since 1.10
	 * @see #getParameterTypeNames()
	 * @see Method#getParameterTypes()
	 */
	@API(status = MAINTAINED, since = "1.13.3")
	public Class<?>[] getParameterTypes() {
		return lazyLoadParameterTypes().clone();
	}

	private Class<?> lazyLoadJavaClass() {
		Class<?> value = this.javaClass;
		if (value == null) {
			// @formatter:off
			Try<Class<?>> tryToLoadClass = this.classLoader == null
				? ReflectionSupport.tryToLoadClass(this.className)
				: ReflectionSupport.tryToLoadClass(this.className, this.classLoader);
			value = tryToLoadClass.getNonNullOrThrow(cause ->
				new PreconditionViolationException("Could not load class with name: " + this.className, cause));
			// @formatter:on
			this.javaClass = value;
		}
		return value;
	}

	private Method lazyLoadJavaMethod() {
		var value = this.javaMethod;
		if (value == null) {
			Class<?> javaClass = lazyLoadJavaClass();
			var parameterTypes = lazyLoadParameterTypes();
			if (parameterTypes.length > 0) {
				value = ReflectionSupport.findMethod(javaClass, this.methodName, parameterTypes).orElseThrow(
					() -> new PreconditionViolationException(
						"Could not find method with name [%s] and parameter types [%s] in class [%s].".formatted(
							this.methodName, this.parameterTypeNames, javaClass.getName())));
			}
			else {
				value = ReflectionSupport.findMethod(javaClass, this.methodName).orElseThrow(
					() -> new PreconditionViolationException(
						"Could not find method with name [%s] in class [%s].".formatted(this.methodName,
							javaClass.getName())));
			}
			this.javaMethod = value;
		}
		return value;
	}

	private Class<?>[] lazyLoadParameterTypes() {
		var value = this.parameterTypes;
		if (value == null) {
			value = ReflectionUtils.resolveParameterTypes(lazyLoadJavaClass(), this.methodName,
				this.parameterTypeNames);
			this.parameterTypes = value;
		}
		return value;
	}

	/**
	 * @since 1.3
	 */
	@API(status = STABLE, since = "1.3")
	@Override
	public boolean equals(Object o) {
		if (this == o) {
			return true;
		}
		if (o == null || getClass() != o.getClass()) {
			return false;
		}
		MethodSelector that = (MethodSelector) o;
		return Objects.equals(this.className, that.className)//
				&& Objects.equals(this.methodName, that.methodName)//
				&& Objects.equals(this.parameterTypeNames, that.parameterTypeNames);
	}

	/**
	 * @since 1.3
	 */
	@API(status = STABLE, since = "1.3")
	@Override
	public int hashCode() {
		return Objects.hash(this.className, this.methodName, this.parameterTypeNames);
	}

	@Override
	public String toString() {
		// @formatter:off
		return new ToStringBuilder(this)
				.append("className", getClassName())
				.append("methodName", getMethodName())
				.append("parameterTypes", getParameterTypeNames())
				.append("classLoader", getClassLoader())
				.toString();
		// @formatter:on
	}

	@Override
	public Optional<DiscoverySelectorIdentifier> toIdentifier() {
		String fullyQualifiedMethodName = ReflectionUtils.getFullyQualifiedMethodName(this.className, this.methodName,
			this.parameterTypeNames);
		return Optional.of(DiscoverySelectorIdentifier.create(IdentifierParser.PREFIX, fullyQualifiedMethodName));
	}

	/**
	 * The {@link DiscoverySelectorIdentifierParser} for {@link MethodSelector
	 * MethodSelectors}.
	 */
	@API(status = INTERNAL, since = "1.11")
	public static class IdentifierParser implements DiscoverySelectorIdentifierParser {

		private static final String PREFIX = "method";

		public IdentifierParser() {
		}

		@Override
		public String getPrefix() {
			return PREFIX;
		}

		@Override
		public Optional<MethodSelector> parse(DiscoverySelectorIdentifier identifier, Context context) {
			return Optional.of(DiscoverySelectors.selectMethod(identifier.getValue()));
		}

	}

}
