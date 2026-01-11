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

import static java.util.stream.Collectors.toCollection;
import static org.apiguardian.api.API.Status.DEPRECATED;
import static org.apiguardian.api.API.Status.EXPERIMENTAL;
import static org.apiguardian.api.API.Status.MAINTAINED;
import static org.apiguardian.api.API.Status.STABLE;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import java.util.function.Predicate;

import org.apiguardian.api.API;
import org.junit.platform.commons.io.ResourceFilter;
import org.junit.platform.commons.util.Preconditions;
import org.junit.platform.engine.DiscoveryFilter;
import org.junit.platform.engine.EngineDiscoveryRequest;
import org.junit.platform.engine.Filter;
import org.junit.platform.engine.TestDescriptor;
import org.junit.platform.engine.discovery.ClassNameFilter;
import org.junit.platform.engine.discovery.ClassSelector;
import org.junit.platform.engine.discovery.ClasspathResourceSelector;
import org.junit.platform.engine.discovery.ClasspathRootSelector;
import org.junit.platform.engine.discovery.ModuleSelector;
import org.junit.platform.engine.discovery.PackageNameFilter;
import org.junit.platform.engine.discovery.PackageSelector;
import org.junit.platform.engine.support.discovery.SelectorResolver.Match;
import org.junit.platform.engine.support.discovery.SelectorResolver.Resolution;

/**
 * Configurable test discovery implementation based on {@link SelectorResolver}
 * and {@link TestDescriptor.Visitor} that can be reused by different
 * {@link org.junit.platform.engine.TestEngine TestEngines}.
 *
 * <p>This resolver takes care of notifying registered
 * {@link org.junit.platform.engine.EngineDiscoveryListener
 * EngineDiscoveryListeners} about the results of processed
 * {@link org.junit.platform.engine.DiscoverySelector DiscoverySelectors}.
 *
 * @param <T> the type of the engine's descriptor
 * @since 1.5
 * @see #builder()
 * @see #resolve(EngineDiscoveryRequest, TestDescriptor)
 */
@API(status = STABLE, since = "1.10")
public class EngineDiscoveryRequestResolver<T extends TestDescriptor> {

	private final List<Function<InitializationContext<T>, SelectorResolver>> resolverCreators;
	private final List<Function<InitializationContext<T>, TestDescriptor.Visitor>> visitorCreators;

	private EngineDiscoveryRequestResolver(List<Function<InitializationContext<T>, SelectorResolver>> resolverCreators,
			List<Function<InitializationContext<T>, TestDescriptor.Visitor>> visitorCreators) {
		this.resolverCreators = new ArrayList<>(resolverCreators);
		this.visitorCreators = new ArrayList<>(visitorCreators);
	}

	/**
	 * Resolve the supplied {@link EngineDiscoveryRequest} and collect the
	 * results into the supplied {@link TestDescriptor} while forwarding
	 * encountered discovery issues to the {@link EngineDiscoveryRequest}'s
	 * {@link org.junit.platform.engine.EngineDiscoveryListener}.
	 *
	 * <p>The algorithm works as follows:
	 *
	 * <ol>
	 *     <li>Enqueue all selectors in the supplied
	 *     {@linkplain EngineDiscoveryRequest request} to be resolved.
	 *     </li>
	 *     <li>
	 *         While there are selectors to be resolved, get the next one.
	 *         Otherwise, the resolution is finished.
	 *         <ol>
	 *             <li>
	 *                 Iterate over all registered {@linkplain SelectorResolver
	 *                 resolvers} in the order they were registered in and find the
	 *                 first one that returns a {@linkplain Resolution resolution}
	 *                 other than {@link Resolution#unresolved() unresolved()}.
	 *             </li>
	 *             <li>
	 *                 If such a {@linkplain Resolution resolution} exists, enqueue
	 *                 its {@linkplain Resolution#getSelectors() selectors}.
	 *             </li>
	 *             <li>
	 *                 For each exact {@linkplain Match match} in the {@linkplain
	 *                 Resolution resolution}, {@linkplain Match#expand() expand}
	 *                 its children and enqueue them as well.
	 *             </li>
	 *         </ol>
	 *     </li>
	 *     <li>
	 *         Iterate over all registered {@linkplain TestDescriptor.Visitor
	 *         visitors} and let the engine test descriptor {@linkplain
	 *         TestDescriptor#accept(TestDescriptor.Visitor) accept} them.
	 *     </li>
	 * </ol>
	 *
	 * @param request the request to be resolved; never {@code null}
	 * @param engineDescriptor the engine's {@code TestDescriptor} to be used
	 * for adding direct children
	 * @see SelectorResolver
	 * @see TestDescriptor.Visitor
	 */
	public void resolve(EngineDiscoveryRequest request, T engineDescriptor) {
		Preconditions.notNull(request, "request must not be null");
		Preconditions.notNull(engineDescriptor, "engineDescriptor must not be null");
		DiscoveryIssueReporter issueReporter = DiscoveryIssueReporter.forwarding(request.getDiscoveryListener(),
			engineDescriptor.getUniqueId());
		resolve(request, engineDescriptor, issueReporter);
	}

