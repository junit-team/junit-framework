/*
 * Copyright 2015-2026 the original author or authors.
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v2.0 which
 * accompanies this distribution and is available at
 *
 * https://www.eclipse.org/legal/epl-v20.html
 */

package org.junit.platform.engine.support.descriptor;

import static org.apiguardian.api.API.Status.STABLE;
import static org.junit.platform.commons.util.ClassUtils.nullSafeToString;

import java.io.Serial;
import java.lang.reflect.Method;
import java.util.Objects;

import org.apiguardian.api.API;
import org.jspecify.annotations.Nullable;
import org.junit.platform.commons.PreconditionViolationException;
import org.junit.platform.commons.support.ReflectionSupport;
import org.junit.platform.commons.util.Preconditions;
import org.junit.platform.commons.util.StringUtils;
import org.junit.platform.commons.util.ToStringBuilder;
import org.junit.platform.engine.TestSource;

/**
 * Method based {@link org.junit.platform.engine.TestSource TestSource}.
 *
 * <p>This class stores the method name along with the names of its parameter
 * types because {@link Method} does not implement {@link java.io.Serializable}.
 *
 * @since 1.0
 * @see org.junit.platform.engine.discovery.MethodSelector
 */
@API(status = STABLE, since = "1.0")
public final class MethodSource implements TestSource {

	@Serial
	private static final long serialVersionUID = 1L;

	/**
	 * Create a new {@code MethodSource} using the supplied class name and
	 * method name.
	 *
	 * @param className the class name; must not be {@code null} or blank
	 * @param methodName the method name; must not be {@code null} or blank
	 */
	public static MethodSource from(String className, String methodName) {
		return new MethodSource(className, methodName);
	}

	/**
	 * Create a new {@code MethodSource} using the supplied class name, method
	 * name, and method parameter types.
	 *
	 * @param className the class name; must not be {@code null} or blank
	 * @param methodName the method name; must not be {@code null} or blank
	 * @param methodParameterTypes a comma-separated list of fully qualified
	 * class names representing the method parameter types
	 */
	public static MethodSource from(String className, String methodName, String methodParameterTypes) {
		return new MethodSource(className, methodName, methodParameterTypes);
	}

	/**
	 * Create a new {@code MethodSource} using the supplied class name, method
	 * name, and method parameter types.
	 *
	 * @param className the class name; must not be {@code null} or blank
	 * @param methodName the method name; must not be {@code null} or blank
	 * @param methodParameterTypes a varargs array of classes representing the
	 * method parameter types
	 * @since 1.5
	 */
	@API(status = STABLE, since = "1.5")
	public static MethodSource from(String className, String methodName, Class<?>... methodParameterTypes) {
		return new MethodSource(className, methodName, nullSafeToString(methodParameterTypes));
	}

	/**
	 * Create a new {@code MethodSource} using the supplied {@link Method method}.
	 *
	 * @param testMethod the Java method; must not be {@code null}
	 * @see #from(Class, Method)
	 */
	public static MethodSource from(Method testMethod) {
		return new MethodSource(testMethod);
	}

	/**
	 * Create a new {@code MethodSource} using the supplied
	 * {@link Class class} and {@link Method method}.
	 *
	 * <p>This method should be used in favor of {@link #from(Method)} if the
	 * test method is inherited from a superclass or present as an interface
	 * {@code default} method.
	 *
	 * @param testClass the Java class; must not be {@code null}
	 * @param testMethod the Java method; must not be {@code null}
	 * @since 1.3
	 */
	@API(status = STABLE, since = "1.3")
	public static MethodSource from(Class<?> testClass, Method testMethod) {
		return new MethodSource(testClass, testMethod);
	}

	private final String className;
	private final String methodName;

	private final @Nullable String methodParameterTypes;

	private @Nullable Class<?> javaClass;

	private transient @Nullable Method javaMethod;

	private MethodSource(String className, String methodName) {
		this(className, methodName, null);
	}

	private MethodSource(String className, String methodName, @Nullable String methodParameterTypes) {
		Preconditions.notBlank(className, "Class name must not be null or blank");
		Preconditions.notBlank(methodName, "Method name must not be null or blank");
		this.className = className;
		this.methodName = methodName;
		this.methodParameterTypes = methodParameterTypes;
	}

