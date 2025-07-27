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

import java.io.Serializable;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.junit.platform.commons.PreconditionViolationException;

/**
 * Unit tests for {@link PackageSource}.
 *
 * @since 1.0
 */
class PackageSourceTests extends AbstractTestSourceTests {

	@Override
	Stream<Serializable> createSerializableInstances() {
		return Stream.of(PackageSource.from("package.source"));
	}

	@SuppressWarnings("DataFlowIssue")
	@Test
	void packageSourceFromNullPackageName() {
		assertThrows(PreconditionViolationException.class, () -> PackageSource.from((String) null));
	}

	@Test
	void packageSourceFromEmptyPackageName() {
		assertThrows(PreconditionViolationException.class, () -> PackageSource.from("  "));
	}

	@SuppressWarnings("DataFlowIssue")
	@Test
	void packageSourceFromNullPackageReference() {
		assertThrows(PreconditionViolationException.class, () -> PackageSource.from((Package) null));
	}

	@ParameterizedTest
	@ValueSource(classes = PackageSourceTests.class)
	@ValueSource(strings = "DefaultPackageTestCase")
	void packageSourceFromPackageName(Class<?> testClass) {
		var testPackage = testClass.getPackage().getName();
		var source = PackageSource.from(testPackage);

		assertThat(source.getPackageName()).isEqualTo(testPackage);
	}

	@Test
	void packageSourceFromPackageReference() {
		var testPackage = getClass().getPackage();
		var source = PackageSource.from(testPackage);

		assertThat(source.getPackageName()).isEqualTo(testPackage.getName());
	}

	@Test
	void equalsAndHashCodeForPackageSource() {
		var pkg1 = getClass().getPackage();
		var pkg2 = String.class.getPackage();
		assertEqualsAndHashCode(PackageSource.from(pkg1), PackageSource.from(pkg1), PackageSource.from(pkg2));
	}

}
