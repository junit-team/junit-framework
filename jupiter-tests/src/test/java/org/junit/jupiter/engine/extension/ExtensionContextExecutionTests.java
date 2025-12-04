/*
 * Copyright 2015-2025 the original author or authors.
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v2.0 which
 * accompanies this distribution and is available at
 *
 * https://www.eclipse.org/legal/epl-v20.html
 */

package org.junit.jupiter.engine.extension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.platform.engine.discovery.DiscoverySelectors.selectClass;

import java.lang.reflect.Method;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.ExtensionContextParameterResolver;
import org.junit.jupiter.engine.AbstractJupiterTestEngineTests;

class ExtensionContextExecutionTests extends AbstractJupiterTestEngineTests {

	@Test
	@ExtendWith(ExtensionContextParameterResolver.class)
	void extensionContextHierarchy(ExtensionContext methodExtensionContext) {
		assertThat(methodExtensionContext).isNotNull();
		assertThat(methodExtensionContext.getElement()).containsInstanceOf(Method.class);

		Optional<ExtensionContext> classExtensionContext = methodExtensionContext.getParent();
		assertThat(classExtensionContext).isNotEmpty();
		assertThat(classExtensionContext.orElseThrow().getElement()).contains(ExtensionContextExecutionTests.class);

		Optional<ExtensionContext> engineExtensionContext = classExtensionContext.orElseThrow().getParent();
		assertThat(engineExtensionContext).isNotEmpty();
		assertThat(engineExtensionContext.orElseThrow().getElement()).isEmpty();

		assertThat(engineExtensionContext.orElseThrow().getParent()).isEmpty();
	}

	@Test
	void twoTestClassesCanShareStateViaEngineExtensionContext() {
		Parent.counter.set(0);

		executeTests(selectClass(A.class), selectClass(B.class)).testEvents()//
				.assertStatistics(stats -> stats.started(2));

		assertThat(Parent.counter).hasValue(1);
	}

	@SuppressWarnings("JUnitMalformedDeclaration")
	@ExtendWith(OnlyIncrementCounterOnce.class)
	static class Parent {
		static final AtomicInteger counter = new AtomicInteger(0);

		@Test
		void test() {
		}
	}

	static class A extends Parent {
	}

	static class B extends Parent {
	}

	static class OnlyIncrementCounterOnce implements BeforeAllCallback {
		@Override
		public void beforeAll(ExtensionContext context) {
			ExtensionContext.Store store = getRoot(context).getStore(ExtensionContext.Namespace.GLOBAL);
			store.computeIfAbsent("counter", key -> Parent.counter.incrementAndGet());
		}

		private ExtensionContext getRoot(ExtensionContext context) {
			return context.getParent().map(this::getRoot).orElse(context);
		}
	}

}
