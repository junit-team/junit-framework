/*
 * Copyright 2015-2026 the original author or authors.
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v2.0 which
 * accompanies this distribution and is available at
 *
 * https://www.eclipse.org/legal/epl-v20.html
 */

package org.junit.jupiter.api.extension;

import static org.apiguardian.api.API.Status.STABLE;

import org.apiguardian.api.API;

/**
 * {@code BeforeAllCallback} defines the API for {@link Extension Extensions}
 * that wish to provide additional behavior to test containers <strong>once</strong>
 * before all tests in the container have been executed.
 *
 * <p>Concrete implementations often implement {@link AfterAllCallback} as well.
 *
 * <p>Extensions that implement {@code BeforeAllCallback} must be registered at
 * the class level.
 *
 * <h2>Constructor Requirements</h2>
 *
 * <p>Consult the documentation in {@link Extension} for details on
 * constructor requirements.
 *
 * <h2>Wrapping Behavior</h2>
 *
 * <p>JUnit Jupiter guarantees <em>wrapping behavior</em> for multiple
 * registered extensions that implement lifecycle callbacks such as
 * {@link BeforeAllCallback}, {@link AfterAllCallback},
 * {@link BeforeClassTemplateInvocationCallback},
 * {@link AfterClassTemplateInvocationCallback}, {@link BeforeEachCallback},
 * {@link AfterEachCallback}, {@link BeforeTestExecutionCallback}, and
 * {@link AfterTestExecutionCallback}.
 *
 * <p>That means that, given two extensions {@code Extension1} and
 * {@code Extension2} with {@code Extension1} registered before
 * {@code Extension2}, any "before" callbacks implemented by {@code Extension1}
 * are guaranteed to execute before any "before" callbacks implemented by
 * {@code Extension2}. Similarly, given the two same two extensions registered
 * in the same order, any "after" callbacks implemented by {@code Extension1}
 * are guaranteed to execute after any "after" callbacks implemented by
 * {@code Extension2}. {@code Extension1} is therefore said to <em>wrap</em>
 * {@code Extension2}.
 *
 * @since 5.0
 * @see org.junit.jupiter.api.BeforeAll
 * @see AfterAllCallback
 * @see BeforeEachCallback
 * @see AfterEachCallback
 * @see BeforeTestExecutionCallback
 * @see AfterTestExecutionCallback
 * @see BeforeClassTemplateInvocationCallback
 * @see AfterClassTemplateInvocationCallback
 */
@FunctionalInterface
@API(status = STABLE, since = "5.0")
public interface BeforeAllCallback extends Extension {

	/**
	 * Callback that is invoked once <em>before</em> all tests in the current
	 * container.
	 *
	 * @param context the current extension context; never {@code null}
	 */
	void beforeAll(ExtensionContext context) throws Exception;

}
