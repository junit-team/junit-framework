/*
 * Copyright 2015-2026 the original author or authors.
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v2.0 which
 * accompanies this distribution and is available at
 *
 * https://www.eclipse.org/legal/epl-v20.html
 */

package org.junit.platform.launcher.core;

import static org.apiguardian.api.API.Status.DEPRECATED;
import static org.apiguardian.api.API.Status.EXPERIMENTAL;
import static org.apiguardian.api.API.Status.MAINTAINED;
import static org.apiguardian.api.API.Status.STABLE;
import static org.junit.platform.launcher.LauncherConstants.OUTPUT_DIR_PROPERTY_NAME;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apiguardian.api.API;
import org.jspecify.annotations.Nullable;
import org.junit.platform.commons.PreconditionViolationException;
import org.junit.platform.commons.util.Preconditions;
import org.junit.platform.engine.ConfigurationParameters;
import org.junit.platform.engine.DiscoveryFilter;
import org.junit.platform.engine.DiscoverySelector;
import org.junit.platform.engine.Filter;
import org.junit.platform.engine.OutputDirectoryCreator;
import org.junit.platform.launcher.EngineFilter;
import org.junit.platform.launcher.LauncherConstants;
import org.junit.platform.launcher.LauncherDiscoveryListener;
import org.junit.platform.launcher.LauncherDiscoveryRequest;
import org.junit.platform.launcher.PostDiscoveryFilter;
import org.junit.platform.launcher.core.LauncherConfigurationParameters.Builder;
import org.junit.platform.launcher.listeners.OutputDir;
import org.junit.platform.launcher.listeners.discovery.LauncherDiscoveryListeners;

/**
 * The {@code LauncherDiscoveryRequestBuilder} provides a light-weight DSL for
 * generating a {@link LauncherDiscoveryRequest}.
 *
 * <h2>Example</h2>
 *
 * <pre class="code">
 * import static org.junit.platform.engine.discovery.DiscoverySelectors.*;
 * import static org.junit.platform.engine.discovery.ClassNameFilter.*;
 * import static org.junit.platform.launcher.EngineFilter.*;
 * import static org.junit.platform.launcher.TagFilter.*;
 * import static org.junit.platform.launcher.core.LauncherDiscoveryRequestBuilder.discoveryRequest;
 *
 * // ...
 *
 * LauncherDiscoveryRequest discoveryRequest = discoveryRequest()
 *    .selectors(
 *        selectPackage("org.example.user"),
 *        selectClass("org.example.payment.PaymentTests"),
 *        selectClass(ShippingTests.class),
 *        selectMethod("org.example.order.OrderTests#test1"),
 *        selectMethod("org.example.order.OrderTests#test2()"),
 *        selectMethod("org.example.order.OrderTests#test3(java.lang.String)"),
 *        selectMethod("org.example.order.OrderTests", "test4"),
 *        selectMethod(OrderTests.class, "test5"),
 *        selectMethod(OrderTests.class, testMethod),
 *        selectUniqueId("unique-id-1"),
 *        selectUniqueId("unique-id-2")
 *    )
 *    .selectors(
 *        selectClasspathRoots(Set.of(Paths.get("/my/local/path1")))
 *    )
 *   .filters(
 *        includeEngines("junit-jupiter", "spek"),
 *        // excludeEngines("junit-vintage"),
 *        includeTags("fast"),
 *        // excludeTags("slow"),
 *        includeClassNamePatterns(".*Test[s]?")
 *        // includeClassNamePatterns("org\.example\.tests.*")
 *    )
 *    .configurationParameter("key1", "value1")
 *    .configurationParameters(configParameterMap)
 *    .build();</pre>
 *
 * @since 1.0
 * @see org.junit.platform.engine.discovery.DiscoverySelectors
 * @see org.junit.platform.engine.discovery.ClassNameFilter
 * @see org.junit.platform.launcher.EngineFilter
 * @see org.junit.platform.launcher.TagFilter
 * @see LauncherExecutionRequestBuilder
 */
