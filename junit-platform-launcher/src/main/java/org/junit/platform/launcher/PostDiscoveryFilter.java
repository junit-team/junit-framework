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

import static org.apiguardian.api.API.Status.STABLE;

import org.apiguardian.api.API;
import org.junit.platform.engine.Filter;
import org.junit.platform.engine.TestDescriptor;
import org.junit.platform.engine.TestEngine;

/**
 * A {@code PostDiscoveryFilter} is applied to {@link TestDescriptor TestDescriptors}
 * after test discovery.
 *
 * <p>A {@code PostDiscoveryFilter} must <strong>not</strong> modify the
 * {@link TestDescriptor TestDescriptors} it is applied to in any way.
 *
 * <p>{@link TestEngine TestEngines} must <strong>not</strong> apply
 * {@code PostDiscoveryFilters} during the test discovery phase.
 *
 * @since 1.0
 * @see LauncherDiscoveryRequest
 * @see TestEngine
 */
@API(status = STABLE, since = "1.0")
public interface PostDiscoveryFilter extends Filter<TestDescriptor> {
}
