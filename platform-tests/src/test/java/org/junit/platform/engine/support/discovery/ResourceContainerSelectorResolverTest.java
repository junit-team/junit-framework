/*
 * Copyright 2015-2026 the original author or authors.
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v2.0 which
 * accompanies this distribution and is available at
 *
 * https://www.eclipse.org/legal/epl-v20.html
 */

package org.junit.platform.engine.support.discovery;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.platform.engine.discovery.DiscoverySelectors.selectClasspathRoots;
import static org.junit.platform.engine.discovery.DiscoverySelectors.selectPackage;
import static org.junit.platform.engine.discovery.PackageNameFilter.includePackageNames;
import static org.junit.platform.engine.support.discovery.SelectorResolver.Match.exact;
import static org.junit.platform.engine.support.discovery.SelectorResolver.Resolution.match;

import java.net.URI;
import java.nio.file.Path;
import java.util.Optional;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.junit.platform.commons.io.ResourceFilter;
import org.junit.platform.engine.TestDescriptor;
import org.junit.platform.engine.UniqueId;
import org.junit.platform.engine.discovery.ClasspathResourceSelector;
import org.junit.platform.engine.support.descriptor.EngineDescriptor;
import org.junit.platform.fakes.TestDescriptorStub;
import org.junit.platform.launcher.core.LauncherDiscoveryRequestBuilder;

class ResourceContainerSelectorResolverTest {

	final TestDescriptor engineDescriptor = new EngineDescriptor(UniqueId.forEngine("resource-engine"),
		"Resource Engine");
	final ResourceFilter resourceFilter = ResourceFilter.of(resource -> resource.getName().endsWith(".resource"));

	final EngineDiscoveryRequestResolver<TestDescriptor> resolver = EngineDiscoveryRequestResolver.builder() //
			.addResourceContainerSelectorResolver(resourceFilter) //
			.addSelectorResolver(new ResourceSelectorResolver()) //
			.build();

	@Test
	void shouldDiscoverAllResourcesInPackage() {
		var request = LauncherDiscoveryRequestBuilder.request() //
				.selectors(selectPackage("org.junit.platform.commons")) //
				.build();

		resolver.resolve(request, engineDescriptor);

		// @formatter:off
		assertThat(engineDescriptor.getChildren())
				.extracting(TestDescriptor::getDisplayName)
				.containsExactlyInAnyOrder(
						"org/junit/platform/commons/example.resource",
						"org/junit/platform/commons/other-example.resource");
		// @formatter:on
	}

	@Test
	void shouldDiscoverAllResourcesInRootPackage() {
		var request = LauncherDiscoveryRequestBuilder.request() //
				.selectors(selectPackage("")) //
				.build();

		resolver.resolve(request, engineDescriptor);

		// @formatter:off
		assertThat(engineDescriptor.getChildren())
				.extracting(TestDescriptor::getDisplayName)
				.containsExactlyInAnyOrder(
						"default-package.resource",
						"org/junit/platform/commons/example.resource",
						"org/junit/platform/commons/other-example.resource");
		// @formatter:on
	}

	@Test
	void shouldFilterPackages() {
		var request = LauncherDiscoveryRequestBuilder.request() //
				.selectors(selectPackage("")) //
				.filters(includePackageNames("org.junit.platform")) //
				.build();

		resolver.resolve(request, engineDescriptor);

		// @formatter:off
		assertThat(engineDescriptor.getChildren())
				.extracting(TestDescriptor::getDisplayName)
				.containsExactlyInAnyOrder(
						"org/junit/platform/commons/example.resource",
						"org/junit/platform/commons/other-example.resource");
		// @formatter:on
	}

	@Test
	void shouldDiscoverAllResourcesInClasspathRoot() {
		var request = LauncherDiscoveryRequestBuilder.request() //
				.selectors(selectClasspathRoots(getTestClasspathResourceRoot())) //
				.build();

		resolver.resolve(request, engineDescriptor);

		// @formatter:off
		assertThat(engineDescriptor.getChildren())
				.extracting(TestDescriptor::getDisplayName)
				.containsExactlyInAnyOrder(
						"default-package.resource",
						"org/junit/platform/commons/example.resource",
						"org/junit/platform/commons/other-example.resource");
		// @formatter:on
	}

	private Set<Path> getTestClasspathResourceRoot() {
		// Gradle puts classes and resources in different roots.
		var defaultPackageResource = "/default-package.resource";
		var resourceUri = getClass().getResource(defaultPackageResource).toString();
		var uri = URI.create(resourceUri.substring(0, resourceUri.length() - defaultPackageResource.length()));
		return Set.of(Path.of(uri));
	}

	private static class ResourceSelectorResolver implements SelectorResolver {
		@Override
		public Resolution resolve(ClasspathResourceSelector selector, Context context) {
			return context.addToParent(parent -> createTestDescriptor(parent, selector.getClasspathResourceName())) //
					.map(testDescriptor -> match(exact(testDescriptor))) //
					.orElseGet(Resolution::unresolved);
		}

		private static Optional<TestDescriptorStub> createTestDescriptor(TestDescriptor parent,
				String classpathResourceName) {
			var uniqueId = parent.getUniqueId().append("resource", classpathResourceName);
			var descriptor = new TestDescriptorStub(uniqueId, classpathResourceName);
			return Optional.of(descriptor);
		}
	}
}