	private MethodSource(Method testMethod) {
		this(Preconditions.notNull(testMethod, "Method must not be null").getDeclaringClass(), testMethod);
	}

	/**
	 * @since 1.3
	 */
	private MethodSource(Class<?> testClass, Method testMethod) {
		Preconditions.notNull(testClass, "Class must not be null");
		Preconditions.notNull(testMethod, "Method must not be null");
		this.className = testClass.getName();
		this.methodName = testMethod.getName();
		this.methodParameterTypes = nullSafeToString(testMethod.getParameterTypes());
		this.javaClass = testClass;
		this.javaMethod = testMethod;
	}

	/**
	 * Get the class name of this source.
	 */
	public String getClassName() {
		return this.className;
	}

	/**
	 * Get the method name of this source.
	 */
	public String getMethodName() {
		return this.methodName;
	}

	/**
	 * Get the method parameter types of this source.
	 */
	public @Nullable String getMethodParameterTypes() {
		return this.methodParameterTypes;
	}

	/**
	 * Get the {@linkplain Class Java class} of this source.
	 *
	 * <p>If the {@link Class} was not provided, but only the name, this method
	 * attempts to lazily load the {@link Class} based on its name and throws a
	 * {@link PreconditionViolationException} if the class cannot be loaded.
	 *
	 * @since 1.7
	 * @see #getClassName()
	 */
	@API(status = STABLE, since = "1.7")
	public Class<?> getJavaClass() {
		return lazyLoadJavaClass();
	}

	/**
	 * Get the {@linkplain Method Java method} of this source.
	 *
	 * <p>If the {@link Method} was not provided, but only the name, this method
	 * attempts to lazily load the {@code Method} based on its name and throws a
	 * {@link PreconditionViolationException} if the method cannot be loaded.
	 *
	 * @since 1.7
	 * @see #getMethodName()
	 */
	@API(status = STABLE, since = "1.7")
	public Method getJavaMethod() {
		return lazyLoadJavaMethod();
	}

	private Class<?> lazyLoadJavaClass() {
		if (this.javaClass == null) {
			// @formatter:off
			this.javaClass = ReflectionSupport.tryToLoadClass(this.className).getNonNullOrThrow(
				cause -> new PreconditionViolationException("Could not load class with name: " + this.className, cause));
			// @formatter:on
		}
		return this.javaClass;
	}

	private Method lazyLoadJavaMethod() {
		if (this.javaMethod == null) {
			Class<?> javaClass = getJavaClass();
			if (StringUtils.isNotBlank(this.methodParameterTypes)) {
				this.javaMethod = ReflectionSupport.findMethod(javaClass, this.methodName,
					this.methodParameterTypes).orElseThrow(
						() -> new PreconditionViolationException(
							"Could not find method with name [%s] and parameter types [%s] in class [%s].".formatted(
								this.methodName, this.methodParameterTypes, javaClass.getName())));
			}
			else {
				this.javaMethod = ReflectionSupport.findMethod(javaClass, this.methodName).orElseThrow(
					() -> new PreconditionViolationException(
						"Could not find method with name [%s] in class [%s].".formatted(this.methodName,
							javaClass.getName())));
			}
		}
		return this.javaMethod;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) {
			return true;
		}
		if (o == null || getClass() != o.getClass()) {
			return false;
		}
		MethodSource that = (MethodSource) o;
		return Objects.equals(this.className, that.className)//
				&& Objects.equals(this.methodName, that.methodName)//
				&& Objects.equals(this.methodParameterTypes, that.methodParameterTypes);
	}

	@Override
	public int hashCode() {
		return Objects.hash(this.className, this.methodName, this.methodParameterTypes);
	}

	@Override
	public String toString() {
		// @formatter:off
		return new ToStringBuilder(this)
				.append("className", this.className)
				.append("methodName", this.methodName)
				.append("methodParameterTypes", this.methodParameterTypes)
				.toString();
		// @formatter:on
	}

}