	/**
	 * Resolve the supplied {@link EngineDiscoveryRequest} and collect the
	 * results into the supplied {@link TestDescriptor} using the supplied
	 * {@link DiscoveryIssueReporter} to report issues encountered during
	 * resolution.
	 *
	 * <p>The algorithm works as described in
	 * {@link #resolve(EngineDiscoveryRequest, TestDescriptor)}.
	 *
	 * @param request the request to be resolved; never {@code null}
	 * @param engineDescriptor the engine's {@code TestDescriptor} to be used
	 * for adding direct children
	 * @param issueReporter the {@link DiscoveryIssueReporter} to report issues
	 * encountered during resolution
	 * @since 1.13
	 * @see #resolve(EngineDiscoveryRequest, TestDescriptor)
	 * @see SelectorResolver
	 * @see TestDescriptor.Visitor
	 */
	@API(status = EXPERIMENTAL, since = "6.0")
	public void resolve(EngineDiscoveryRequest request, T engineDescriptor, DiscoveryIssueReporter issueReporter) {
		Preconditions.notNull(request, "request must not be null");
		Preconditions.notNull(engineDescriptor, "engineDescriptor must not be null");
		Preconditions.notNull(issueReporter, "issueReporter must not be null");
		InitializationContext<T> initializationContext = new DefaultInitializationContext<>(request, engineDescriptor,
			issueReporter);
		List<SelectorResolver> resolvers = instantiate(resolverCreators, initializationContext);
		List<TestDescriptor.Visitor> visitors = instantiate(visitorCreators, initializationContext);
		new EngineDiscoveryRequestResolution(request, engineDescriptor, resolvers, visitors).run();
	}

	private <R> List<R> instantiate(List<Function<InitializationContext<T>, R>> creators,
			InitializationContext<T> context) {
		return creators.stream().map(creator -> creator.apply(context)).collect(toCollection(ArrayList::new));
	}

	/**
	 * Create a new {@link Builder} for creating a {@link EngineDiscoveryRequestResolver}.
	 *
	 * @param <T> the type of the engine's descriptor
	 * @return a new builder; never {@code null}
	 */
	public static <T extends TestDescriptor> Builder<T> builder() {
		return new Builder<>();
	}

	/**
	 * Builder for {@link EngineDiscoveryRequestResolver}.
	 *
	 * @param <T> the type of the engine's descriptor
	 * @since 1.5
	 */
	@API(status = STABLE, since = "1.10")
	public static class Builder<T extends TestDescriptor> {

		private final List<Function<InitializationContext<T>, SelectorResolver>> resolverCreators = new ArrayList<>();
		private final List<Function<InitializationContext<T>, TestDescriptor.Visitor>> visitorCreators = new ArrayList<>();

		private Builder() {
		}

		/**
		 * Add a predefined resolver that resolves {@link ClasspathRootSelector
		 * ClasspathRootSelectors}, {@link ModuleSelector ModuleSelectors}, and
		 * {@link PackageSelector PackageSelectors} into {@link ClassSelector
		 * ClassSelectors} by scanning for classes that satisfy the supplied
		 * predicate in the respective class containers to this builder.
		 *
		 * @param classFilter predicate the resolved classes must satisfy; never
		 * {@code null}
		 * @return this builder for method chaining
		 */
		public Builder<T> addClassContainerSelectorResolver(Predicate<Class<?>> classFilter) {
			Preconditions.notNull(classFilter, "classFilter must not be null");
			return addClassContainerSelectorResolverWithContext(__ -> classFilter);
		}

