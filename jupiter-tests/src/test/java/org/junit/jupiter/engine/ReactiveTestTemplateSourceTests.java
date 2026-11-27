/*
 * Copyright 2015-2026 the original author or authors.
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v2.0 which
 * accompanies this distribution and is available at
 *
 * https://www.eclipse.org/legal/epl-v20.html
 */

package org.junit.jupiter.engine;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Constants.JUPITER_EXECUTION_REACTIVE_PROPERTY_NAME;
import static org.junit.platform.engine.discovery.DiscoverySelectors.selectClass;

import java.lang.reflect.Method;
import java.net.URI;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.stream.Stream;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestTemplate;
import org.junit.jupiter.api.extension.AsyncInvocationInterceptor;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.extension.Extension;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.ReflectiveInvocationContext;
import org.junit.jupiter.api.extension.TestTemplateInvocationContext;
import org.junit.jupiter.api.extension.TestTemplateInvocationContextProvider;
import org.junit.platform.engine.support.descriptor.ClasspathResourceSource;
import org.junit.platform.engine.support.descriptor.FilePosition;
import org.junit.platform.testkit.engine.EngineTestKit;

class ReactiveTestTemplateSourceTests {

	@Test
	void preservesCustomSourceAndAsyncTemplateInterceptor() {
		final var results = EngineTestKit.engine(new JupiterTestEngine()).configurationParameter(
			JUPITER_EXECUTION_REACTIVE_PROPERTY_NAME, "true").selectors(
				selectClass(AsyncTemplateTestCase.class)).execute();

		results.testEvents().assertStatistics(stats -> stats.started(1).succeeded(1).failed(0));
		final var expectedSource = ClasspathResourceSource.from("async-template.js", FilePosition.from(42));
		assertThat(results.testEvents().started().list()).singleElement().satisfies(
			event -> assertThat(event.getTestDescriptor().getSource()).contains(expectedSource));
	}

	@ExtendWith(TemplateProvider.class)
	static class AsyncTemplateTestCase {

		private boolean intercepted;
		private boolean completed;

		@TestTemplate
		CompletionStage<Void> template() {
			return CompletableFuture.runAsync(() -> {
				assertTrue(intercepted, "the async template interceptor must be used");
				completed = true;
			});
		}

		@AfterEach
		void checkCompletion() {
			assertTrue(completed, "the async template must complete before afterEach");
		}
	}

	static class TemplateProvider implements TestTemplateInvocationContextProvider {

		@Override
		public boolean supportsTestTemplate(final ExtensionContext context) {
			return true;
		}

		@Override
		public Stream<TestTemplateInvocationContext> provideTestTemplateInvocationContexts(
				final ExtensionContext context) {
			return Stream.of(new TestTemplateInvocationContext() {
				@Override
				public URI testSourceUri() {
					return URI.create("classpath:/async-template.js?line=42");
				}

				@Override
				public List<Extension> getAdditionalExtensions() {
					return List.of(new AsyncInvocationInterceptor() {
						@Override
						public CompletionStage<Void> interceptTestTemplateMethodAsync(
								final AsyncInvocation<Void> invocation,
								final ReflectiveInvocationContext<Method> invocationContext,
								final ExtensionContext extensionContext) {
							((AsyncTemplateTestCase) extensionContext.getRequiredTestInstance()).intercepted = true;
							return invocation.proceedAsync();
						}
					});
				}
			});
		}
	}

}
