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

import static java.util.stream.Collectors.groupingBy;
import static java.util.stream.Collectors.toSet;
import static org.junit.platform.commons.support.ResourceSupport.findAllResourcesInClasspathRoot;
import static org.junit.platform.commons.support.ResourceSupport.findAllResourcesInModule;
import static org.junit.platform.commons.support.ResourceSupport.findAllResourcesInPackage;
import static org.junit.platform.engine.support.discovery.ResourceUtils.packageName;
import static org.junit.platform.engine.support.discovery.SelectorResolver.Resolution.selectors;
import static org.junit.platform.engine.support.discovery.SelectorResolver.Resolution.unresolved;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Predicate;

import org.junit.platform.commons.io.Resource;
import org.junit.platform.commons.io.ResourceFilter;
import org.junit.platform.engine.discovery.ClasspathResourceSelector;
import org.junit.platform.engine.discovery.ClasspathRootSelector;
import org.junit.platform.engine.discovery.DiscoverySelectors;
import org.junit.platform.engine.discovery.ModuleSelector;
import org.junit.platform.engine.discovery.PackageSelector;

/**
 * @since 1.12
 */
class ResourceContainerSelectorResolver implements SelectorResolver {
	private final ResourceFilter resourceFilter;

	ResourceContainerSelectorResolver(ResourceFilter resourceFilter, Predicate<String> packageFilter) {
		this.resourceFilter = ResourceFilter.of(packageName(packageFilter).and(resourceFilter::match));
	}

	@Override
	public Resolution resolve(ClasspathRootSelector selector, Context context) {
		return resourceSelectors(findAllResourcesInClasspathRoot(selector.getClasspathRoot(), resourceFilter));
	}

	@Override
	public Resolution resolve(ModuleSelector selector, Context context) {
		return resourceSelectors(findAllResourcesInModule(selector.getModuleName(), resourceFilter));
	}

	@Override
	public Resolution resolve(PackageSelector selector, Context context) {
		return resourceSelectors(findAllResourcesInPackage(selector.getPackageName(), resourceFilter));
	}

	private Resolution resourceSelectors(List<Resource> resources) {
		Set<ClasspathResourceSelector> selectors = resources.stream() //
				.collect(groupingBy(Resource::getName)) //
				.values() //
				.stream() //
				.map(LinkedHashSet::new) //
				.map(DiscoverySelectors::selectClasspathResourceByName) //
				.collect(toSet());

		if (selectors.isEmpty()) {
			return unresolved();
		}
		return selectors(selectors);
	}

}
