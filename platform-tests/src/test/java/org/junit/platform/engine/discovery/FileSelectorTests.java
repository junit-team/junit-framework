/*
 * Copyright 2015-2026 the original author or authors.
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v2.0 which
 * accompanies this distribution and is available at
 *
 * https://www.eclipse.org/legal/epl-v20.html
 */

package org.junit.platform.engine.discovery;

import static org.junit.jupiter.api.EqualsAndHashCodeAssertions.assertEqualsAndHashCode;

import org.junit.jupiter.api.Test;

/**
 * Unit tests for {@link FileSelector}.
 *
 * @since 1.3
 * @see DiscoverySelectorsTests
 */
class FileSelectorTests {

	@Test
	void equalsAndHashCode() {
		var selector1 = new FileSelector("/example/foo.txt", null);
		var selector2 = new FileSelector("/example/foo.txt", null);
		var selector3 = new FileSelector("/example/bar.txt", null);

		assertEqualsAndHashCode(selector1, selector2, selector3);
	}

	@Test
	void equalsAndHashCodeWithFilePosition() {
		var selector1 = new FileSelector("/example/foo.txt", FilePosition.from(1));
		var selector2 = new FileSelector("/example/foo.txt", FilePosition.from(1));
		var selector3 = new FileSelector("/example/bar.txt", FilePosition.from(2));

		assertEqualsAndHashCode(selector1, selector2, selector3);
	}

}
