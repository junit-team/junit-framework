/*
 * Copyright 2015-2025 the original author or authors.
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v2.0 which
 * accompanies this distribution and is available at
 *
 * https://www.eclipse.org/legal/epl-v20.html
 */

package org.junit.platform.engine.support.descriptor;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.EqualsAndHashCodeAssertions.assertEqualsAndHashCode;
import static org.junit.platform.commons.test.PreconditionAssertions.assertPreconditionViolationFor;

import java.net.URI;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;

/**
 * Unit tests for {@link DefaultUriSource}.
 *
 * @since 1.3
 */
class DefaultUriSourceTests extends AbstractTestSourceTests {

	@Override
	Stream<UriSource> createSerializableInstances() {
		return Stream.of(new DefaultUriSource(URI.create("sample://instance")));
	}

	@SuppressWarnings("DataFlowIssue")
	@Test
	void nullSourceUriYieldsException() {
		assertPreconditionViolationFor(() -> new DefaultUriSource(null));
	}

	@Test
	void getterReturnsSameUriInstanceAsSuppliedToTheConstructor() throws Exception {
		var expected = new URI("foo.txt");
		var actual = new DefaultUriSource(expected).getUri();
		assertSame(expected, actual);
	}

	@Test
	void equalsAndHashCode() throws Exception {
		var uri1 = new URI("foo.txt");
		var uri2 = new URI("bar.txt");
		assertEqualsAndHashCode(new DefaultUriSource(uri1), new DefaultUriSource(uri1), new DefaultUriSource(uri2));
	}

	@Test
	void testToString() {
		var actual = new DefaultUriSource(URI.create("foo.txt")).toString();
		assertEquals("DefaultUriSource [uri = foo.txt]", actual);
	}
}
