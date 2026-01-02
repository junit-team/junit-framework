/*
 * Copyright 2015-2025 the original author or authors.
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v2.0 which
 * accompanies this distribution and is available at
 *
 * https://www.eclipse.org/legal/epl-v20.html
 */

package org.junit.platform.engine.support.discovery;

import static org.junit.platform.engine.DiscoveryIssue.Severity.INFO;
import static org.junit.platform.engine.DiscoveryIssue.Severity.WARNING;
import static org.junit.platform.engine.discovery.DiscoverySelectors.selectClass;
import static org.junit.platform.engine.support.discovery.SelectorResolver.Resolution.unresolved;
import static org.junit.platform.launcher.core.LauncherDiscoveryRequestBuilder.request;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import org.jspecify.annotations.NullMarked;
import org.junit.jupiter.api.Test;
import org.junit.platform.engine.DiscoveryIssue;
import org.junit.platform.engine.UniqueId;
import org.junit.platform.engine.discovery.ClassSelector;
import org.junit.platform.engine.support.descriptor.ClassSource;
import org.junit.platform.engine.support.descriptor.EngineDescriptor;
import org.junit.platform.launcher.LauncherDiscoveryListener;

@NullMarked
class EngineDiscoveryRequestResolverTests {

	@Test
	void allowsSelectorResolversToReportDiscoveryIssues() {
		var resolver = EngineDiscoveryRequestResolver.builder() //
				.addSelectorResolver(ctx -> new SelectorResolver() {
					@Override
					public Resolution resolve(ClassSelector selector, Context context) {
						ctx.getIssueReporter() //
								.reportIssue(DiscoveryIssue.builder(INFO, "test") //
										.source(ClassSource.from(selector.getClassName())));
						return unresolved();
					}
				}) //
				.build();

		var engineId = UniqueId.forEngine("engine");
		var engineDescriptor = new EngineDescriptor(engineId, "Engine");
		var listener = mock(LauncherDiscoveryListener.class);
		var request = request() //
				.selectors(selectClass(EngineDiscoveryRequestResolverTests.class)) //
				.listeners(listener) //
				.build();

		resolver.resolve(request, engineDescriptor);

		var issue = DiscoveryIssue.builder(INFO, "test") //
				.source(ClassSource.from(EngineDiscoveryRequestResolverTests.class)) //
				.build();
		verify(listener).issueEncountered(engineId, issue);
	}

	@Test
	void allowsVisitorsToReportDiscoveryIssues() {
		var resolver = EngineDiscoveryRequestResolver.builder() //
				.addTestDescriptorVisitor(ctx -> //
				descriptor -> ctx.getIssueReporter() //
						.reportIssue(DiscoveryIssue.create(WARNING, descriptor.getDisplayName()))) //
				.build();

		var engineId = UniqueId.forEngine("engine");
		var engineDescriptor = new EngineDescriptor(engineId, "Engine");
		var listener = mock(LauncherDiscoveryListener.class);
		var request = request() //
				.selectors(selectClass(EngineDiscoveryRequestResolverTests.class)) //
				.listeners(listener) //
				.build();

		resolver.resolve(request, engineDescriptor);

		verify(listener).issueEncountered(engineId, DiscoveryIssue.create(WARNING, "Engine"));
	}

}