		/**
		 * Add a predefined resolver that resolves {@link ClasspathRootSelector
		 * ClasspathRootSelectors}, {@link ModuleSelector ModuleSelectors}, and
		 * {@link PackageSelector PackageSelectors} into {@link ClassSelector
		 * ClassSelectors} by scanning for classes that satisfy the predicate
		 * created by the supplied {@code Function} in the respective class
		 * containers to this builder.
		 *
		 * @param classFilterCreator the function that will be called to create
		 * the predicate the resolved classes must satisfy; never
		 * {@code null}
		 * @return this builder for method chaining
		 */
		@API(status = EXPERIMENTAL, since = "6.0")
		public Builder<T> addClassContainerSelectorResolverWithContext(
				Function<InitializationContext<T>, Predicate<Class<?>>> classFilterCreator) {
			Preconditions.notNull(classFilterCreator, "classFilterCreator must not be null");
			return addSelectorResolver(context -> new ClassContainerSelectorResolver(classFilterCreator.apply(context),
				context.getClassNameFilter()));
		}

		/**
		 * Add a predefined resolver that resolves {@link ClasspathRootSelector
		 * ClasspathRootSelectors}, {@link ModuleSelector ModuleSelectors}, and
		 * {@link PackageSelector PackageSelectors} into {@link ClasspathResourceSelector
		 * ClasspathResourceSelectors} by scanning for resources that satisfy the supplied
		 * predicate in the respective class containers to this builder.
		 *
		 * @param resourceFilter predicate the resolved classes must satisfy; never
		 * {@code null}
		 * @return this builder for method chaining
		 * @since 1.12
		 * @deprecated Please use {@link #addResourceContainerSelectorResolver(ResourceFilter)} instead.
		 */
		@API(status = DEPRECATED, since = "1.14")
		@Deprecated(since = "1.14", forRemoval = true)
		@SuppressWarnings("removal")
		public Builder<T> addResourceContainerSelectorResolver(
				Predicate<org.junit.platform.commons.support.Resource> resourceFilter) {
			Preconditions.notNull(resourceFilter, "resourceFilter must not be null");
			return addResourceContainerSelectorResolver(
				ResourceFilter.of(r -> resourceFilter.test(org.junit.platform.commons.support.Resource.of(r))));
		}

		/**
		 * Add a predefined resolver that resolves {@link ClasspathRootSelector
		 * ClasspathRootSelectors}, {@link ModuleSelector ModuleSelectors}, and
		 * {@link PackageSelector PackageSelectors} into
		 * {@link ClasspathResourceSelector ClasspathResourceSelectors} by
		 * scanning for resources that match the supplied resource filter in the
		 * respective class containers to this builder.
		 *
		 * @param resourceFilter filter the resolved classes must match; never
		 * {@code null}
		 * @return this builder for method chaining
		 * @since 1.14
		 */
		@API(status = MAINTAINED, since = "1.14")
		public Builder<T> addResourceContainerSelectorResolver(ResourceFilter resourceFilter) {
			Preconditions.notNull(resourceFilter, "resourceFilter must not be null");
			return addSelectorResolver(
				context -> new ResourceContainerSelectorResolver(resourceFilter, context.getPackageFilter()));
		}

		/**
		 * Add a context insensitive {@link SelectorResolver} to this builder.
		 *
		 * @param resolver the resolver to add; never {@code null}
		 * @return this builder for method chaining
		 */
		public Builder<T> addSelectorResolver(SelectorResolver resolver) {
			Preconditions.notNull(resolver, "resolver must not be null");
			return addSelectorResolver(context -> resolver);
		}

		/**
		 * Add a context sensitive {@link SelectorResolver} to this builder.
		 *
		 * @param resolverCreator the function that will be called to create the
		 * {@link SelectorResolver} to be added.
		 * @return this builder for method chaining
		 * @see InitializationContext
		 */
		public Builder<T> addSelectorResolver(Function<InitializationContext<T>, SelectorResolver> resolverCreator) {
			resolverCreators.add(resolverCreator);
			return this;
		}

		/**
		 * Add a context sensitive {@link TestDescriptor.Visitor} to this
		 * builder.
		 *
		 * <p>If multiple {@linkplain TestDescriptor.Visitor visitors} are registered,
		 * they will iterate over the test tree separately. To avoid the overhead of
		 * multiple iterations, consider combining multiple visitors into a single
		 * visitor using
		 * {@link TestDescriptor.Visitor#composite(TestDescriptor.Visitor...)}.
		 *
		 * @param visitorCreator the function that will be called to create the
		 * {@link TestDescriptor.Visitor} to be added.
		 * @return this builder for method chaining
		 * @see InitializationContext
		 */
		public Builder<T> addTestDescriptorVisitor(
				Function<InitializationContext<T>, TestDescriptor.Visitor> visitorCreator) {
			visitorCreators.add(visitorCreator);
			return this;
		}

