/*
 * Copyright 2015-2026 the original author or authors.
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v2.0 which
 * accompanies this distribution and is available at
 *
 * https://www.eclipse.org/legal/epl-v20.html
 */

package org.junit.vintage.engine.samples.junit4;

import org.junit.runner.RunWith;
import org.junit.vintage.engine.samples.junit4.ConfigurableRunner.ChildCount;

/**
 * Simulates a Spock 1.x test with only {@code @Unroll} feature methods.
 */
@RunWith(DynamicRunner.class)
@ChildCount(1)
public class CompletelyDynamicTestCase {
}
