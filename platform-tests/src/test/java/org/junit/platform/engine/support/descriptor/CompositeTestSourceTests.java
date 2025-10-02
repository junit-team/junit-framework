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

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.EqualsAndHashCodeAssertions.assertEqualsAndHashCode;
import static org.junit.platform.commons.test.PreconditionAssertions.assertPreconditionViolationFor;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;

/**
 * Unit tests for {@link CompositeTestSource}.
 *
 * @since 1.0
 */
class CompositeTestSourceTests extends AbstractTestSourceTests {

	@Override
	Stream<CompositeTestSource> createSerializableInstances() {
		var fileSource = FileSource.from(new File("sample.instance"));
		var classSource = ClassSource.from(getClass());
		var sources = List.of(fileSource, classSource);
		return Stream.of(CompositeTestSource.from(sources));
	}

	@SuppressWarnings("DataFlowIssue")
	@Test
	void createCompositeTestSourceFromNullList() {
		assertPreconditionViolationFor(() -> CompositeTestSource.from(null));
	}

	@Test
	void createCompositeTestSourceFromEmptyList() {
		assertPreconditionViolationFor(() -> CompositeTestSource.from(List.of()));
	}

	@Test
	void createCompositeTestSourceFromClassAndFileSources() {
		var fileSource = FileSource.from(new File("example.test"));
		var classSource = ClassSource.from(getClass());
		var sources = new ArrayList<>(List.of(fileSource, classSource));
		var compositeTestSource = CompositeTestSource.from(sources);

		assertThat(compositeTestSource.getSources()).hasSize(2);
		assertThat(compositeTestSource.getSources()).contains(fileSource, classSource);

		// Ensure the supplied sources list was defensively copied.
		sources.remove(1);
		assertThat(compositeTestSource.getSources()).hasSize(2);

		// Ensure the returned sources list is immutable.
		assertThrows(UnsupportedOperationException.class, () -> compositeTestSource.getSources().add(fileSource));
	}

	@Test
	void equalsAndHashCode() {
		var sources1 = List.of(ClassSource.from(Number.class));
		var sources2 = List.of(ClassSource.from(String.class));
		assertEqualsAndHashCode(CompositeTestSource.from(sources1), CompositeTestSource.from(sources1),
			CompositeTestSource.from(sources2));
	}

}
