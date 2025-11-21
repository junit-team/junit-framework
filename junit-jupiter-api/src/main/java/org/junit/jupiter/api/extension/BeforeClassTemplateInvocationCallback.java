/*
 * Copyright 2015-2025 the original author or authors.
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v2.0 which
 * accompanies this distribution and is available at
 *
 * https://www.eclipse.org/legal/epl-v20.html
 */

package org.junit.jupiter.api.extension;

import static org.apiguardian.api.API.Status.EXPERIMENTAL;

import org.apiguardian.api.API;
import org.junit.jupiter.api.ClassTemplate;

/**
 * {@code BeforeClassTemplateInvocationCallback} defines the API for
 * {@link Extension Extensions} that wish to provide additional behavior
 * <strong>once</strong> before each invocation of a
 * {@link ClassTemplate @ClassTemplate}.
 *
 * <p>Concrete implementations often implement
 * {@link AfterClassTemplateInvocationCallback} as well.
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
 * @since 5.13
 * @see ClassTemplate
 * @see AfterClassTemplateInvocationCallback
 * @see BeforeAllCallback
 * @see AfterAllCallback
 * @see BeforeEachCallback
 * @see AfterEachCallback
 * @see BeforeTestExecutionCallback
 * @see AfterTestExecutionCallback
 */
@FunctionalInterface
@API(status = EXPERIMENTAL, since = "6.0")
public interface BeforeClassTemplateInvocationCallback extends Extension {

	/**
	 * Callback that is invoked <em>before</em> each invocation of a class
	 * template.
	 *
	 * @param context the current extension context; never {@code null}
	 */
	void beforeClassTemplateInvocation(ExtensionContext context) throws Exception;

}
