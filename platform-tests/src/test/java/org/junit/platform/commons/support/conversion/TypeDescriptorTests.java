/*
 * Copyright 2015-2025 the original author or authors.
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v2.0 which
 * accompanies this distribution and is available at
 *
 * https://www.eclipse.org/legal/epl-v20.html
 */

package org.junit.platform.commons.support.conversion;

import static org.junit.jupiter.api.EqualsAndHashCodeAssertions.assertEqualsAndHashCode;

import org.junit.jupiter.api.Test;

class TypeDescriptorTests {

	@Test
	void equalsAndHashCode() {
		var typeDescriptor1 = TypeDescriptor.forClass(String.class);
		var typeDescriptor2 = TypeDescriptor.forClass(String.class);
		var typeDescriptor3 = TypeDescriptor.forClass(Object.class);

		assertEqualsAndHashCode(typeDescriptor1, typeDescriptor2, typeDescriptor3);
	}

}
