/*
 * Copyright 2015-2026 the original author or authors.
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v2.0 which
 * accompanies this distribution and is available at
 *
 * https://www.eclipse.org/legal/epl-v20.html
 */

package org.junit.platform.engine.support.hierarchical;

import static org.apiguardian.api.API.Status.MAINTAINED;

import org.apiguardian.api.API;

/**
 * Marker interface for an execution context used by a concrete implementation
 * of {@link HierarchicalTestEngine} and its collaborators.
 *
 * @since 1.0
 * @see HierarchicalTestEngine
 */
@API(status = MAINTAINED, since = "1.0")
public interface EngineExecutionContext {
}
