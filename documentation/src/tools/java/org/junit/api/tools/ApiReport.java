/*
 * Copyright 2015-2026 the original author or authors.
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v2.0 which
 * accompanies this distribution and is available at
 *
 * https://www.eclipse.org/legal/epl-v20.html
 */

package org.junit.api.tools;

import java.util.List;
import java.util.Map;
import java.util.SortedSet;

import io.github.classgraph.ClassInfo;

import org.apiguardian.api.API.Status;

/**
 * @since 1.0
 */
record ApiReport(SortedSet<ClassInfo> types, Map<Status, List<Declaration>> declarations) {
}