		/**
		 * Build the {@link EngineDiscoveryRequestResolver} that has been
		 * configured via this builder.
		 */
		public EngineDiscoveryRequestResolver<T> build() {
			return new EngineDiscoveryRequestResolver<>(resolverCreators, visitorCreators);
		}

	}

	/**
	 * The initialization context for creating {@linkplain SelectorResolver
	 * resolvers} and {@linkplain TestDescriptor.Visitor visitors} that depend
	 * on the {@link EngineDiscoveryRequest} to be resolved or the engine
	 * descriptor that will be used to collect the results.
	 *
	 * @since 1.5
	 * @see Builder#addSelectorResolver(Function)
	 * @see Builder#addTestDescriptorVisitor(Function)
	 */
	@API(status = STABLE, since = "1.10")
	public interface InitializationContext<T extends TestDescriptor> {

		/**
		 * Get the {@link EngineDiscoveryRequest} that is about to be resolved.
		 *
		 * @return the {@link EngineDiscoveryRequest}; never {@code null}
		 */
		EngineDiscoveryRequest getDiscoveryRequest();

		/**
		 * Get the engine's {@link TestDescriptor} that will be used to collect
		 * the results.
		 *
		 * @return engine's {@link TestDescriptor}; never {@code null}
		 */
		T getEngineDescriptor();

		/**
		 * Get the class name filter built from the {@link ClassNameFilter
		 * ClassNameFilters} and {@link PackageNameFilter PackageNameFilters}
		 * in the {@link EngineDiscoveryRequest} that is about to be resolved.
		 *
		 * @return the predicate for filtering the resolved class names; never
		 * {@code null}
		 */
		Predicate<String> getClassNameFilter();

		/**
		 * Get the package name filter built from the {@link PackageNameFilter
		 * PackageNameFilters} in the {@link EngineDiscoveryRequest} that is
		 * about to be resolved.
		 *
		 * @return the predicate for filtering the resolved resource names; never
		 * {@code null}
		 * @since 1.12
		 */
		@API(status = MAINTAINED, since = "1.13.3")
		Predicate<String> getPackageFilter();

		/**
		 * {@return the {@link DiscoveryIssueReporter} for the current
		 * resolution}
		 *
		 * @since 1.13
		 */
		@API(status = EXPERIMENTAL, since = "6.0")
		DiscoveryIssueReporter getIssueReporter();
	}

	private static class DefaultInitializationContext<T extends TestDescriptor> implements InitializationContext<T> {

		private final EngineDiscoveryRequest request;
		private final T engineDescriptor;
		private final Predicate<String> classNameFilter;
		private final Predicate<String> packageFilter;
		private final DiscoveryIssueReporter issueReporter;

		DefaultInitializationContext(EngineDiscoveryRequest request, T engineDescriptor,
				DiscoveryIssueReporter issueReporter) {
			this.request = request;
			this.engineDescriptor = engineDescriptor;
			this.classNameFilter = buildClassNamePredicate(request);
			this.packageFilter = buildPackagePredicate(request);
			this.issueReporter = issueReporter;
		}

		/**
		 * Build a {@link Predicate} for fully qualified class names to be used for
		 * classpath scanning from an {@link EngineDiscoveryRequest}.
		 *
		 * @param request the request to build a predicate from
		 */
		private Predicate<String> buildClassNamePredicate(EngineDiscoveryRequest request) {
			List<DiscoveryFilter<String>> filters = new ArrayList<>();
			filters.addAll(request.getFiltersByType(ClassNameFilter.class));
			filters.addAll(request.getFiltersByType(PackageNameFilter.class));
			return Filter.composeFilters(filters).toPredicate();
		}

		private Predicate<String> buildPackagePredicate(EngineDiscoveryRequest request) {
			List<DiscoveryFilter<String>> filters = new ArrayList<>();
			filters.addAll(request.getFiltersByType(PackageNameFilter.class));
			return Filter.composeFilters(filters).toPredicate();
		}

		@Override
		public EngineDiscoveryRequest getDiscoveryRequest() {
			return request;
		}

		@Override
		public T getEngineDescriptor() {
			return engineDescriptor;
		}

		@Override
		public Predicate<String> getClassNameFilter() {
			return classNameFilter;
		}

		@Override
		public Predicate<String> getPackageFilter() {
			return packageFilter;
		}

		@Override
		public DiscoveryIssueReporter getIssueReporter() {
			return issueReporter;
		}
	}

}
