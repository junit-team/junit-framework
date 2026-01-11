/*
 * Copyright 2015-2026 the original author or authors.
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v2.0 which
 * accompanies this distribution and is available at
 *
 * https://www.eclipse.org/legal/epl-v20.html
 */

package org.junit.jupiter.api.extension;

import static org.apiguardian.api.API.Status.STABLE;

import org.apiguardian.api.API;

/**
 * Marker interface for all extensions.
 *
 * <p>An {@code Extension} can be registered <em>declaratively</em> via
 * {@link ExtendWith @ExtendWith}, <em>programmatically</em> via
 * {@link RegisterExtension @RegisterExtension}, or <em>automatically</em> via
 * the {@link java.util.ServiceLoader} mechanism. For details on the latter,
 * consult the User Guide.
 *
 * <h2>Constructor Requirements</h2>
 *
 * <p>Extension implementations must have a <em>default constructor</em> if
 * registered via {@code @ExtendWith} or the {@code ServiceLoader}. When
 * registered via {@code @ExtendWith} the default constructor is not required
 * to be {@code public}. When registered via the {@code ServiceLoader} the
 * default constructor must be {@code public}. When registered via
 * {@code @RegisterExtension} the extension's constructors typically must be
 * {@code public} unless the extension provides {@code static} factory methods
 * or a builder API as an alternative to constructors.
 *
 * @since 5.0
 */
@API(status = STABLE, since = "5.0")
public interface Extension {
}