@API(status = STABLE, since = "1.0")
public final class LauncherDiscoveryRequestBuilder {

	/**
	 * Property name used to set the default discovery listener that is added to all : {@value}
	 *
	 * <h4>Supported Values</h4>
	 *
	 * <p>Supported values are {@code "logging"} and {@code "abortOnFailure"}.
	 *
	 * <p>If not specified, the default is {@value #DEFAULT_DISCOVERY_LISTENER_CONFIGURATION_PROPERTY_VALUE}.
	 */
	public static final String DEFAULT_DISCOVERY_LISTENER_CONFIGURATION_PROPERTY_NAME = "junit.platform.discovery.listener.default";

	private static final String DEFAULT_DISCOVERY_LISTENER_CONFIGURATION_PROPERTY_VALUE = "abortOnFailure";

	private final List<DiscoverySelector> selectors = new ArrayList<>();
	private final List<EngineFilter> engineFilters = new ArrayList<>();
	private final List<DiscoveryFilter<?>> discoveryFilters = new ArrayList<>();
	private final List<PostDiscoveryFilter> postDiscoveryFilters = new ArrayList<>();
	private final Map<String, String> configurationParameters = new HashMap<>();
	private final List<String> configurationParametersResources = new ArrayList<>();
	private final List<LauncherDiscoveryListener> discoveryListeners = new ArrayList<>();
	private boolean implicitConfigurationParametersEnabled = true;

	private @Nullable ConfigurationParameters parentConfigurationParameters;

	private @Nullable OutputDirectoryCreator outputDirectoryCreator;

	/**
	 * Create a new {@code LauncherDiscoveryRequestBuilder}.
	 *
	 * @return a new builder
	 * @see #discoveryRequest()
	 */
	public static LauncherDiscoveryRequestBuilder request() {
		return new LauncherDiscoveryRequestBuilder();
	}

	/**
	 * Create a new {@code LauncherDiscoveryRequestBuilder}.
	 *
	 * <p>This method is an <em>alias</em> for {@link #request()} and is intended
	 * to be used when statically imported &mdash; for example, via:
	 * {@code import static org.junit.platform.launcher.core.LauncherDiscoveryRequestBuilder.discoveryRequest;}
	 *
	 * @return a new builder
	 * @since 6.0
	 * @see #request()
	 */
	@API(status = STABLE, since = "6.0")
	public static LauncherDiscoveryRequestBuilder discoveryRequest() {
		return request();
	}

	private LauncherDiscoveryRequestBuilder() {
	}

	/**
	 * Add all supplied {@code selectors} to the request.
	 *
	 * @param selectors the {@code DiscoverySelectors} to add; never {@code null}
	 * @return this builder for method chaining
	 */
	public LauncherDiscoveryRequestBuilder selectors(DiscoverySelector... selectors) {
		Preconditions.notNull(selectors, "selectors array must not be null");
		selectors(Arrays.asList(selectors));
		return this;
	}

	/**
	 * Add all supplied {@code selectors} to the request.
	 *
	 * @param selectors the {@code DiscoverySelectors} to add; never {@code null}
	 * @return this builder for method chaining
	 */
	public LauncherDiscoveryRequestBuilder selectors(List<? extends DiscoverySelector> selectors) {
		Preconditions.notNull(selectors, "selectors list must not be null");
		Preconditions.containsNoNullElements(selectors, "individual selectors must not be null");
		this.selectors.addAll(selectors);
		return this;
	}

