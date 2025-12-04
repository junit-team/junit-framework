/*
 * Copyright 2015-2025 the original author or authors.
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v2.0 which
 * accompanies this distribution and is available at
 *
 * https://www.eclipse.org/legal/epl-v20.html
 */

package org.junit.platform.commons.support;

import static org.junit.jupiter.api.EqualsAndHashCodeAssertions.assertEqualsAndHashCode;

import java.net.URI;

import org.junit.jupiter.api.Test;
import org.junit.platform.commons.io.Resource;

public class ResourceInteroperabilityTests {

	@Test
	void newAndOldResourcesAreLogicallyEquivalent() {
		var oldResource = new DefaultResource("foo", URI.create("foo"));
		var newResource = Resource.of("foo", URI.create("foo"));
		var differentResource = Resource.of("foo", URI.create("bar"));

		assertEqualsAndHashCode(oldResource, newResource, differentResource);
	}
}
