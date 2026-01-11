/*
 * Copyright 2015-2026 the original author or authors.
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v2.0 which
 * accompanies this distribution and is available at
 *
 * https://www.eclipse.org/legal/epl-v20.html
 */

package org.junit.platform.launcher;

import org.junit.platform.engine.FilterResult;
import org.junit.platform.engine.TestDescriptor;
import org.junit.platform.engine.TestTag;

public class TestPostDiscoveryTagFilter implements PostDiscoveryFilter {
	@Override
	public FilterResult apply(final TestDescriptor object) {
		var include = object.getTags().stream().map(TestTag::getName).anyMatch("test-post-discovery"::equals);
		return FilterResult.includedIf(include);
	}
}
