/*
 * Copyright 2015-2026 the original author or authors.
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v2.0 which
 * accompanies this distribution and is available at
 *
 * https://www.eclipse.org/legal/epl-v20.html
 */

package org.junit.jupiter.api.util;

import static org.apiguardian.api.API.Status.STABLE;

import java.util.TimeZone;
import java.util.function.Supplier;

import org.apiguardian.api.API;

/**
 * Custom {@link TimeZone} provider for use with
 * {@link DefaultTimeZone#timeZoneProvider()}.
 *
 * @since 6.1
 */
@API(status = STABLE, since = "6.1")
public interface TimeZoneProvider extends Supplier<TimeZone> {
}
