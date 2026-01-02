/*
 * Copyright 2015-2025 the original author or authors.
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v2.0 which
 * accompanies this distribution and is available at
 *
 * https://www.eclipse.org/legal/epl-v20.html
 */

package org.junit.jupiter.api.extension;

import static org.junit.jupiter.api.extension.ExtensionContext.Namespace.GLOBAL;
import static org.junit.platform.testkit.engine.EventConditions.event;
import static org.junit.platform.testkit.engine.EventConditions.finishedWithFailure;
import static org.junit.platform.testkit.engine.EventConditions.reportEntry;
import static org.junit.platform.testkit.engine.EventConditions.test;
import static org.junit.platform.testkit.engine.TestExecutionResultConditions.cause;
import static org.junit.platform.testkit.engine.TestExecutionResultConditions.message;
import static org.junit.platform.testkit.engine.TestExecutionResultConditions.suppressed;

import java.util.Map;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.engine.AbstractJupiterTestEngineTests;

public class CloseableResourceIntegrationTests extends AbstractJupiterTestEngineTests {

	@Test
	void closesCloseableResourcesInReverseInsertOrder() {
		executeTestsForClass(TestCase.class).allEvents().reportingEntryPublished() //
				.assertEventsMatchExactly( //
					reportEntry(Map.of("3", "closed")), //
					reportEntry(Map.of("2", "closed")), //
					reportEntry(Map.of("1", "closed")));
	}

	@ExtendWith(ExtensionContextParameterResolver.class)
	static class TestCase {
		@Test
		void closesCloseableResourcesInExtensionContext(ExtensionContext extensionContext) {
			ExtensionContext.Store store = extensionContext.getStore(GLOBAL);
			store.put("foo", reportEntryOnClose(extensionContext, "1"));
			store.put("bar", reportEntryOnClose(extensionContext, "2"));
			store.put("baz", reportEntryOnClose(extensionContext, "3"));
		}

		private AutoCloseable reportEntryOnClose(ExtensionContext extensionContext, String key) {
			return () -> extensionContext.publishReportEntry(Map.of(key, "closed"));
		}
	}

	@Test
	void exceptionsDuringCloseAreReportedAsSuppressed() {
		executeTestsForClass(ExceptionInCloseableResourceTestCase.class).testEvents() //
				.assertEventsMatchLoosely(event( //
					test(), //
					finishedWithFailure( //
						message("Exception in test"), //
						suppressed(0, //
							message("Failed to close extension context"), //
							cause(message("Exception in onClose")) //
						))));
	}

	@ExtendWith(ThrowingOnCloseExtension.class)
	static class ExceptionInCloseableResourceTestCase {

		@Test
		void test() {
			throw new RuntimeException("Exception in test");
		}

	}

	static class ThrowingOnCloseExtension implements BeforeEachCallback {

		@Override
		public void beforeEach(ExtensionContext context) {
			context.getStore(GLOBAL).put("throwingResource", new ThrowingResource());
		}
	}

	@SuppressWarnings({ "deprecation", "try" })
	static class ThrowingResource implements ExtensionContext.Store.CloseableResource, AutoCloseable {

		@Override
		public void close() throws Exception {
			throw new RuntimeException("Exception in onClose");
		}
	}

}
