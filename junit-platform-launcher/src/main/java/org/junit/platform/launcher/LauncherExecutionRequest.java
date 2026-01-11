/*
 * Copyright 2015-2026 the original author or authors.
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v2.0 which
 * accompanies this distribution and is available at
 *
 * https://www.eclipse.org/legal/epl-v20.html
 */

package org.junit.platform.launcher;

import static org.apiguardian.api.API.Status.MAINTAINED;

import java.util.Collection;
import java.util.Optional;

import org.apiguardian.api.API;
import org.junit.platform.engine.CancellationToken;

/**
 * {@code LauncherExecutionRequest} encapsulates a request for test execution
 * passed to the {@link Launcher}.
 *
 * <p>Most importantly, a {@code LauncherExecutionRequest} contains either a
 * {@link LauncherDiscoveryRequest} for on-the-fly test discovery or a
 * {@link TestPlan} that has previously been discovered.
 *
 * <p>Moreover, a {@code LauncherExecutionRequest} may contain the following:
 *
 * <ul>
 * <li>Additional {@linkplain TestExecutionListener Test Execution Listeners}
 * that should be notified of events pertaining to this execution request.</li>
 * </ul>
 *
 * <p>This interface is not intended to be implemented by clients.
 *
 * @since 6.0
 * @see org.junit.platform.launcher.core.LauncherExecutionRequestBuilder
 * @see Launcher#execute(LauncherExecutionRequest)
 */
@API(status = MAINTAINED, since = "6.0")
public interface LauncherExecutionRequest {

	/**
	 * {@return the test plan for this execution request}
	 *
	 * <p>If absent, a {@link TestPlan} will be present.
	 */
	Optional<TestPlan> getTestPlan();

	/**
	 * {@return the discovery request for this execution request}
	 *
	 * <p>If absent, a {@link TestPlan} will be present.
	 */
	Optional<LauncherDiscoveryRequest> getDiscoveryRequest();

	/**
	 * {@return the collection of additional test execution listeners that
	 * should be notified about events pertaining to this execution request}
	 */
	Collection<? extends TestExecutionListener> getAdditionalTestExecutionListeners();

	/**
	 * {@return the cancellation token for this execution request}
	 */
	CancellationToken getCancellationToken();

}
