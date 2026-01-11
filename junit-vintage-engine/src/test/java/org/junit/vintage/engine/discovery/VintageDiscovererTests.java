/*
 * Copyright 2015-2026 the original author or authors.
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v2.0 which
 * accompanies this distribution and is available at
 *
 * https://www.eclipse.org/legal/epl-v20.html
 */

package org.junit.vintage.engine.discovery;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.platform.commons.util.CollectionUtils.getOnlyElement;
import static org.junit.platform.engine.SelectorResolutionResult.Status.FAILED;
import static org.junit.platform.engine.SelectorResolutionResult.Status.UNRESOLVED;
import static org.junit.platform.engine.discovery.DiscoverySelectors.selectClass;
import static org.junit.platform.engine.discovery.DiscoverySelectors.selectUniqueId;
import static org.junit.platform.launcher.core.LauncherDiscoveryRequestBuilder.DEFAULT_DISCOVERY_LISTENER_CONFIGURATION_PROPERTY_NAME;
import static org.junit.platform.launcher.core.LauncherDiscoveryRequestBuilder.request;
import static org.junit.vintage.engine.VintageUniqueIdBuilder.engineId;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import java.util.function.Consumer;

import org.junit.jupiter.api.Test;
import org.junit.platform.engine.DiscoverySelector;
import org.junit.platform.engine.EngineDiscoveryRequest;
import org.junit.platform.engine.SelectorResolutionResult;
import org.junit.platform.engine.TestDescriptor;
import org.junit.platform.engine.UniqueId;
import org.junit.platform.engine.discovery.ClassNameFilter;
import org.junit.platform.engine.discovery.PackageNameFilter;
import org.junit.platform.launcher.LauncherDiscoveryListener;
import org.junit.vintage.engine.VintageUniqueIdBuilder;
import org.junit.vintage.engine.samples.junit3.AbstractJUnit3TestCase;
import org.junit.vintage.engine.samples.junit4.AbstractJunit4TestCaseWithConstructorParameter;
import org.mockito.ArgumentCaptor;

/**
 * Tests for {@link VintageDiscoverer}.
 *
 * @since 4.12
 */
class VintageDiscovererTests {

	@Test
	void classNameFilterExcludesClass() {
		// @formatter:off
		EngineDiscoveryRequest request = request()
				.selectors(selectClass(Foo.class), selectClass(Bar.class))
				.filters(ClassNameFilter.includeClassNamePatterns(".*Foo"))
				.build();
		// @formatter:on

		var testDescriptor = discover(request);

		assertThat(testDescriptor.getChildren()).hasSize(1);
		assertThat(getOnlyElement(testDescriptor.getChildren()).getUniqueId().toString()).contains(Foo.class.getName());
	}

	@Test
	void packageNameFilterExcludesClasses() {
		// @formatter:off
		EngineDiscoveryRequest request = request()
				.selectors(selectClass(Foo.class), selectClass(Bar.class))
				.filters(PackageNameFilter.excludePackageNames("org.junit.vintage.engine.discovery"))
				.build();
		// @formatter:on

		var testDescriptor = discover(request);

		assertThat(testDescriptor.getChildren()).isEmpty();
	}

	@Test
	void doesNotResolveAbstractJUnit3Classes() {
		doesNotResolve(selectClass(AbstractJUnit3TestCase.class));
	}

	@Test
	void doesNotResolveAbstractJUnit4Classes() {
		doesNotResolve(selectClass(AbstractJunit4TestCaseWithConstructorParameter.class));
	}

	@Test
	void failsToResolveUnloadableTestClass() {
		var uniqueId = VintageUniqueIdBuilder.uniqueIdForClass("foo.bar.UnknownClass");

		doesNotResolve(selectUniqueId(uniqueId), result -> {
			assertThat(result.getStatus()).isEqualTo(FAILED);
			assertThat(result.getThrowable().orElseThrow()).hasMessageContaining("Unknown class");
		});
	}

	@Test
	void ignoresUniqueIdsOfOtherEngines() {
		doesNotResolve(selectUniqueId(UniqueId.forEngine("someEngine")));
	}

	private void doesNotResolve(DiscoverySelector selector) {
		doesNotResolve(selector, result -> assertThat(result.getStatus()).isEqualTo(UNRESOLVED));
	}

	private void doesNotResolve(DiscoverySelector selector, Consumer<SelectorResolutionResult> resultCheck) {
		var discoveryListener = mock(LauncherDiscoveryListener.class);
		var request = request() //
				.selectors(selector) //
				.listeners(discoveryListener) //
				.configurationParameter(DEFAULT_DISCOVERY_LISTENER_CONFIGURATION_PROPERTY_NAME, "logging") //
				.build();

		var testDescriptor = discover(request);

		assertThat(testDescriptor.getChildren()).isEmpty();
		var resultCaptor = ArgumentCaptor.forClass(SelectorResolutionResult.class);
		verify(discoveryListener).selectorProcessed(eq(UniqueId.forEngine("junit-vintage")), eq(selector),
			resultCaptor.capture());
		resultCheck.accept(resultCaptor.getValue());
	}

	private TestDescriptor discover(EngineDiscoveryRequest request) {
		return new VintageDiscoverer().discover(request, engineId());
	}

	@SuppressWarnings("NewClassNamingConvention")
	public static class Foo {

		@org.junit.Test
		public void test() {
		}

	}

	@SuppressWarnings("NewClassNamingConvention")
	public static class Bar {

		@org.junit.Test
		public void test() {
		}

	}

}
