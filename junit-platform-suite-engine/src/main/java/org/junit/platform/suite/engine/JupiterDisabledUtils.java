/*
 * Copyright 2026 the original author or authors.
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v2.0 which
 * accompanies this distribution and is available at
 *
 * https://www.eclipse.org/legal/epl-v20.html
 */

package org.junit.platform.suite.engine;

import static org.junit.platform.commons.support.AnnotationSupport.findAnnotation;
import static org.junit.platform.engine.DiscoveryIssue.Severity.INFO;

import java.lang.annotation.Annotation;
import java.util.Optional;

import org.jspecify.annotations.Nullable;
import org.junit.platform.commons.function.Try;
import org.junit.platform.engine.DiscoveryIssue;
import org.junit.platform.engine.support.discovery.DiscoveryIssueReporter;
import org.junit.platform.suite.api.Disabled;

final class JupiterDisabledUtils {
	private static final String ORG_JUNIT_JUPITER_API_DISABLED = "org.junit.jupiter.api.Disabled";
	private static @Nullable Try<Class<? extends Annotation>> jupiterDisabled;

	static void reportUseOfJupiterDisabled(Class<?> suiteClass, DiscoveryIssueReporter issueReporter) {
		getJupiterDisabledAnnotation() //
				.flatMap(annotationClass -> findAnnotation(suiteClass, annotationClass)) //
				.map(annotation -> DiscoveryIssue.create(INFO,
					"The suite [%s] was annotated with [%s] which does not disabled the suite. Did you mean to use [%s]?" //
							.formatted(suiteClass, annotation.annotationType().getName(), Disabled.class.getName()))) //
				.ifPresent(issueReporter::reportIssue);
	}

	@SuppressWarnings("unchecked")
	private static Optional<Class<? extends Annotation>> getJupiterDisabledAnnotation() {
		if (jupiterDisabled == null) {
			jupiterDisabled = Try.call(
				() -> (Class<? extends Annotation>) Class.forName(ORG_JUNIT_JUPITER_API_DISABLED));
		}
		return jupiterDisabled.toOptional();

	}
}
