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

import static org.junit.jupiter.api.condition.AbstractJreCondition.DISABLED_ON_CURRENT_JRE;
import static org.junit.jupiter.api.condition.AbstractJreCondition.ENABLED_ON_CURRENT_JRE;

import java.lang.annotation.Annotation;
import java.util.function.Function;

import org.junit.platform.commons.util.Preconditions;

/**
 * Abstract base class for {@link EnabledForJreRangeCondition} and
 * {@link DisabledForJreRangeCondition}.
 *
 * @since 5.12
 */
abstract class AbstractJreRangeCondition<A extends Annotation> extends BooleanExecutionCondition<A> {

	private static final JRE DEFAULT_MINIMUM_JRE = JRE.JAVA_17;
	private static final JRE DEFAULT_MAXIMUM_JRE = JRE.OTHER;

	AbstractJreRangeCondition(Class<A> annotationType, Function<A, String> customDisabledReason) {
		super(annotationType, ENABLED_ON_CURRENT_JRE, DISABLED_ON_CURRENT_JRE, customDisabledReason);
	}

	protected final boolean isCurrentVersionWithinRange(JRE minJre, JRE maxJre, int minVersion, int maxVersion) {
		String annotationName = super.annotationType.getSimpleName();

		boolean minJreSet = minJre != JRE.UNDEFINED;
		boolean maxJreSet = maxJre != JRE.UNDEFINED;
		boolean minVersionSet = minVersion != JRE.UNDEFINED_VERSION;
		boolean maxVersionSet = maxVersion != JRE.UNDEFINED_VERSION;

		// Users must choose between JRE enum constants and version numbers.
		Preconditions.condition(!minJreSet || !minVersionSet,
			() -> "@%s's minimum value must be configured with either a JRE enum constant or numeric version, but not both".formatted(
				annotationName));
		Preconditions.condition(!maxJreSet || !maxVersionSet,
			() -> "@%s's maximum value must be configured with either a JRE enum constant or numeric version, but not both".formatted(
				annotationName));

		// Users must supply valid values for minVersion and maxVersion.
		Preconditions.condition(!minVersionSet || (minVersion >= JRE.MINIMUM_VERSION),
			() -> "@%s's minVersion [%d] must be greater than or equal to %d".formatted(annotationName, minVersion,
				JRE.MINIMUM_VERSION));
		Preconditions.condition(!maxVersionSet || (maxVersion >= JRE.MINIMUM_VERSION),
			() -> "@%s's maxVersion [%d] must be greater than or equal to %d".formatted(annotationName, maxVersion,
				JRE.MINIMUM_VERSION));

		// Now that we have checked the basic preconditions, we need to ensure that we are
		// using valid JRE enum constants.
		if (!minJreSet) {
			minJre = DEFAULT_MINIMUM_JRE;
		}
		if (!maxJreSet) {
			maxJre = DEFAULT_MAXIMUM_JRE;
		}

		int min = (minVersionSet ? minVersion : minJre.version());
		int max = (maxVersionSet ? maxVersion : maxJre.version());

		// Finally, we need to validate the effective minimum and maximum values.
		Preconditions.condition((min != DEFAULT_MINIMUM_JRE.version() || max != DEFAULT_MAXIMUM_JRE.version()),
			() -> "You must declare a non-default value for the minimum or maximum value in @" + annotationName);
		Preconditions.condition(min <= max,
			() -> "@%s's minimum value [%d] must be less than or equal to its maximum value [%d]".formatted(
				annotationName, min, max));

		return JRE.isCurrentVersionWithinRange(min, max);
	}

}
