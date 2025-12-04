/*
 * Copyright 2015-2025 the original author or authors.
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v2.0 which
 * accompanies this distribution and is available at
 *
 * https://www.eclipse.org/legal/epl-v20.html
 */

package org.junit.jupiter.api.condition;

import static org.apiguardian.api.API.Status.STABLE;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Repeatable;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.apiguardian.api.API;
import org.junit.jupiter.api.extension.ExtendWith;

/**
 * {@code @DisabledIfEnvironmentVariable} is used to signal that the annotated test
 * class or test method is <em>disabled</em> if the value of the specified
 * {@linkplain #named environment variable} matches the specified
 * {@linkplain #matches regular expression}.
 *
 * <p>When declared at the class level, the result will apply to all test methods
 * within that class as well.
 *
 * <p>This annotation is not {@link java.lang.annotation.Inherited @Inherited}.
 * Consequently, if you wish to apply the same semantics to a subclass, this
 * annotation must be redeclared on the subclass.
 *
 * <p>If a test method is disabled via this annotation, that prevents execution
 * of the test method and method-level lifecycle callbacks such as
 * {@code @BeforeEach} methods, {@code @AfterEach} methods, and corresponding
 * extension APIs. However, that does not prevent the test class from being
 * instantiated, and it does not prevent the execution of class-level lifecycle
 * callbacks such as {@code @BeforeAll} methods, {@code @AfterAll} methods, and
 * corresponding extension APIs.
 *
 * <p>If the specified environment variable is undefined, the presence of this
 * annotation will have no effect on whether or not the class or method
 * is disabled.
 *
 * <p>This annotation may be used as a meta-annotation in order to create a
 * custom <em>composed annotation</em> that inherits the semantics of this
 * annotation.
 *
 * <p>This annotation is a {@linkplain Repeatable repeatable} annotation and may
 * be declared multiple times on an {@link java.lang.reflect.AnnotatedElement
 * AnnotatedElement} such as a test interface, test class, or test method.
 * Specifically, this annotation will be found if it is directly present,
 * indirectly present, or meta-present on a given element.
 *
 * @since 5.1
 * @see org.junit.jupiter.api.condition.EnabledIf
 * @see org.junit.jupiter.api.condition.DisabledIf
 * @see org.junit.jupiter.api.condition.EnabledOnOs
 * @see org.junit.jupiter.api.condition.DisabledOnOs
 * @see org.junit.jupiter.api.condition.EnabledOnJre
 * @see org.junit.jupiter.api.condition.DisabledOnJre
 * @see org.junit.jupiter.api.condition.EnabledForJreRange
 * @see org.junit.jupiter.api.condition.DisabledForJreRange
 * @see org.junit.jupiter.api.condition.EnabledInNativeImage
 * @see org.junit.jupiter.api.condition.DisabledInNativeImage
 * @see org.junit.jupiter.api.condition.EnabledIfSystemProperty
 * @see org.junit.jupiter.api.condition.DisabledIfSystemProperty
 * @see org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable
 * @see org.junit.jupiter.api.Disabled
 */
@Target({ ElementType.TYPE, ElementType.METHOD })
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Repeatable(DisabledIfEnvironmentVariables.class)
@ExtendWith(DisabledIfEnvironmentVariableCondition.class)
@API(status = STABLE, since = "5.1")
@SuppressWarnings("exports")
public @interface DisabledIfEnvironmentVariable {

	/**
	 * The name of the environment variable to retrieve.
	 *
	 * @return the environment variable name; never <em>blank</em>
	 * @see System#getenv(String)
	 */
	String named();

	/**
	 * A regular expression that will be used to match against the retrieved
	 * value of the {@link #named} environment variable.
	 *
	 * @return the regular expression; never <em>blank</em>
	 * @see String#matches(String)
	 * @see java.util.regex.Pattern
	 */
	String matches();

	/**
	 * Custom reason to provide if the test or container is disabled.
	 *
	 * <p>If a custom reason is supplied, it will be combined with the default
	 * reason for this annotation. If a custom reason is not supplied, the default
	 * reason will be used.
	 *
	 * @since 5.7
	 */
	@API(status = STABLE, since = "5.7")
	String disabledReason() default "";

}