	/**
	 * Add all supplied {@code filters} to the request.
	 *
	 * <p>The {@code filters} are combined using AND semantics, i.e. all of them
	 * have to include a resource for it to end up in the test plan.
	 *
	 * <p><strong>Warning</strong>: be cautious when registering multiple competing
	 * {@link EngineFilter#includeEngines include} {@code EngineFilters} or multiple
	 * competing {@link EngineFilter#excludeEngines exclude} {@code EngineFilters}
	 * for the same discovery request since doing so will likely lead to
	 * undesirable results (i.e., zero engines being active).
	 *
	 * @param filters the {@code Filter}s to add; never {@code null}
	 * @return this builder for method chaining
	 */
	public LauncherDiscoveryRequestBuilder filters(Filter<?>... filters) {
		Preconditions.notNull(filters, "filters array must not be null");
		Preconditions.containsNoNullElements(filters, "individual filters must not be null");
		Arrays.stream(filters).forEach(this::storeFilter);
		return this;
	}

	/**
	 * Add the supplied <em>configuration parameter</em> to the request.
	 *
	 * @param key the configuration parameter key under which to store the
	 * value; never {@code null} or blank
	 * @param value the value to store
	 * @return this builder for method chaining
	 */
	public LauncherDiscoveryRequestBuilder configurationParameter(String key, String value) {
		Preconditions.notBlank(key, "configuration parameter key must not be null or blank");
		this.configurationParameters.put(key, value);
		return this;
	}

	/**
	 * Add all supplied configuration parameters to the request.
	 *
	 * @param configurationParameters the map of configuration parameters to add;
	 * never {@code null}
	 * @return this builder for method chaining
	 * @see #configurationParameter(String, String)
	 */
	public LauncherDiscoveryRequestBuilder configurationParameters(Map<String, String> configurationParameters) {
		Preconditions.notNull(configurationParameters, "configuration parameters map must not be null");
		configurationParameters.forEach(this::configurationParameter);
		return this;
	}

	/**
	 * Add all of the supplied configuration parameters resource files to the request.
	 * @param paths the classpath locations of the properties files
	 * never {@code null}
	 * @return this builder for method chaining
	 */
	public LauncherDiscoveryRequestBuilder configurationParametersResources(String... paths) {
		Preconditions.notNull(paths, "property file paths must not be null");
		Collections.addAll(configurationParametersResources, paths);
		return this;
	}

	/**
	 * Set the parent configuration parameters to use for the request.
	 *
	 * <p>Any explicit configuration parameters configured via
	 * {@link #configurationParameter(String, String)} or
	 * {@link #configurationParameters(Map)} takes precedence over the supplied
	 * configuration parameters.
	 *
	 * @param parentConfigurationParameters the parent instance to use for looking
	 * up configuration parameters that have not been explicitly configured;
	 * never {@code null}
	 * @return this builder for method chaining
	 * @since 1.8
	 * @see #configurationParameter(String, String)
	 * @see #configurationParameters(Map)
	 */
	@API(status = STABLE, since = "1.10")
	public LauncherDiscoveryRequestBuilder parentConfigurationParameters(
			ConfigurationParameters parentConfigurationParameters) {
		Preconditions.notNull(parentConfigurationParameters, "parent configuration parameters must not be null");
		this.parentConfigurationParameters = parentConfigurationParameters;
		return this;
	}

	/**
	 * Configure whether implicit configuration parameters should be considered.
	 *
	 * <p>By default, in addition to those parameters that are passed explicitly
	 * to this builder, configuration parameters are read from system properties
	 * and from the {@code junit-platform.properties} classpath resource.
	 * Passing {@code false} to this method, disables the latter two sources so
	 * that only explicit configuration parameters are taken into account.
	 *
	 * @param enabled {@code true} if implicit configuration parameters should be
	 * considered
	 * @return this builder for method chaining
	 * @since 1.7
	 * @see #configurationParameter(String, String)
	 * @see #configurationParameters(Map)
	 */
	@API(status = STABLE, since = "1.10")
	public LauncherDiscoveryRequestBuilder enableImplicitConfigurationParameters(boolean enabled) {
		this.implicitConfigurationParametersEnabled = enabled;
		return this;
	}

