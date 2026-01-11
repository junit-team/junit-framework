/*
 * Copyright 2015-2026 the original author or authors.
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v2.0 which
 * accompanies this distribution and is available at
 *
 * https://www.eclipse.org/legal/epl-v20.html
 */

package org.junit.jupiter.engine;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.fail;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.engine.subpackage.SuperClassWithPackagePrivateLifecycleMethodInDifferentPackageTestCase;

/**
 * Integration tests that explicitly demonstrate the overriding rules for
 * lifecycle methods in the {@link JupiterTestEngine}.
 *
 * @since 5.9
 */
class LifecycleMethodOverridingTests {

	@Nested
	@DisplayName("A package-private lifecycle method can be overridden by")
	class PackagePrivateSuperClassTests {

		@Nested
		@DisplayName("a protected lifecycle method in a subclass")
		class ProtectedExtendsPackagePrivateLifecycleMethod
				extends SuperClassWithPackagePrivateLifecycleMethodTestCase {

			@Override
			@BeforeEach
			protected void beforeEach() {
			}

		}

		@Nested
		@DisplayName("a package-private lifecycle method in a subclass")
		class PackagePrivateExtendsPackagePrivateLifecycleMethod
				extends SuperClassWithPackagePrivateLifecycleMethodTestCase {

			@Override
			@BeforeEach
			void beforeEach() {
			}

		}

		@Nested
		@DisplayName("a public lifecycle method in a subclass")
		class PublicExtendsPackagePrivateLifecycleMethod extends SuperClassWithPackagePrivateLifecycleMethodTestCase {

			@Override
			@BeforeEach
			public void beforeEach() {
			}

		}
	}

	@Nested
	@DisplayName("A package-private lifecycle method from a different package cannot be overridden by")
	class PackagePrivateSuperClassInDifferentPackageTests {

		@Nested
		@DisplayName("a protected lifecycle method in a subclass")
		class ProtectedExtendsPackagePrivateLifecycleMethod
				extends SuperClassWithPackagePrivateLifecycleMethodInDifferentPackageTestCase {

			// @Override
			@BeforeEach
			protected void beforeEach() {
				assertThat(super.beforeEachInvoked).isTrue();
			}

		}

		@Nested
		@DisplayName("a package-private lifecycle method in a subclass")
		class PackagePrivateExtendsPackagePrivateLifecycleMethod
				extends SuperClassWithPackagePrivateLifecycleMethodInDifferentPackageTestCase {

			// @Override
			@BeforeEach
			void beforeEach() {
				assertThat(super.beforeEachInvoked).isTrue();
			}

		}

		@Nested
		@DisplayName("a public lifecycle method in a subclass")
		class PublicExtendsPackagePrivateLifecycleMethod
				extends SuperClassWithPackagePrivateLifecycleMethodInDifferentPackageTestCase {

			// @Override
			@BeforeEach
			public void beforeEach() {
				assertThat(super.beforeEachInvoked).isTrue();
			}

		}
	}

	@Nested
	@DisplayName("A protected lifecycle method can be overridden by")
	class ProtectedSuperClassTests {

		@Nested
		@DisplayName("a protected lifecycle method in a subclass")
		class ProtectedExtendsPackagePrivate extends SuperClassWithProtectedLifecycleMethodTestCase {

			@Override
			@BeforeEach
			protected void beforeEach() {
			}

		}

		@Nested
		@DisplayName("a public lifecycle method in a subclass")
		class PublicExtendsPackagePrivate extends SuperClassWithProtectedLifecycleMethodTestCase {

			@Override
			@BeforeEach
			public void beforeEach() {
			}

		}
	}

	@Nested
	@DisplayName("A public lifecycle method can be overridden by")
	class PublicSuperClassTests {

		@Nested
		@DisplayName("a public lifecycle method in a subclass")
		class PublicExtendsPackagePrivate extends SuperClassWithPublicLifecycleMethodTestCase {

			@Override
			@BeforeEach
			public void beforeEach() {
			}

		}
	}

}

// -------------------------------------------------------------------------

class SuperClassWithPackagePrivateLifecycleMethodTestCase {

	@BeforeEach
	void beforeEach() {
		fail();
	}

	@Test
	void test() {
	}

}

class SuperClassWithProtectedLifecycleMethodTestCase {

	@BeforeEach
	protected void beforeEach() {
		fail();
	}

	@Test
	void test() {
	}

}

class SuperClassWithPublicLifecycleMethodTestCase {

	@BeforeEach
	public void beforeEach() {
		fail();
	}

	@Test
	void test() {
	}

}
