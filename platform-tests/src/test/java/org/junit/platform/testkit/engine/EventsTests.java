/*
 * Copyright 2015-2026 the original author or authors.
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v2.0 which
 * accompanies this distribution and is available at
 *
 * https://www.eclipse.org/legal/epl-v20.html
 */

package org.junit.platform.testkit.engine;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.platform.testkit.engine.EventConditions.engine;
import static org.junit.platform.testkit.engine.EventConditions.event;
import static org.junit.platform.testkit.engine.EventConditions.finishedSuccessfully;
import static org.junit.platform.testkit.engine.EventConditions.finishedWithFailure;
import static org.junit.platform.testkit.engine.EventConditions.skippedWithReason;
import static org.junit.platform.testkit.engine.EventConditions.started;

import java.util.List;

import org.assertj.core.error.AssertJMultipleFailuresError;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;
import org.junit.platform.engine.TestDescriptor;
import org.junit.platform.engine.TestExecutionResult;
import org.junit.platform.engine.UniqueId;
import org.junit.platform.engine.support.descriptor.EngineDescriptor;
import org.opentest4j.AssertionFailedError;

class EventsTests {

	TestDescriptor engineDescriptor = new EngineDescriptor(UniqueId.forEngine("e1"), "engine");

	List<Event> list = List.of(Event.executionStarted(engineDescriptor),
		Event.executionSkipped(engineDescriptor, "reason1"), Event.executionSkipped(engineDescriptor, "reason2"),
		Event.executionFinished(engineDescriptor, TestExecutionResult.successful()));

	Events events = new Events(list, "test");

	@Test
	@DisplayName("assertEventsMatchExactly: all events in order -> match")
	void assertEventsMatchExactlyMatchesAllEventsInOrder() {
		events.assertEventsMatchExactly( //
			event(engine(), started()), //
			event(engine(), skippedWithReason("reason1")), //
			event(engine(), skippedWithReason("reason2")), //
			event(engine(), finishedSuccessfully()) //
		);
	}

	@Test
	@DisplayName("assertEventsMatchLoosely: all events in order -> match")
	void assertEventsMatchLooselyMatchesAllEventsInOrder() {
		events.assertEventsMatchLoosely( //
			event(engine(), started()), //
			event(engine(), skippedWithReason("reason1")), //
			event(engine(), skippedWithReason("reason2")), //
			event(engine(), finishedSuccessfully()) //
		);
	}

	@Test
	@DisplayName("assertEventsMatchLoosely: all events in wrong order -> match")
	void assertEventsMatchLooselyMatchesAllEventsInWrongOrder() {
		events.assertEventsMatchLoosely( //
			event(engine(), skippedWithReason("reason2")), //
			event(engine(), finishedSuccessfully()), //
			event(engine(), skippedWithReason("reason1")), //
			event(engine(), started()) //
		);
	}

	@Test
	@DisplayName("assertEventsMatchLoosely: tailing subset -> match")
	void assertEventsMatchLooselyMatchesATailingSubset() {
		events.assertEventsMatchLoosely( //
			event(engine(), skippedWithReason("reason1")), //
			event(engine(), finishedSuccessfully()) //
		);
	}

	@Test
	@DisplayName("assertEventsMatchLoosely: starting subset -> match")
	void assertEventsMatchLooselyMatchesAStartingSubset() {
		events.assertEventsMatchLoosely( //
			event(engine(), started()), //
			event(engine(), skippedWithReason("reason1")) //
		);
	}

	@Test
	@DisplayName("assertEventsMatchLoosely: subset in wrong order -> match")
	void assertEventsMatchLooselyMatchesASubsetInWrongOrder() {
		events.assertEventsMatchLoosely( //
			event(engine(), skippedWithReason("reason1")), //
			event(engine(), started()) //
		);
	}

	@Test
	@DisplayName("assertEventsMatchLoosely: only last event -> match")
	void assertEventsMatchLooselyMatchesTheLastEventAlone() {
		events.assertEventsMatchLoosely( //
			event(engine(), finishedSuccessfully()) //
		);
	}

	@Test
	@DisplayName("assertEventsMatchLoosely: only first event -> match")
	void assertEventsMatchLooselyMatchesTheFirstEventAlone() {
		events.assertEventsMatchLoosely( //
			event(engine(), started()) //
		);
	}

	@Test
	@DisplayName("assertEventsMatchLoosely: only bad events -> fails")
	void assertEventsMatchLooselyWithBadConditionsOnlyFails() {
		Executable willFail = () -> events.assertEventsMatchLoosely( //
			event(engine(), finishedWithFailure()), //
			event(engine(), skippedWithReason("other")) //
		);

		var error = assertThrows(AssertJMultipleFailuresError.class, willFail);

		var failures = error.getFailures();
		assertEquals(2, failures.size());
		assertEquals(AssertionError.class, failures.get(0).getClass());
		assertEquals(AssertionError.class, failures.get(1).getClass());
	}

	@Test
	@DisplayName("assertEventsMatchLoosely: one matching and one bad event -> fails")
	void assertEventsMatchLooselyWithOneMatchingAndOneBadConditionFailsPartly() {
		Executable willFail = () -> events.assertEventsMatchLoosely( //
			event(engine(), started()), //
			event(engine(), finishedWithFailure()) //
		);

		var error = assertThrows(AssertJMultipleFailuresError.class, willFail);

		var failures = error.getFailures();
		assertEquals(1, failures.size());
		assertEquals(AssertionError.class, failures.getFirst().getClass());
	}