	/**
	 * Add all supplied discovery listeners to the request.
	 *
	 * <p>In addition to the {@linkplain LauncherDiscoveryListener listeners}
	 * registered using this method, this builder will add a default listener
	 * to this request that can be specified using the
	 * {@value #DEFAULT_DISCOVERY_LISTENER_CONFIGURATION_PROPERTY_NAME}
	 * configuration parameter.
	 *
	 * @param listeners the {@code LauncherDiscoveryListeners} to add; never
	 * {@code null}
	 * @return this builder for method chaining
	 * @since 1.6
	 * @see LauncherDiscoveryListener
	 * @see LauncherDiscoveryListeners
	 * @see LauncherDiscoveryRequestBuilder#DEFAULT_DISCOVERY_LISTENER_CONFIGURATION_PROPERTY_NAME
	 */
	@API(status = STABLE, since = "1.10")
	public LauncherDiscoveryRequestBuilder listeners(LauncherDiscoveryListener... listeners) {
		Preconditions.notNull(listeners, "discovery listener array must not be null");
		Preconditions.containsNoNullElements(listeners, "individual discovery listeners must not be null");
		this.discoveryListeners.addAll(Arrays.asList(listeners));
		return this;
	}

	/**
	 * Set the
	 * {@link org.junit.platform.engine.reporting.OutputDirectoryProvider} to use for the request.
	 *
	 * <p>If not specified, a default provider will be used that can be
	 * configured via the {@value LauncherConstants#OUTPUT_DIR_PROPERTY_NAME}
	 * configuration parameter.
	 *
	 * @param outputDirectoryProvider the output directory provider to use;
	 *                                never {@code null}
	 * @return this builder for method chaining
	 * @since 1.12
	 * @see org.junit.platform.engine.reporting.OutputDirectoryProvider
	 * @see LauncherConstants#OUTPUT_DIR_PROPERTY_NAME
	 * @deprecated Please use
	 * {@link #outputDirectoryCreator(OutputDirectoryCreator)} instead
	 */
	@SuppressWarnings("removal")
	@API(status = DEPRECATED, since = "1.14")
	@Deprecated(since = "1.14", forRemoval = true)
	public LauncherDiscoveryRequestBuilder outputDirectoryProvider(
			org.junit.platform.engine.reporting.OutputDirectoryProvider outputDirectoryProvider) {
		return outputDirectoryCreator(outputDirectoryProvider);
	}

	/**
	 * Set the {@link OutputDirectoryCreator} to use for the request.
	 *
	 * <p>If not specified, a default implementation will be used that can be
	 * configured via the {@value LauncherConstants#OUTPUT_DIR_PROPERTY_NAME}
	 * configuration parameter.
	 *
	 * @param outputDirectoryCreator the output directory creator to use;
	 *                                never {@code null}
	 * @return this builder for method chaining
	 * @since 1.14
	 * @see OutputDirectoryCreator
	 * @see LauncherConstants#OUTPUT_DIR_PROPERTY_NAME
	 */
	@SuppressWarnings("removal")
	@API(status = MAINTAINED, since = "1.14")
	public LauncherDiscoveryRequestBuilder outputDirectoryCreator(OutputDirectoryCreator outputDirectoryCreator) {
		this.outputDirectoryCreator = Preconditions.notNull(outputDirectoryCreator,
			"outputDirectoryCreator must not be null");
		return this;
	}

	private void storeFilter(Filter<?> filter) {
		if (filter instanceof EngineFilter engineFilter) {
			this.engineFilters.add(engineFilter);
		}
		else if (filter instanceof PostDiscoveryFilter postDiscoveryFilter) {
			this.postDiscoveryFilters.add(postDiscoveryFilter);
		}
		else if (filter instanceof DiscoveryFilter<?> discoveryFilter) {
			this.discoveryFilters.add(discoveryFilter);
		}
		else {
			throw new PreconditionViolationException(
				"Filter [%s] must implement %s, %s, or %s.".formatted(filter, EngineFilter.class.getSimpleName(),
					PostDiscoveryFilter.class.getSimpleName(), DiscoveryFilter.class.getSimpleName()));
		}
	}

