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

import static org.apiguardian.api.API.Status.INTERNAL;

import org.apiguardian.api.API;

/**
 * {@code Filterable} is implemented by
 * {@link org.junit.platform.engine.TestDescriptor TestDescriptors} that may
 * register dynamic tests during execution and support selective test execution.
 *
 * @since 5.1
 * @see DynamicDescendantFilter
 */
@API(status = INTERNAL, since = "5.1")
public interface Filterable {

	DynamicDescendantFilter getDynamicDescendantFilter();

}
