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

import static org.apiguardian.api.API.Status.EXPERIMENTAL;

import java.util.ServiceLoader;

import org.apiguardian.api.API;

/**
 * An {@code EarlyExtension} is a special kind of {@link Extension} that is
 * available <em>before</em> a test {@link ExtensionContext} exists.
 *
 * <p>Regular {@link Extension Extensions} are only discovered and instantiated
 * once the engine starts executing the discovery result. An
 * {@code EarlyExtension}, in contrast, is loaded <em>during discovery</em> so
 * that the engine can consult it while it is still deciding what is and is not
 * a test.
 *
 * <p>Implementations can be registered <em>automatically</em> via the
 * {@link ServiceLoader} mechanism by listing them under the standard
 * {@code META-INF/services/org.junit.jupiter.api.extension.Extension} file,
 * in which case they are filtered by type and only loaded when the engine is
 * asked to auto-detect extensions. Alternatively, when an implementation is
 * only needed at runtime (for example to convert a returned value), it can be
 * registered declaratively via {@link ExtendWith @ExtendWith} or
 * {@link RegisterExtension @RegisterExtension} just like any other
 * {@link Extension}.
 *
 * <h2>Lifecycle</h2>
 *
 * <p>Implementations must be <em>stateless</em> with respect to test execution,
 * and their discovery-relevant methods must return the same result regardless
 * of when they are called. Implementations must have a {@code public} default
 * constructor when loaded via the {@code ServiceLoader}.
 *
 * @since 6.2
 */
@API(status = EXPERIMENTAL, since = "6.2")
public interface EarlyExtension extends Extension {
}
