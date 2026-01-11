/*
 * Copyright 2015-2026 the original author or authors.
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v2.0 which
 * accompanies this distribution and is available at
 *
 * https://www.eclipse.org/legal/epl-v20.html
 */

package org.junit.platform.suite.api;

import static org.apiguardian.api.API.Status.MAINTAINED;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Repeatable;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.apiguardian.api.API;

/**
 * {@code @SelectMethod} is a {@linkplain Repeatable repeatable} annotation that
 * specifies a method to <em>select</em> when running a test suite on the JUnit
 * Platform.
 *
 * @since 1.10
 * @see Suite
 * @see org.junit.platform.engine.discovery.DiscoverySelectors#selectMethod(String)
 * @see org.junit.platform.engine.discovery.DiscoverySelectors#selectMethod(String, String, String)
 * @see org.junit.platform.engine.discovery.DiscoverySelectors#selectMethod(String, String, Class...)
 * @see org.junit.platform.engine.discovery.DiscoverySelectors#selectMethod(Class, String, String)
 * @see org.junit.platform.engine.discovery.DiscoverySelectors#selectMethod(Class, String, Class...)
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
@Inherited
@Documented
@API(status = MAINTAINED, since = "1.13.3")
@Repeatable(SelectMethods.class)
public @interface SelectMethod {

	/**
	 * The <em>fully qualified method name</em> of the method to select.
	 *
	 * <p>The following formats are supported.
	 *
	 * <ul>
	 * <li>{@code [fully qualified class name]#[methodName]}</li>
	 * <li>{@code [fully qualified class name]#[methodName](parameter type list)}
	 * </ul>
	 *
	 * <p>The <em>parameter type list</em> is a comma-separated list of primitive
	 * names or fully qualified class names for the types of parameters accepted
	 * by the method.
	 *
	 * <p>Array parameter types may be specified using either the JVM's internal
	 * String representation (e.g., {@code [[I} for {@code int[][]},
	 * {@code [Ljava.lang.String;} for {@code java.lang.String[]}, etc.) or
	 * <em>source code syntax</em> (e.g., {@code int[][]}, {@code java.lang.String[]},
	 * etc.).
	 *
	 * <table class="plain">
	 * <caption>Examples</caption>
	 * <tr><th>Method</th><th>Fully Qualified Method Name</th></tr>
	 * <tr><td>{@code java.lang.String.chars()}</td><td>{@code java.lang.String#chars}</td></tr>
	 * <tr><td>{@code java.lang.String.chars()}</td><td>{@code java.lang.String#chars()}</td></tr>
	 * <tr><td>{@code java.lang.String.equalsIgnoreCase(String)}</td><td>{@code java.lang.String#equalsIgnoreCase(java.lang.String)}</td></tr>
	 * <tr><td>{@code java.lang.String.substring(int, int)}</td><td>{@code java.lang.String#substring(int, int)}</td></tr>
	 * <tr><td>{@code example.Calc.avg(int[])}</td><td>{@code example.Calc#avg([I)}</td></tr>
	 * <tr><td>{@code example.Calc.avg(int[])}</td><td>{@code example.Calc#avg(int[])}</td></tr>
	 * <tr><td>{@code example.Matrix.multiply(double[][])}</td><td>{@code example.Matrix#multiply([[D)}</td></tr>
	 * <tr><td>{@code example.Matrix.multiply(double[][])}</td><td>{@code example.Matrix#multiply(double[][])}</td></tr>
	 * <tr><td>{@code example.Service.process(String[])}</td><td>{@code example.Service#process([Ljava.lang.String;)}</td></tr>
	 * <tr><td>{@code example.Service.process(String[])}</td><td>{@code example.Service#process(java.lang.String[])}</td></tr>
	 * <tr><td>{@code example.Service.process(String[][])}</td><td>{@code example.Service#process([[Ljava.lang.String;)}</td></tr>
	 * <tr><td>{@code example.Service.process(String[][])}</td><td>{@code example.Service#process(java.lang.String[][])}</td></tr>
	 * </table>
	 *
	 * <p>Cannot be combined with any other attribute.
	 *
	 * @see org.junit.platform.engine.discovery.DiscoverySelectors#selectMethod(String)
	 */
	String value() default "";

	/**
	 * The class in which the method is declared, or a subclass thereof.
	 *
	 * <p>Cannot be used in conjunction with {@link #typeName()}.
	 */
	Class<?> type() default Class.class;

	/**
	 * The <em>fully qualified class name</em> in which the method is declared, or a subclass thereof.
	 *
	 * <p>Cannot be used in conjunction with {@link #type()}.
	 */
	String typeName() default "";

	/**
	 * The name of the method to select; never blank unless {@link #value()} is used.
	 */
	String name() default "";

	/**
	 * The parameter types of the method to select.
	 *
	 * <p>Cannot be used in conjunction with {@link #parameterTypeNames()}.
	 */
	Class<?>[] parameterTypes() default {};

	/**
	 * The parameter types of the method to select.
	 *
	 * <p>This is typically a comma-separated list of atomic types, fully
	 * qualified class names, or array types; however, the exact syntax
	 * depends on the underlying test engine.
	 *
	 * <p>If the method takes no parameters, this attribute must be an
	 * empty string.
	 *
	 * <p>Array parameter types may be specified using either the JVM's internal
	 * String representation (e.g., {@code [[I} for {@code int[][]},
	 * {@code [Ljava.lang.String;} for {@code java.lang.String[]}, etc.) or
	 * <em>source code syntax</em> (e.g., {@code int[][]}, {@code java.lang.String[]},
	 * etc.).
	 *
	 * <table class="plain">
	 * <caption>Examples</caption>
	 * <tr><th>Method</th><th>Parameter types list</th></tr>
	 * <tr><td>{@code java.lang.String.chars()}</td><td>The empty string</td></tr>
	 * <tr><td>{@code java.lang.String.equalsIgnoreCase(String)}</td><td>{@code java.lang.String}</td></tr>
	 * <tr><td>{@code java.lang.String.substring(int, int)}</td><td>{@code int, int}</td></tr>
	 * <tr><td>{@code example.Calc.avg(int[])}</td><td>{@code [I}</td></tr>
	 * <tr><td>{@code example.Calc.avg(int[])}</td><td>{@code int[]}</td></tr>
	 * <tr><td>{@code example.Matrix.multiply(double[][])}</td><td>{@code [[D}</td></tr>
	 * <tr><td>{@code example.Matrix.multiply(double[][])}</td><td>{@code double[][]}</td></tr>
	 * <tr><td>{@code example.Service.process(String[])}</td><td>{@code [Ljava.lang.String;}</td></tr>
	 * <tr><td>{@code example.Service.process(String[])}</td><td>{@code java.lang.String[]}</td></tr>
	 * <tr><td>{@code example.Service.process(String[][])}</td><td>{@code [[Ljava.lang.String;}</td></tr>
	 * <tr><td>{@code example.Service.process(String[][])}</td><td>{@code java.lang.String[][]}</td></tr>
	 * </table>
	 *
	 * <p>Cannot be used in conjunction with {@link #parameterTypes()}.
	 */
	String parameterTypeNames() default "";

}
