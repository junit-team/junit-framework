/*
 * Copyright 2015-2025 the original author or authors.
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v2.0 which
 * accompanies this distribution and is available at
 *
 * https://www.eclipse.org/legal/epl-v20.html
 */

package org.junit.jupiter.api.extension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.EqualsAndHashCodeAssertions.assertEqualsAndHashCode;

import org.junit.jupiter.api.Test;

/**
 * Interoperability tests for {@link org.junit.jupiter.api.MediaType} and the
 * deprecated {@link org.junit.jupiter.api.extension.MediaType}.
 *
 * @since 6.0
 * @see DeprecatedMediaTypeTests
 */
class MediaTypeInteroperabilityTests {

	@Test
	@SuppressWarnings("removal")
	void newAndDeprecatedMediaTypesAreLogicallyEquivalent() {
		var mediaType = org.junit.jupiter.api.MediaType.TEXT_PLAIN_UTF_8;
		var deprecatedMediaType = MediaType.TEXT_PLAIN_UTF_8;
		var differentMediaType = MediaType.TEXT_PLAIN;

		assertThat(mediaType.getClass()).isNotEqualTo(deprecatedMediaType.getClass());
		assertEqualsAndHashCode(mediaType, deprecatedMediaType, differentMediaType);
	}

}