	@Test
	@DisplayName("assertEventsMatchLooselyInOrder: all events in order -> match")
	void assertEventsMatchLooselyInOrderMatchesAllEventsInOrder() {
		events.assertEventsMatchLooselyInOrder( //
			event(engine(), started()), //
			event(engine(), skippedWithReason("reason1")), //
			event(engine(), skippedWithReason("reason2")), //
			event(engine(), finishedSuccessfully()) //
		);
	}

	@Test
	@DisplayName("assertEventsMatchLooselyInOrder: all events in wrong order -> fail")
	void assertEventsMatchLooselyInOrderWithAllEventsInWrongOrderFails() {
		Executable willFail = () -> events.assertEventsMatchLooselyInOrder( //
			event(engine(), skippedWithReason("reason2")), //
			event(engine(), finishedSuccessfully()), //
			event(engine(), skippedWithReason("reason1")), //
			event(engine(), started()) //
		);

		var error = assertThrows(AssertionFailedError.class, willFail);
		assertThat(error).hasMessageContaining("Conditions are not in the correct order.");
	}

	@Test
	@DisplayName("assertEventsMatchLooselyInOrder: tailing subset in order -> match")
	void assertEventsMatchLooselyInOrderMatchesATailingSubset() {
		events.assertEventsMatchLooselyInOrder( //
			event(engine(), skippedWithReason("reason1")), //
			event(engine(), finishedSuccessfully()) //
		);
	}

	@Test
	@DisplayName("assertEventsMatchLooselyInOrder: starting subset in order -> match")
	void assertEventsMatchLooselyInOrderMatchesAStartingSubset() {
		events.assertEventsMatchLooselyInOrder( //
			event(engine(), started()), //
			event(engine(), skippedWithReason("reason1")) //
		);
	}

	@Test
	@DisplayName("assertEventsMatchLooselyInOrder: subset in wrong order -> fail")
	void assertEventsMatchLooselyInOrderWithASubsetInWrongOrderFails() {
		Executable willFail = () -> events.assertEventsMatchLooselyInOrder( //
			event(engine(), skippedWithReason("reason1")), //
			event(engine(), started()) //
		);

		var error = assertThrows(AssertionFailedError.class, willFail);
		assertThat(error).hasMessageContaining("Conditions are not in the correct order.");
	}

	@Test
	@DisplayName("assertEventsMatchLooselyInOrder: last event alone -> match")
	void assertEventsMatchLooselyInOrderMatchesTheLastEventAlone() {
		events.assertEventsMatchLooselyInOrder( //
			event(engine(), finishedSuccessfully()) //
		);
	}

	@Test
	@DisplayName("assertEventsMatchLooselyInOrder: first event alone -> match")
	void assertEventsMatchLooselyInOrderMatchesTheFirstEventAlone() {
		events.assertEventsMatchLooselyInOrder( //
			event(engine(), started()) //
		);
	}

	@Test
	@DisplayName("assertEventsMatchLooselyInOrder: bad events only -> fail")
	void assertEventsMatchLooselyInOrderWithBadConditionsOnlyFails() {
		Executable willFail = () -> events.assertEventsMatchLooselyInOrder( //
			event(engine(), finishedWithFailure()), //
			event(engine(), skippedWithReason("other")) //
		);

		var error = assertThrows(AssertJMultipleFailuresError.class, willFail);

		var failures = error.getFailures();
		assertEquals(2, failures.size());
		assertEquals(AssertionError.class, failures.get(0).getClass());
		assertEquals(AssertionError.class, failures.get(1).getClass());
	}

	@Test
	@DisplayName("assertEventsMatchLooselyInOrder: one matching event and one bad event -> fail")
	void assertEventsMatchLooselyInOrderWithOneMatchingAndOneBadConditionFailsPartly() {
		Executable willFail = () -> events.assertEventsMatchLooselyInOrder( //
			event(engine(), started()), //
			event(engine(), finishedWithFailure()) //
		);

		var error = assertThrows(AssertJMultipleFailuresError.class, willFail);
		assertEquals(1, error.getFailures().size());
	}

	@Test
	@DisplayName("assertEventsMatchLooselyInOrder: first and last event in order -> match")
	void assertEventsMatchLooselyInOrderMatchesFirstAndLastEventInOrder() {
		events.assertEventsMatchLooselyInOrder( //
			event(engine(), started()), //
			event(engine(), finishedSuccessfully()) //
		);
	}

	@Test
	@DisplayName("assertEventsMatchLooselyInOrder: second and last event in bad order -> fail")
	void assertEventsMatchLooselyInOrderWithSecondAndLastEventInBadOrderFails() {
		Executable willFail = () -> events.assertEventsMatchLooselyInOrder( //
			event(engine(), finishedSuccessfully()), //
			event(engine(), skippedWithReason("reason1")) //
		);

		var error = assertThrows(AssertionFailedError.class, willFail);
		assertThat(error).hasMessageContaining("Conditions are not in the correct order.");
	}

	@Test
	@DisplayName("assertEventsMatchLooselyInOrder: too many events -> fail")
	void assertEventsMatchLooselyInOrderWithTooManyEventsFails() {
		Executable willFail = () -> events.assertEventsMatchLooselyInOrder( //
			event(engine(), finishedSuccessfully()), //
			event(engine(), finishedSuccessfully()), //
			event(engine(), finishedSuccessfully()), //
			event(engine(), finishedSuccessfully()), //
			event(engine(), finishedSuccessfully()), //
			event(engine(), finishedSuccessfully()));

		var error = assertThrows(AssertionError.class, willFail);
		assertThat(error).hasMessageEndingWith("to be less than or equal to 4 but was 6");
	}

}
