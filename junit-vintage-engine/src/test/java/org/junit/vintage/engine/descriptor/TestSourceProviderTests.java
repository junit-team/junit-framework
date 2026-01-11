/*
 * Copyright 2015-2026 the original author or authors.
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v2.0 which
 * accompanies this distribution and is available at
 *
 * https://www.eclipse.org/legal/epl-v20.html
 */

package org.junit.vintage.engine.descriptor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;
import org.junit.platform.engine.support.descriptor.MethodSource;
import org.junit.runner.Description;
import org.junit.vintage.engine.samples.junit4.ConcreteJUnit4TestCase;

/**
 * @since 5.6
 */
class TestSourceProviderTests {

	@Test
	void findsInheritedMethod() {
		var description = Description.createTestDescription(ConcreteJUnit4TestCase.class, "theTest");

		var source = new TestSourceProvider().findTestSource(description);
		assertThat(source).isInstanceOf(MethodSource.class);

		var methodSource = (MethodSource) source;
		assertEquals(ConcreteJUnit4TestCase.class.getName(), methodSource.getClassName());
		assertEquals("theTest", methodSource.getMethodName());
	}

}
