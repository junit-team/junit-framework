/*
 * Copyright 2015-2025 the original author or authors.
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v2.0 which
 * accompanies this distribution and is available at
 *
 * https://www.eclipse.org/legal/epl-v20.html
 */

package org.junit.jupiter.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.DynamicContainer.dynamicContainer;
import static org.junit.jupiter.api.DynamicTest.dynamicTest;
import static org.junit.jupiter.api.parallel.ExecutionMode.CONCURRENT;
import static org.junit.jupiter.api.parallel.ExecutionMode.SAME_THREAD;
import static org.junit.platform.commons.test.PreconditionAssertions.assertPreconditionViolationFor;
import static org.junit.platform.commons.test.PreconditionAssertions.assertPreconditionViolationNotNullFor;
import static org.junit.platform.commons.test.PreconditionAssertions.assertPreconditionViolationNotNullOrBlankFor;

import java.net.URI;
import java.util.Collection;
import java.util.List;
import java.util.stream.Stream;

/**
 * @since 6.1
 */
class DynamicContainerTests {

	@Test
	void appliesConfiguration() {
		var child = dynamicTest("Test", Assertions::fail);

		var container = dynamicContainer(config -> config //
				.displayName("Container") //
				.testSourceUri(URI.create("https://junit.org")) //
				.executionMode(CONCURRENT) //
				.childExecutionMode(SAME_THREAD) //
				.children(child));

		assertThat(container.getDisplayName()).isEqualTo("Container");
		assertThat(container.getTestSourceUri()).contains(URI.create("https://junit.org"));
		assertThat(container.getExecutionMode()).contains(CONCURRENT);
		assertThat(container.getChildExecutionMode()).contains(SAME_THREAD);
		assertThat(container.getChildren().map(DynamicNode.class::cast)).containsExactly(child);
	}

	@Test
	void displayNameMustNotBeBlank() {
		assertPreconditionViolationNotNullOrBlankFor("displayName", () -> dynamicContainer(__ -> {
		}));
		assertPreconditionViolationNotNullOrBlankFor("displayName",
			() -> dynamicContainer(config -> config.displayName("")));
	}

	@SuppressWarnings("DataFlowIssue")
	@Test
	void executionModeMustNotBeNull() {
		assertPreconditionViolationNotNullFor("executionMode",
			() -> dynamicContainer(config -> config.executionMode(null)));
	}

	@SuppressWarnings("DataFlowIssue")
	@Test
	void childExecutionModeMustNotBeNull() {
		assertPreconditionViolationNotNullFor("executionMode",
			() -> dynamicContainer(config -> config.childExecutionMode(null)));
	}

	@SuppressWarnings("DataFlowIssue")
	@Test
	void childrenMustBeConfigured() {
		assertPreconditionViolationNotNullFor("children",
			() -> dynamicContainer(config -> config.children((DynamicNode[]) null)));
		assertPreconditionViolationNotNullFor("children",
			() -> dynamicContainer(config -> config.children((Stream<? extends DynamicNode>) null)));
		assertPreconditionViolationNotNullFor("children",
			() -> dynamicContainer(config -> config.children((Collection<? extends DynamicNode>) null)));
		assertPreconditionViolationNotNullFor("children",
			() -> dynamicContainer(config -> config.displayName("container")));
		assertPreconditionViolationFor(() -> dynamicContainer(config -> config.children((DynamicNode) null))) //
				.withMessage("children must not contain null elements");
	}

	@Test
	void childrenMustNotBeConfiguredMoreThanOnce() {
		assertPreconditionViolationFor(() -> dynamicContainer(config -> config.children().children())) //
				.withMessage("children can only be set once");
		assertPreconditionViolationFor(() -> dynamicContainer(config -> config.children().children(Stream.empty()))) //
				.withMessage("children can only be set once");
		assertPreconditionViolationFor(() -> dynamicContainer(config -> config.children().children(List.of()))) //
				.withMessage("children can only be set once");
	}

}
