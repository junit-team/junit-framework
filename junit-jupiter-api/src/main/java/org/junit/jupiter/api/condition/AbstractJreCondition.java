/*
 * Copyright 2015-2026 the original author or authors.
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v2.0 which
 * accompanies this distribution and is available at
 *
 * https://www.eclipse.org/legal/epl-v20.html
 */

package org.junit.jupiter.api.condition;

import static java.util.function.Predicate.isEqual;

import java.lang.annotation.Annotation;
import java.util.Arrays;
import java.util.function.Function;
import java.util.stream.IntStream;

import org.junit.platform.commons.util.Preconditions;

/**
 * Abstract base class for {@link EnabledOnJreCondition} and
 * {@link DisabledOnJreCondition}.
 *
 * @since 5.12
 */
abstract class AbstractJreCondition<A extends Annotation> extends BooleanExecutionCondition<A> {

	static final String ENABLED_ON_CURRENT_JRE = //
		"Enabled on JRE version: " + System.getProperty("java.version");

	static final String DISABLED_ON_CURRENT_JRE = //
		"Disabled on JRE version: " + System.getProperty("java.version");

	AbstractJreCondition(Class<A> annotationType, Function<A, String> customDisabledReason) {
		super(annotationType, ENABLED_ON_CURRENT_JRE, DISABLED_ON_CURRENT_JRE, customDisabledReason);
	}

	protected final IntStream validatedVersions(JRE[] jres, int[] versions) {
		String annotationName = super.annotationType.getSimpleName();

		Preconditions.condition(jres.length > 0 || versions.length > 0,
			() -> "You must declare at least one JRE or version in @" + annotationName);

		Preconditions.condition(Arrays.stream(jres).noneMatch(isEqual(JRE.UNDEFINED)),
			() -> "JRE.UNDEFINED is not supported in @" + annotationName);
		Arrays.stream(versions).min().ifPresent(version -> Preconditions.condition(version >= JRE.MINIMUM_VERSION,
			() -> "Version [%d] in @%s must be greater than or equal to %d".formatted(version, annotationName,
				JRE.MINIMUM_VERSION)));

		return IntStream.concat(//
			Arrays.stream(jres).mapToInt(JRE::version), //
			Arrays.stream(versions) //
		).distinct();
	}

}
