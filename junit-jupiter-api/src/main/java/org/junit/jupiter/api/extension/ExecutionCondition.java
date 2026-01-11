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
 * {@code ExecutionCondition} defines the {@link Extension} API for
 * programmatic, <em>conditional test execution</em>.
 *
 * <p>An {@code ExecutionCondition} is
 * {@linkplain #evaluateExecutionCondition(ExtensionContext) evaluated}
 * to determine if a given container or test should be executed based on the
 * supplied {@link ExtensionContext}.
 *
 * <p>If an {@code ExecutionCondition} {@linkplain ConditionEvaluationResult#disabled
 * disables} a test method, that prevents execution of the test method and
 * method-level lifecycle callbacks such as {@code @BeforeEach} methods,
 * {@code @AfterEach} methods, and corresponding extension APIs. However, that
 * does not prevent the test class from being instantiated, and it does not prevent
 * the execution of class-level lifecycle callbacks such as {@code @BeforeAll}
 * methods, {@code @AfterAll} methods, and corresponding extension APIs.
 *
 * <h2>Constructor Requirements</h2>
 *
 * <p>Consult the documentation in {@link Extension} for details on
 * constructor requirements.
 *
 * @since 5.0
 * @see org.junit.jupiter.api.Disabled
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
 * @see org.junit.jupiter.api.condition.DisabledIfEnvironmentVariable
 */
@FunctionalInterface
@API(status = STABLE, since = "5.0")
public interface ExecutionCondition extends Extension {

	/**
	 * Evaluate this condition for the supplied {@link ExtensionContext}.
	 *
	 * <p>An {@linkplain ConditionEvaluationResult#enabled enabled} result
	 * indicates that the container or test should be executed; whereas, a
	 * {@linkplain ConditionEvaluationResult#disabled disabled} result
	 * indicates that the container or test should not be executed.
	 *
	 * @param context the current extension context; never {@code null}
	 * @return the result of evaluating this condition; never {@code null}
	 */
	ConditionEvaluationResult evaluateExecutionCondition(ExtensionContext context);

}
