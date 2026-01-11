/*
 * Copyright 2015-2026 the original author or authors.
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v2.0 which
 * accompanies this distribution and is available at
 *
 * https://www.eclipse.org/legal/epl-v20.html
 */

package org.junit.jupiter.api;

/**
 * This is a top-level type in order to avoid issues with
 * {@link Class#getCanonicalName()} when using different class
 * loaders in tests.
 *
 * @since 5.0
 */
@SuppressWarnings("serial")
class EnigmaThrowable extends Throwable {
}
