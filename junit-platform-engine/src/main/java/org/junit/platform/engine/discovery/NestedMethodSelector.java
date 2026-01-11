/*
 * Copyright 2015-2026 the original author or authors.
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
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import org.apiguardian.api.API;
import org.jspecify.annotations.Nullable;
import org.junit.platform.commons.PreconditionViolationException;
import org.junit.platform.commons.util.ReflectionUtils;
import org.junit.platform.commons.util.ToStringBuilder;
import org.junit.platform.engine.DiscoverySelector;
import org.junit.platform.engine.DiscoverySelectorIdentifier;

/**
 * A {@link DiscoverySelector} that selects a nested {@link Method} or a
 * combination of enclosing class names, class name, method name, and parameter
 * types so that {@link org.junit.platform.engine.TestEngine TestEngines} can
 * discover tests or containers based on methods.
 *
 * <p>If a Java {@link Method} is provided, the selector will return that
 * {@linkplain #getMethod() method} and its method name, class name, enclosing
 * class names, and parameter types accordingly. If class names or method names
 * are provided, this selector will only attempt to lazily load a class or method
 * if {@link #getEnclosingClasses()}, {@link #getNestedClass()},
 * {@link #getMethod()}, or {@link #getParameterTypes()} is invoked.
 *
 * <p>In this context, a Java {@code Method} means anything that can be referenced
 * as a {@link Method} on the JVM &mdash; for example, methods from Java classes
 * or methods from other JVM languages such Groovy, Scala, etc.
 *
 * @since 1.6
 * @see DiscoverySelectors#selectNestedMethod(List, String, String)
 * @see DiscoverySelectors#selectNestedMethod(List, String, String, String)
 * @see DiscoverySelectors#selectNestedMethod(List, Class, String)
 * @see DiscoverySelectors#selectNestedMethod(List, Class, String, String)
 * @see DiscoverySelectors#selectNestedMethod(List, Class, Method)
 * @see org.junit.platform.engine.support.descriptor.MethodSource
 * @see NestedClassSelector
 * @see MethodSelector
 */
@API(status = STABLE, since = "1.6")
public final class NestedMethodSelector implements DiscoverySelector {

	private final NestedClassSelector nestedClassSelector;
	private final MethodSelector methodSelector;

	NestedMethodSelector(@Nullable ClassLoader classLoader, List<String> enclosingClassNames, String nestedClassName,
			String methodName, String parameterTypeNames) {
		this.nestedClassSelector = new NestedClassSelector(classLoader, enclosingClassNames, nestedClassName);
		this.methodSelector = new MethodSelector(classLoader, nestedClassName, methodName, parameterTypeNames);
	}

	/**
	 * @since 1.10
	 */
	NestedMethodSelector(@Nullable ClassLoader classLoader, List<String> enclosingClassNames, String nestedClassName,
			String methodName, Class<?>... parameterTypes) {
		this.nestedClassSelector = new NestedClassSelector(classLoader, enclosingClassNames, nestedClassName);
		this.methodSelector = new MethodSelector(classLoader, nestedClassName, methodName, parameterTypes);
	}

	NestedMethodSelector(List<Class<?>> enclosingClasses, Class<?> nestedClass, String methodName,
			String parameterTypeNames) {
		this.nestedClassSelector = new NestedClassSelector(enclosingClasses, nestedClass);
		this.methodSelector = new MethodSelector(nestedClass, methodName, parameterTypeNames);
	}

	/**
	 * @since 1.10
	 */
	NestedMethodSelector(List<Class<?>> enclosingClasses, Class<?> nestedClass, String methodName,
			Class<?>... parameterTypes) {
		this.nestedClassSelector = new NestedClassSelector(enclosingClasses, nestedClass);
		this.methodSelector = new MethodSelector(nestedClass, methodName, parameterTypes);
	}

	NestedMethodSelector(List<Class<?>> enclosingClasses, Class<?> nestedClass, Method method) {
		this.nestedClassSelector = new NestedClassSelector(enclosingClasses, nestedClass);
		this.methodSelector = new MethodSelector(nestedClass, method);
	}

	/**
	 * Get the {@link ClassLoader} used to load the nested class.
	 *
	 * @since 1.10
	 */
	@API(status = MAINTAINED, since = "1.13.3")
	public @Nullable ClassLoader getClassLoader() {
		return this.nestedClassSelector.getClassLoader();
	}

	/**
	 * Get the names of the classes enclosing the nested class
	 * containing the selected method.
	 */
	public List<String> getEnclosingClassNames() {
		return this.nestedClassSelector.getEnclosingClassNames();
	}

	/**
	 * Get the list of {@link Class} enclosing the nested {@link Class}
	 * containing the selected {@link Method}.
	 *
	 * <p>If the {@link Class} were not provided, but only the name of the
	 * nested class and its enclosing classes, this method attempts to lazily
	 * load the list of enclosing {@link Class} and throws a
	 * {@link PreconditionViolationException} if the classes cannot be loaded.
	 */
	public List<Class<?>> getEnclosingClasses() {
		return this.nestedClassSelector.getEnclosingClasses();
	}

