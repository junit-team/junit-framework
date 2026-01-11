/*
 * Copyright 2015-2026 the original author or authors.
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v2.0 which
 * accompanies this distribution and is available at
 *
 * https://www.eclipse.org/legal/epl-v20.html
 */

package org.junit.jupiter.api.extension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

class Heavyweight implements ParameterResolver, BeforeEachCallback {

	@Override
	public void beforeEach(ExtensionContext context) {
		context.getStore(ExtensionContext.Namespace.GLOBAL).put("once", new CloseableOnlyOnceResource());
	}

	@Override
	public boolean supportsParameter(ParameterContext parameterContext, ExtensionContext context) {
		return Resource.class.equals(parameterContext.getParameter().getType());
	}

	@Override
	public Object resolveParameter(ParameterContext parameterContext, ExtensionContext context) {
		var engineContext = context.getRoot();
		var store = engineContext.getStore(ExtensionContext.Namespace.GLOBAL);
		var resource = store.computeIfAbsent(ResourceValue.class);
		resource.usages.incrementAndGet();
		return resource;
	}

	interface Resource {
		String ID = "org.junit.jupiter.extensions.Heavyweight.Resource";

		int usages();
	}

	/**
	 * Demo resource class.
	 *
	 * <p>The class implements interface {@link AutoCloseable}
	 * and interface {@link AutoCloseable} to show and ensure that a single
	 * {@link ResourceValue#close()} method implementation is needed to comply
	 * with both interfaces.
	 */
	static class ResourceValue implements Resource, AutoCloseable {

		static final AtomicInteger creations = new AtomicInteger();
		private final AtomicInteger usages = new AtomicInteger();

		@SuppressWarnings("unused") // used via reflection
		ResourceValue() {
			// Open long-living resources here.
			assertEquals(1, creations.incrementAndGet(), "Singleton pattern failure!");
		}

		@Override
		public void close() {
			// Close resources here.
			assertEquals(9, usages.get(), "Usage counter mismatch!");
		}

		@Override
		public int usages() {
			return usages.get();
		}
	}

	private static class CloseableOnlyOnceResource implements AutoCloseable {

		private final AtomicBoolean closed = new AtomicBoolean();

		@Override
		public void close() {
			assertTrue(closed.compareAndSet(false, true), "already closed!");
		}
	}

}
