/*
 * Copyright 2015-2025 the original author or authors.
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v2.0 which
 * accompanies this distribution and is available at
 *
 * https://www.eclipse.org/legal/epl-v20.html
 */

package org.junit.jupiter.api.util;

import java.util.TimeZone;
import java.util.function.Supplier;

import org.apiguardian.api.API;

@API(status = API.Status.STABLE, since = "6.1")
public interface TimeZoneProvider extends Supplier<TimeZone> {

	interface NullTimeZoneProvider extends TimeZoneProvider {
	}

}