	/**
	 * Get the name of the nested class containing the selected method.
	 */
	public String getNestedClassName() {
		return this.nestedClassSelector.getNestedClassName();
	}

	/**
	 * Get the nested {@link Class} containing the selected {@link Method}.
	 *
	 * <p>If the {@link Class} were not provided, but only the name of the
	 * nested class and its enclosing classes, this method attempts to lazily
	 * load the nested {@link Class} and throws a
	 * {@link PreconditionViolationException} if the class cannot be loaded.
	 */
	public Class<?> getNestedClass() {
		return this.nestedClassSelector.getNestedClass();
	}

	/**
	 * Get the name of the selected method.
	 */
	public String getMethodName() {
		return this.methodSelector.getMethodName();
	}

	/**
	 * Get the selected {@link Method}.
	 *
	 * <p>If the {@link Method} was not provided, but only the name, this method
	 * attempts to lazily load the {@code Method} based on its name and throws a
	 * {@link PreconditionViolationException} if the method cannot be loaded.
	 */
	public Method getMethod() {
		return this.methodSelector.getJavaMethod();
	}

	/**
	 * Get the names of parameter types for the selected method as a {@link String}.
	 *
	 * <p>See {@link MethodSelector#getParameterTypeNames()} for details.
	 *
	 * @return the names of parameter types supplied to this {@code NestedMethodSelector}
	 * via a constructor or deduced from a {@code Method} or parameter types supplied
	 * via a constructor; never {@code null} but potentially an empty string
	 * @since 1.10
	 * @see MethodSelector#getParameterTypeNames()
	 *
	 */
	@API(status = STABLE, since = "1.10")
	public String getParameterTypeNames() {
		return this.methodSelector.getParameterTypeNames();
	}

	/**
	 * Get the parameter types for the selected method.
	 *
	 * <p>See {@link MethodSelector#getParameterTypes()} for details.
	 *
	 * @return the method's parameter types; never {@code null} but potentially
	 * an empty array if the selected method does not declare parameters
	 * @since 1.10
	 * @see #getParameterTypeNames()
	 * @see MethodSelector#getParameterTypes()
	 */
	@API(status = MAINTAINED, since = "1.13.3")
	public Class<?>[] getParameterTypes() {
		return this.methodSelector.getParameterTypes();
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) {
			return true;
		}
		if (o == null || getClass() != o.getClass()) {
			return false;
		}
		NestedMethodSelector that = (NestedMethodSelector) o;
		return this.nestedClassSelector.equals(that.nestedClassSelector)
				&& this.methodSelector.equals(that.methodSelector);
	}

	@Override
	public int hashCode() {
		return Objects.hash(this.nestedClassSelector, this.methodSelector);
	}

	@Override
	public String toString() {
		return new ToStringBuilder(this) //
				.append("enclosingClassNames", getEnclosingClassNames()) //
				.append("nestedClassName", getNestedClassName()) //
				.append("methodName", getMethodName()) //
				.append("parameterTypes", getParameterTypeNames()) //
				.append("classLoader", getClassLoader()) //
				.toString();
	}

	@Override
	public Optional<DiscoverySelectorIdentifier> toIdentifier() {
		return nestedClassSelector.toIdentifier() //
				.map(parent -> {
					String fullyQualifiedMethodName = ReflectionUtils.getFullyQualifiedMethodName(parent.getValue(),
						methodSelector.getMethodName(), methodSelector.getParameterTypeNames());
					return DiscoverySelectorIdentifier.create(IdentifierParser.PREFIX, fullyQualifiedMethodName);
				});
	}

	/**
	 * The {@link DiscoverySelectorIdentifierParser} for
	 * {@link NestedMethodSelector NestedMethodSelectors}.
	 */
	@API(status = INTERNAL, since = "1.11")
	public static class IdentifierParser implements DiscoverySelectorIdentifierParser {

		private static final String PREFIX = "nested-method";

		public IdentifierParser() {
		}

		@Override
		public String getPrefix() {
			return PREFIX;
		}

		@Override
		public Optional<NestedMethodSelector> parse(DiscoverySelectorIdentifier identifier, Context context) {
			List<String> parts = Arrays.asList(identifier.getValue().split("/"));
			List<String> enclosingClassNames = parts.subList(0, parts.size() - 1);

			String[] methodParts = ReflectionUtils.parseFullyQualifiedMethodName(parts.get(parts.size() - 1));
			String nestedClassName = methodParts[0];
			String methodName = methodParts[1];
			String parameterTypeNames = methodParts[2];

			return Optional.of(DiscoverySelectors.selectNestedMethod(enclosingClassNames, nestedClassName, methodName,
				parameterTypeNames));
		}

	}

}