	/**
	 * Builds this discovery request and returns a new builder for creating a
	 * {@link org.junit.platform.launcher.LauncherExecutionRequest} that is
	 * initialized to contain the resulting discovery request.
	 *
	 * @return a new {@link LauncherExecutionRequestBuilder}
	 * @since 6.0
	 */
	@API(status = EXPERIMENTAL, since = "6.0")
	public LauncherExecutionRequestBuilder forExecution() {
		return LauncherExecutionRequestBuilder.request(build());
	}

	/**
	 * Build the {@link LauncherDiscoveryRequest} that has been configured via
	 * this builder.
	 */
	public LauncherDiscoveryRequest build() {
		LauncherConfigurationParameters launcherConfigurationParameters = buildLauncherConfigurationParameters();
		LauncherDiscoveryListener discoveryListener = getLauncherDiscoveryListener(launcherConfigurationParameters);
		OutputDirectoryCreator outputDirectoryCreator = getOutputDirectoryCreator(launcherConfigurationParameters);
		return new DefaultDiscoveryRequest(this.selectors, this.engineFilters, this.discoveryFilters,
			this.postDiscoveryFilters, launcherConfigurationParameters, discoveryListener, outputDirectoryCreator);
	}

	private OutputDirectoryCreator getOutputDirectoryCreator(LauncherConfigurationParameters configurationParameters) {
		if (this.outputDirectoryCreator != null) {
			return this.outputDirectoryCreator;
		}
		return new HierarchicalOutputDirectoryCreator(
			() -> OutputDir.create(configurationParameters.get(OUTPUT_DIR_PROPERTY_NAME)).toPath());
	}

	private LauncherConfigurationParameters buildLauncherConfigurationParameters() {
		Builder builder = LauncherConfigurationParameters.builder() //
				.explicitParameters(this.configurationParameters) //
				.configurationResources(this.configurationParametersResources) //
				.enableImplicitProviders(this.implicitConfigurationParametersEnabled);

		if (this.parentConfigurationParameters != null) {
			builder.parentConfigurationParameters(this.parentConfigurationParameters);
		}

		return builder.build();
	}

	private LauncherDiscoveryListener getLauncherDiscoveryListener(ConfigurationParameters configurationParameters) {
		LauncherDiscoveryListener defaultDiscoveryListener = getDefaultLauncherDiscoveryListener(
			configurationParameters);
		if (this.discoveryListeners.isEmpty()) {
			return defaultDiscoveryListener;
		}
		if (this.discoveryListeners.contains(defaultDiscoveryListener)) {
			return LauncherDiscoveryListeners.composite(this.discoveryListeners);
		}
		List<LauncherDiscoveryListener> allDiscoveryListeners = new ArrayList<>(this.discoveryListeners.size() + 1);
		allDiscoveryListeners.addAll(this.discoveryListeners);
		allDiscoveryListeners.add(defaultDiscoveryListener);
		return LauncherDiscoveryListeners.composite(allDiscoveryListeners);
	}

	private LauncherDiscoveryListener getDefaultLauncherDiscoveryListener(
			ConfigurationParameters configurationParameters) {
		String value = configurationParameters.get(DEFAULT_DISCOVERY_LISTENER_CONFIGURATION_PROPERTY_NAME) //
				.orElse(DEFAULT_DISCOVERY_LISTENER_CONFIGURATION_PROPERTY_VALUE);
		return LauncherDiscoveryListeners.fromConfigurationParameter(
			DEFAULT_DISCOVERY_LISTENER_CONFIGURATION_PROPERTY_NAME, value);
	}

}
