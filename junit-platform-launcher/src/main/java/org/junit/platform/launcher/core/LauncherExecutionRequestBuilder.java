/*
 * Copyright 2015-2025 the original author or authors.
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v2.0 which
 * accompanies this distribution and is available at
 *
 * https://www.eclipse.org/legal/epl-v20.html
 */

package org.junit.platform.launcher.core;

import static org.apiguardian.api.API.Status.MAINTAINED;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;

import org.apiguardian.api.API;
import org.jspecify.annotations.Nullable;
import org.junit.platform.commons.util.Preconditions;
import org.junit.platform.launcher.LauncherDiscoveryRequest;
import org.junit.platform.launcher.LauncherExecutionRequest;
import org.junit.platform.launcher.TestExecutionListener;
import org.junit.platform.launcher.TestPlan;

/**
 * The {@code LauncherExecutionRequestBuilder} provides a light-weight DSL for
 * generating a {@link LauncherExecutionRequest}.
 *
 * @since 6.0
 * @see LauncherExecutionRequest
 */
@API(status = MAINTAINED, since = "6.0")
public final class LauncherExecutionRequestBuilder {

	/**
	 * Create a new {@code LauncherExecutionRequestBuilder} from the supplied
	 * {@link LauncherDiscoveryRequest}.
	 *
	 * @return a new builder
	 */
	public static LauncherExecutionRequestBuilder request(LauncherDiscoveryRequest discoveryRequest) {
		Preconditions.notNull(discoveryRequest, "LauncherDiscoveryRequest must not be null");
		return new LauncherExecutionRequestBuilder(discoveryRequest, null);
	}

	/**
	 * Create a new {@code LauncherExecutionRequestBuilder} from the supplied
	 * {@link TestPlan}.
	 *
	 * @return a new builder
	 */
	public static LauncherExecutionRequestBuilder request(TestPlan testPlan) {
		Preconditions.notNull(testPlan, "TestPlan must not be null");
		return new LauncherExecutionRequestBuilder(null, testPlan);
	}

	private final @Nullable LauncherDiscoveryRequest discoveryRequest;
	private final @Nullable TestPlan testPlan;
	private final Collection<TestExecutionListener> executionListeners = new ArrayList<>();

	private LauncherExecutionRequestBuilder(@Nullable LauncherDiscoveryRequest discoveryRequest,
			@Nullable TestPlan testPlan) {

		this.discoveryRequest = discoveryRequest;
		this.testPlan = testPlan;
	}

	/**
	 * Add all supplied execution listeners to the request.
	 *
	 * @param listeners the {@code TestExecutionListener} to add; never
	 * {@code null}
	 * @return this builder for method chaining
	 * @see TestExecutionListener
	 */
	public LauncherExecutionRequestBuilder listeners(TestExecutionListener... listeners) {
		Preconditions.notNull(listeners, "TestExecutionListener array must not be null");
		Preconditions.containsNoNullElements(listeners, "individual listeners must not be null");
		Collections.addAll(this.executionListeners, listeners);
		return this;
	}

	/**
	 * Build the {@link LauncherExecutionRequest} that has been configured via
	 * this builder.
	 */
	public LauncherExecutionRequest build() {
		return new DefaultLauncherExecutionRequest(discoveryRequest, testPlan, executionListeners);
	}

}
