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

import static org.junit.jupiter.engine.descriptor.MethodSourceSupport.METHOD_SCHEME;
import static org.junit.platform.engine.support.descriptor.ClassSource.CLASS_SCHEME;
import static org.junit.platform.engine.support.descriptor.ClasspathResourceSource.CLASSPATH_SCHEME;

import java.net.URI;

import org.junit.platform.commons.util.Preconditions;
import org.junit.platform.engine.TestSource;
import org.junit.platform.engine.support.descriptor.ClassSource;
import org.junit.platform.engine.support.descriptor.ClasspathResourceSource;
import org.junit.platform.engine.support.descriptor.UriSource;

/**
 * Jupiter internal support for creating {@link TestSource} instances from
 * {@link URI URIs}.
 *
 * @since 6.2
 */
final class TestSourceSupport {

	static TestSource fromUri(URI uri) {
		Preconditions.notNull(uri, "URI must not be null");
		if (CLASSPATH_SCHEME.equals(uri.getScheme())) {
			return ClasspathResourceSource.from(uri);
		}
		if (CLASS_SCHEME.equals(uri.getScheme())) {
			return ClassSource.from(uri);
		}
		if (METHOD_SCHEME.equals(uri.getScheme())) {
			return MethodSourceSupport.from(uri);
		}
		return UriSource.from(uri);
	}

	private TestSourceSupport() {
	}

}
