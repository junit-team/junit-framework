/*
 * Copyright 2015-2026 the original author or authors.
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v2.0 which
 * accompanies this distribution and is available at
 *
 * https://www.eclipse.org/legal/epl-v20.html
 */

package org.junit.jupiter.engine.extension.sub;

/**
 * Intentionally in a subpackage in order to properly test deactivation
 * of conditions based on patterns. In other words, we do not want this
 * condition declared in the same package as the
 * {@link org.junit.jupiter.engine.extension.DisabledCondition}
 *
 * ExecutionCondition always returns disabled, since we want to test the
 * deactivation of the condition itself.
 *
 * @since 5.7
 */
public class AnotherAlwaysDisabledCondition extends AlwaysDisabledCondition {
}
