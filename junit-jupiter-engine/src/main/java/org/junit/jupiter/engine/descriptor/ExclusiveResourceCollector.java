/*
 * Copyright 2015-2026 the original author or authors.
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v2.0 which
 * accompanies this distribution and is available at
 *
 * https://www.eclipse.org/legal/epl-v20.html
 */

package org.junit.jupiter.engine.descriptor;

import static org.junit.jupiter.api.parallel.ResourceLockTarget.SELF;
import static org.junit.platform.commons.support.AnnotationSupport.findRepeatableAnnotations;

import java.lang.reflect.AnnotatedElement;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Stream;

import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.parallel.ResourceAccessMode;
import org.junit.jupiter.api.parallel.ResourceLock;
import org.junit.jupiter.api.parallel.ResourceLockTarget;
import org.junit.jupiter.api.parallel.ResourceLocksProvider;
import org.junit.platform.commons.util.ReflectionUtils;
import org.junit.platform.commons.util.StringUtils;
import org.junit.platform.engine.support.hierarchical.ExclusiveResource;

/**
 * @since 5.12
 */
abstract class ExclusiveResourceCollector {

	private static final ExclusiveResourceCollector NO_EXCLUSIVE_RESOURCES = new ExclusiveResourceCollector() {

		@Override
		Stream<ExclusiveResource> getAllExclusiveResources(
				Function<ResourceLocksProvider, Set<ResourceLocksProvider.Lock>> providerToLocks) {
			return Stream.empty();
		}

		@Override
		Stream<ExclusiveResource> getStaticResourcesFor(ResourceLockTarget target) {
			return Stream.empty();
		}

		@Override
		Stream<ExclusiveResource> getDynamicResources(
				Function<ResourceLocksProvider, Set<ResourceLocksProvider.Lock>> providerToLocks) {
			return Stream.empty();
		}
	};

	Stream<ExclusiveResource> getAllExclusiveResources(
			Function<ResourceLocksProvider, Set<ResourceLocksProvider.Lock>> providerToLocks) {
		return Stream.concat(getStaticResourcesFor(SELF), getDynamicResources(providerToLocks));
	}

	abstract Stream<ExclusiveResource> getStaticResourcesFor(ResourceLockTarget target);

	abstract Stream<ExclusiveResource> getDynamicResources(
			Function<ResourceLocksProvider, Set<ResourceLocksProvider.Lock>> providerToLocks);

	static ExclusiveResourceCollector from(AnnotatedElement element) {
		List<ResourceLock> annotations = findRepeatableAnnotations(element, ResourceLock.class);
		return annotations.isEmpty() ? NO_EXCLUSIVE_RESOURCES : new DefaultExclusiveResourceCollector(annotations);
	}

	private static class DefaultExclusiveResourceCollector extends ExclusiveResourceCollector {

		private final List<ResourceLock> annotations;

		@Nullable
		private List<ResourceLocksProvider> providers;

		DefaultExclusiveResourceCollector(List<ResourceLock> annotations) {
			this.annotations = annotations;
		}

		@Override
		Stream<ExclusiveResource> getStaticResourcesFor(ResourceLockTarget target) {
			return annotations.stream() //
					.filter(annotation -> StringUtils.isNotBlank(annotation.value())) //
					.filter(annotation -> annotation.target() == target) //
					.map(annotation -> new ExclusiveResource(annotation.value(), toLockMode(annotation.mode())));
		}

		@Override
		Stream<ExclusiveResource> getDynamicResources(
				Function<ResourceLocksProvider, Set<ResourceLocksProvider.Lock>> providerToLocks) {
			List<ResourceLocksProvider> providers = getProviders();
			if (providers.isEmpty()) {
				return Stream.empty();
			}
			return providers.stream() //
					.map(providerToLocks) //
					.flatMap(Collection::stream) //
					.map(lock -> new ExclusiveResource(lock.getKey(), toLockMode(lock.getAccessMode())));
		}

		private List<ResourceLocksProvider> getProviders() {
			if (this.providers == null) {
				this.providers = annotations.stream() //
						.flatMap(annotation -> instantiate(annotation.providers())) //
						.toList();
			}
			return providers;
		}

		private static Stream<ResourceLocksProvider> instantiate(Class<? extends ResourceLocksProvider>[] providers) {
			return Stream.of(providers).map(ReflectionUtils::newInstance);
		}

		private static ExclusiveResource.LockMode toLockMode(ResourceAccessMode mode) {
			return switch (mode) {
				case READ -> ExclusiveResource.LockMode.READ;
				case READ_WRITE -> ExclusiveResource.LockMode.READ_WRITE;
			};
		}
	}
}
