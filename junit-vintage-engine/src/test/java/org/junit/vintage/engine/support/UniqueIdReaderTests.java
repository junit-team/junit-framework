/*
 * Copyright 2015-2026 the original author or authors.
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v2.0 which
 * accompanies this distribution and is available at
 *
 * https://www.eclipse.org/legal/epl-v20.html
 */

package org.junit.vintage.engine.support;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.runner.Description.createTestDescription;

import java.util.logging.Level;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.fixtures.TrackLogRecords;
import org.junit.platform.commons.logging.LogRecordListener;

/**
 * Tests for {@link UniqueIdReader}.
 *
 * @since 4.12
 */
@TrackLogRecords
class UniqueIdReaderTests {

	@Test
	void readsUniqueId(LogRecordListener listener) {
		var description = createTestDescription("ClassName", "methodName", "uniqueId");

		var uniqueId = new UniqueIdReader().apply(description);

		assertEquals("uniqueId", uniqueId);
		assertThat(listener.stream(UniqueIdReader.class)).isEmpty();
	}

	@Test
	void returnsDisplayNameWhenUniqueIdCannotBeRead(LogRecordListener listener) {
		var description = createTestDescription("ClassName", "methodName", "uniqueId");
		assertEquals("methodName(ClassName)", description.getDisplayName());

		var uniqueId = new UniqueIdReader("wrongFieldName").apply(description);

		assertEquals(description.getDisplayName(), uniqueId);

		var logRecord = listener.stream(UniqueIdReader.class, Level.WARNING).findFirst();
		assertThat(logRecord).isPresent();
		assertThat(logRecord.get().getMessage()).isEqualTo(
			"Could not read unique ID for Description; using display name instead: " + description.getDisplayName());
		assertThat(logRecord.get().getThrown()).isInstanceOf(NoSuchFieldException.class);
	}

}
