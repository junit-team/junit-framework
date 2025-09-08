/*
 * Copyright 2015-2025 the original author or authors.
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v2.0 which
 * accompanies this distribution and is available at
 *
 * https://www.eclipse.org/legal/epl-v20.html
 */

package org.junit.platform.console.output;

import static org.junit.jupiter.api.Assertions.assertLinesMatch;
import static org.junit.platform.commons.util.CollectionUtils.getOnlyElement;
import static org.junit.platform.launcher.core.OutputDirectoryCreators.dummyOutputDirectoryCreator;
import static org.mockito.Mockito.mock;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Set;
import java.util.stream.Stream;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.platform.engine.TestExecutionResult;
import org.junit.platform.engine.UniqueId;
import org.junit.platform.engine.support.descriptor.EngineDescriptor;
import org.junit.platform.fakes.TestDescriptorStub;
import org.junit.platform.launcher.TestIdentifier;
import org.junit.platform.launcher.TestPlan;
import org.opentest4j.TestAbortedException;

public class TestFeedPrintingListenerTests {

	TestPlan testPlan;
	TestIdentifier testIdentifier;

	StringWriter stringWriter = new StringWriter();
	TestFeedPrintingListener listener = new TestFeedPrintingListener(new PrintWriter(stringWriter), ColorPalette.NONE);

	@BeforeEach
	void prepareListener() {
		var engineDescriptor = new EngineDescriptor(UniqueId.forEngine("demo-engine"), "Demo Engine");
		var testDescriptor = new TestDescriptorStub(engineDescriptor.getUniqueId().append("type", "test"),
			"%c ool test");
		engineDescriptor.addChild(testDescriptor);

		testPlan = TestPlan.from(true, Set.of(engineDescriptor), mock(), dummyOutputDirectoryCreator());
		testIdentifier = testPlan.getTestIdentifier(testDescriptor.getUniqueId());

		listener.testPlanExecutionStarted(testPlan);
	}

	@Test
	public void testExecutionSkipped() {
		listener.executionSkipped(testIdentifier, "Test disabled");
		assertLinesMatch( //
			"""
					Demo Engine > %c ool test :: SKIPPED
					\tReason: Test disabled
					""".lines(), //
			actualLines() //
		);
	}

	@Test
	public void testExecutionFailed() {
		listener.executionFinished(testIdentifier, TestExecutionResult.failed(new AssertionError("Boom!")));
		assertLinesMatch( //
			"""
					Demo Engine > %c ool test :: FAILED
					\tjava.lang.AssertionError: Boom!
					>>>>
					""".lines(), //
			actualLines() //
		);
	}

	@Test
	public void testExecutionAborted() {
		listener.executionFinished(testIdentifier, TestExecutionResult.aborted(new TestAbortedException("Boom!")));
		assertLinesMatch( //
			"""
					Demo Engine > %c ool test :: ABORTED
					\torg.opentest4j.TestAbortedException: Boom!
					>>>>
					""".lines(), //
			actualLines() //
		);
	}

	@Test
	public void testExecutionSucceeded() {
		listener.executionFinished(testIdentifier, TestExecutionResult.successful());
		assertLinesMatch(Stream.of("Demo Engine > %c ool test :: SUCCESSFUL"), actualLines());
	}

	@Test
	public void testExecutionFailedOfContainer() {
		var engineIdentifier = getOnlyElement(testPlan.getRoots());
		listener.executionFinished(engineIdentifier, TestExecutionResult.failed(new AssertionError("Boom!")));
		assertLinesMatch( //
			"""
					Demo Engine :: FAILED
					\tjava.lang.AssertionError: Boom!
					>>>>
					""".lines(), //
			actualLines() //
		);
	}

	private Stream<String> actualLines() {
		return stringWriter.toString().lines();
	}

}
