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
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.apiguardian.api.API;

/**
 * {@code @AfterSuite} is used to signal that the annotated method should be
 * executed <em>after</em> <strong>all</strong> tests in the current test suite.
 *
 * <h2>Method Signatures</h2>
 *
 * <p>{@code @AfterSuite} methods must have a {@code void} return type, must
 * be {@code static} and must not be {@code private}.
 *
 * <h2>Inheritance and Execution Order</h2>
 *
 * <p>{@code @AfterSuite} methods are inherited from superclasses as long as they
 * are not <em>overridden</em> according to the visibility rules of the Java
 * language. Furthermore, {@code @AfterSuite} methods from superclasses will be
 * executed after {@code @AfterSuite} methods in subclasses.
 *
 * <p>The JUnit Platform Suite Engine does not guarantee the execution order of
 * multiple {@code @AfterSuite} methods that are declared within a single test
 * class or test interface. While it may at times appear that these methods are
 * invoked in alphabetical order, they are in fact sorted using an algorithm
 * that is deterministic but intentionally non-obvious.
 *
 * <p>In addition, {@code @AfterSuite} methods are in no way linked to
 * {@code @BeforeSuite} methods. Consequently, there are no guarantees with regard
 * to their <em>wrapping</em> behavior. For example, given two
 * {@code @BeforeSuite} methods {@code createA()} and {@code createB()} as well as
 * two {@code @AfterSuite} methods {@code destroyA()} and {@code destroyB()}, the
 * order in which the {@code @BeforeSuite} methods are executed (e.g.
 * {@code createA()} before {@code createB()}) does not imply any order for the
 * seemingly corresponding {@code @AfterSuite} methods. In other words,
 * {@code destroyA()} might be called before <em>or</em> after
 * {@code destroyB()}. The JUnit Team therefore recommends that developers
 * declare at most one {@code @BeforeSuite} method and at most one
 * {@code @AfterSuite} method per test class or test interface unless there are no
 * dependencies between the {@code @BeforeSuite} methods or between the
 * {@code @AfterSuite} methods.
 *
 * <h2>Composition</h2>
 *
 * <p>{@code @AfterSuite} may be used as a meta-annotation in order to create
 * a custom <em>composed annotation</em> that inherits the semantics of
 * {@code @AfterSuite}.
 *
 * @since 1.11
 * @see BeforeSuite
 * @see Suite
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@API(status = MAINTAINED, since = "1.13.3")
public @interface AfterSuite {
}
