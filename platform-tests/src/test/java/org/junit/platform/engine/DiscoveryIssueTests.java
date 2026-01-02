/*
 * Copyright 2015-2025 the original author or authors.
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v2.0 which
 * accompanies this distribution and is available at
 *
 * https://www.eclipse.org/legal/epl-v20.html
 */

package org.junit.platform.engine;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.EqualsAndHashCodeAssertions.assertEqualsAndHashCode;
import static org.mockito.Mockito.mock;

import java.util.Optional;
import java.util.function.UnaryOperator;

import org.junit.jupiter.api.Test;
import org.junit.platform.engine.DiscoveryIssue.Severity;
import org.junit.platform.engine.support.descriptor.ClassSource;

class DiscoveryIssueTests {

	@Test
	void create() {
		var issue = DiscoveryIssue.create(Severity.ERROR, "message");

		assertThat(issue.severity()).isEqualTo(Severity.ERROR);
		assertThat(issue.message()).isEqualTo("message");
		assertThat(issue.source()).isEmpty();
		assertThat(issue.cause()).isEmpty();
	}

	@Test
	void builder() {
		var source = mock(TestSource.class);
		var cause = new RuntimeException("boom");

		var issue = DiscoveryIssue.builder(Severity.WARNING, "message") //
				.source(source) //
				.cause(cause) //
				.build();

		assertThat(issue.severity()).isEqualTo(Severity.WARNING);
		assertThat(issue.message()).isEqualTo("message");
		assertThat(issue.source()).containsSame(source);
		assertThat(issue.cause()).containsSame(cause);
	}

	@Test
	void equalsAndHashCode() {
		assertEqualsAndHashCode( //
			DiscoveryIssue.create(Severity.ERROR, "message"), //
			DiscoveryIssue.builder(Severity.ERROR, "message").build(), //
			DiscoveryIssue.create(Severity.WARNING, "message") //
		);
		assertEqualsAndHashCode( //
			DiscoveryIssue.create(Severity.ERROR, "message"), //
			DiscoveryIssue.builder(Severity.ERROR, "message").build(), //
			DiscoveryIssue.create(Severity.ERROR, "anotherMessage") //
		);
		assertEqualsAndHashCode( //
			DiscoveryIssue.builder(Severity.ERROR, "message") //
					.source(ClassSource.from(DiscoveryIssue.class)).build(), //
			DiscoveryIssue.builder(Severity.ERROR, "message") //
					.source(Optional.of(ClassSource.from(DiscoveryIssue.class))).build(), //
			DiscoveryIssue.builder(Severity.ERROR, "message") //
					.source(ClassSource.from(DefaultDiscoveryIssue.class)).build() //
		);
		var cause = new RuntimeException("boom");
		assertEqualsAndHashCode( //
			DiscoveryIssue.builder(Severity.ERROR, "message").cause(cause).build(), //
			DiscoveryIssue.builder(Severity.ERROR, "message").cause(Optional.of(cause)).build(), //
			DiscoveryIssue.builder(Severity.ERROR, "message").cause(new RuntimeException("boom")).build() //
		);
	}

	@Test
	void stringRepresentationWithoutAttributes() {
		var issue = DiscoveryIssue.create(Severity.WARNING, "message");

		assertThat(issue.toString()) //
				.isEqualTo("DiscoveryIssue [severity = WARNING, message = 'message']");
	}

	@Test
	void stringRepresentationWithOptionalAttributes() {
		var issue = DiscoveryIssue.builder(Severity.WARNING, "message") //
				.source(ClassSource.from(DiscoveryIssue.class)) //
				.cause(new RuntimeException("boom")) //
				.build();

		assertThat(issue.toString()) //
				.isEqualTo(
					"DiscoveryIssue [severity = WARNING, message = 'message', source = ClassSource [className = 'org.junit.platform.engine.DiscoveryIssue', filePosition = null], cause = java.lang.RuntimeException: boom]");
	}

	@Test
	void withNewMessage() {
		var issue = DiscoveryIssue.builder(Severity.WARNING, "message") //
				.source(ClassSource.from(DiscoveryIssue.class)) //
				.cause(new RuntimeException("boom")) //
				.build();

		var newIssue = issue.withMessage(__ -> "new message");

		assertThat(newIssue.severity()).isEqualTo(Severity.WARNING);
		assertThat(newIssue.message()).isEqualTo("new message");
		assertThat(newIssue.source()).containsSame(issue.source().orElseThrow());
		assertThat(newIssue.cause()).containsSame(issue.cause().orElseThrow());
	}

	@Test
	void withSameMessage() {
		var issue = DiscoveryIssue.builder(Severity.WARNING, "message") //
				.source(ClassSource.from(DiscoveryIssue.class)) //
				.cause(new RuntimeException("boom")) //
				.build();

		var newIssue = issue.withMessage(UnaryOperator.identity());

		assertThat(newIssue).isSameAs(issue);
	}

}
