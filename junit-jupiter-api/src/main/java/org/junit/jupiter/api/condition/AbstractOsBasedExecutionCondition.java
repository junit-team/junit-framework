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

import static org.junit.jupiter.api.extension.ConditionEvaluationResult.enabled;
import static org.junit.platform.commons.support.AnnotationSupport.findAnnotation;

import java.lang.annotation.Annotation;
import java.util.Locale;

import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.extension.ConditionEvaluationResult;
import org.junit.jupiter.api.extension.ExecutionCondition;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.platform.commons.util.StringUtils;

/**
 * Base class for OS-based {@link ExecutionCondition} implementations.
 *
 * @since 5.9
 */
abstract class AbstractOsBasedExecutionCondition<A extends Annotation> implements ExecutionCondition {

	static final String CURRENT_ARCHITECTURE = System.getProperty("os.arch");
	static final String CURRENT_OS = System.getProperty("os.name");

	private final Class<A> annotationType;

	AbstractOsBasedExecutionCondition(Class<A> annotationType) {
		this.annotationType = annotationType;
	}

	@Override
	public ConditionEvaluationResult evaluateExecutionCondition(ExtensionContext context) {
		return findAnnotation(context.getElement(), this.annotationType) //
				.map(this::evaluateExecutionCondition) //
				.orElseGet(this::enabledByDefault);
	}

	abstract ConditionEvaluationResult evaluateExecutionCondition(A annotation);

	/**
	 * Determine if the name of the current operating system contains the
	 * supplied name, ignoring case.
	 *
	 * @see #osNameMatches(String, String)
	 */
	static boolean isCurrentOsName(String name) {
		return osNameMatches(CURRENT_OS, name);
	}

	/**
	 * Determine if the supplied operating system name contains the supplied
	 * name, ignoring case.
	 *
	 * <p>Both names are converted to lower case using {@link Locale#ENGLISH},
	 * so that the result does not depend on the default locale. The supplied
	 * name is matched literally. If either name is blank, this method returns
	 * {@code false}.
	 */
	static boolean osNameMatches(@Nullable String osName, String name) {
		if (StringUtils.isBlank(osName) || StringUtils.isBlank(name)) {
			return false;
		}
		return osName.toLowerCase(Locale.ENGLISH).contains(name.toLowerCase(Locale.ENGLISH));
	}

	String createReason(boolean enabled, boolean osSpecified, boolean archSpecified) {
		StringBuilder reason = new StringBuilder() //
				.append(enabled ? "Enabled" : "Disabled") //
				.append(osSpecified ? " on operating system: " : " on architecture: ");

		if (osSpecified && archSpecified) {
			reason.append("%s (%s)".formatted(CURRENT_OS, CURRENT_ARCHITECTURE));
		}
		else if (osSpecified) {
			reason.append(CURRENT_OS);
		}
		else {
			reason.append(CURRENT_ARCHITECTURE);
		}

		return reason.toString();
	}

	private ConditionEvaluationResult enabledByDefault() {
		String reason = "@%s is not present".formatted(this.annotationType.getSimpleName());
		return enabled(reason);
	}

}
