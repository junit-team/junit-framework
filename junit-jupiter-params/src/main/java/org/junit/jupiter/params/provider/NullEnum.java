/*
 * Copyright 2015-2025 the original author or authors.
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v2.0 which
 * accompanies this distribution and is available at
 *
 * https://www.eclipse.org/legal/epl-v20.html
 */

package org.junit.jupiter.params.provider;

import static org.apiguardian.api.API.Status.INTERNAL;

import org.apiguardian.api.API;

/**
 * Dummy enum class used as the default value for optional attributes of annotations.
 *
 * @since 5.6
 * @see EnumSource#value()
 */
@API(status = INTERNAL, since = "5.7")
public enum NullEnum {
}
