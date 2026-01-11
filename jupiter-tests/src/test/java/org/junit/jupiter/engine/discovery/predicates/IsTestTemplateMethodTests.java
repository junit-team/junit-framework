/*
 * Copyright 2015-2026 the original author or authors.
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v2.0 which
 * accompanies this distribution and is available at
 *
 * https://www.eclipse.org/legal/epl-v20.html
 */

package org.junit.jupiter.engine.discovery.predicates;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.platform.commons.util.CollectionUtils.getOnlyElement;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestTemplate;
import org.junit.platform.commons.support.ReflectionSupport;
import org.junit.platform.engine.DiscoveryIssue;
import org.junit.platform.engine.support.descriptor.MethodSource;
import org.junit.platform.engine.support.discovery.DiscoveryIssueReporter;

/**
 * Unit tests for {@link IsTestTemplateMethod}.
 *
 * @since 5.0
 */
class IsTestTemplateMethodTests {

	final List<DiscoveryIssue> discoveryIssues = new ArrayList<>();
	final IsTestTemplateMethod isTestTemplateMethod = new IsTestTemplateMethod(
		DiscoveryIssueReporter.collecting(discoveryIssues));

	@Test
	void testTemplateMethodReturningVoid() {
		assertThat(isTestTemplateMethod).accepts(method("templateReturningVoid"));
	}

	@Test
	void bogusTestTemplateMethodReturningObject() {
		var method = method("bogusTemplateReturningObject");

		assertThat(isTestTemplateMethod).rejects(method);

		var issue = getOnlyElement(discoveryIssues);
		assertThat(issue.severity()).isEqualTo(DiscoveryIssue.Severity.WARNING);
		assertThat(issue.message()).isEqualTo(
			"@TestTemplate method '%s' must not return a value. It will not be executed.", method.toGenericString());
		assertThat(issue.source()).contains(MethodSource.from(method));
	}

	private static Method method(String name) {
		return ReflectionSupport.findMethod(ClassWithTestTemplateMethods.class, name).orElseThrow();
	}

	@SuppressWarnings("unused")
	private static class ClassWithTestTemplateMethods {

		@TestTemplate
		void templateReturningVoid() {
		}

		@TestTemplate
		String bogusTemplateReturningObject() {
			return "";
		}

	}

}
