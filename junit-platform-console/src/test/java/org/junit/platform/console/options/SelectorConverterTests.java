/*
 * Copyright 2015-2026 the original author or authors.
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v2.0 which
 * accompanies this distribution and is available at
 *
 * https://www.eclipse.org/legal/epl-v20.html
 */

package org.junit.platform.console.options;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import org.junit.platform.engine.discovery.ClasspathResourceSelector;
import org.junit.platform.engine.discovery.FilePosition;
import org.junit.platform.engine.discovery.FileSelector;

class SelectorConverterTests {

	@Test
	void fileSelectorAcceptsWindowsPathWithSpacesAndQuery() {
		SelectorConverter.File converter = new SelectorConverter.File();
		FileSelector selector = converter.convert("C:\\work\\My Tests\\FooTest.java?line=12&column=34");

		assertEquals("C:\\work\\My Tests\\FooTest.java", selector.getRawPath());
		FilePosition position = selector.getPosition().orElseThrow();
		assertEquals(12, position.getLine());
		assertEquals(34, position.getColumn().orElseThrow());
	}

	@Test
	void classpathResourceSelectorAcceptsSpacesAndQuery() {
		SelectorConverter.ClasspathResource converter = new SelectorConverter.ClasspathResource();
		ClasspathResourceSelector selector = converter.convert("data files/my test.xml?line=5");

		assertEquals("data files/my test.xml", selector.getClasspathResourceName());
		FilePosition position = selector.getPosition().orElseThrow();
		assertEquals(5, position.getLine());
		assertTrue(position.getColumn().isEmpty());
	}
}
