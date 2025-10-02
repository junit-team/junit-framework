/*
 * Copyright 2015-2025 the original author or authors.
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v2.0 which
 * accompanies this distribution and is available at
 *
 * https://www.eclipse.org/legal/epl-v20.html
 */

package org.junit.platform.commons.util;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.DynamicTest.dynamicTest;
import static org.junit.platform.commons.test.PreconditionAssertions.assertPreconditionViolationFor;

import java.util.List;
import java.util.function.Function;

import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestFactory;
import org.junit.jupiter.api.function.Executable;
import org.opentest4j.ValueWrapper;

/**
 * Unit tests for {@link PackageUtils}.
 *
 * @since 1.0
 */
class PackageUtilsTests {

	@SuppressWarnings("DataFlowIssue")
	@Test
	void getAttributeWithNullType() {
		assertPreconditionViolationFor(() -> PackageUtils.getAttribute(null, p -> "any")).withMessage(
			"type must not be null");
	}

	@SuppressWarnings("DataFlowIssue")
	@Test
	void getAttributeWithNullFunction() {
		assertPreconditionViolationFor(
			() -> PackageUtils.getAttribute(getClass(), (Function<Package, String>) null)).withMessage(
				"function must not be null");
	}

	@SuppressWarnings("DataFlowIssue")
	@Test
	void getAttributeWithFunctionReturningNullIsEmpty() {
		assertFalse(PackageUtils.getAttribute(ValueWrapper.class, p -> null).isPresent());
	}

	@Test
	void getAttributeFromDefaultPackageMemberIsEmpty() throws Exception {
		var classInDefaultPackage = ReflectionUtils.tryToLoadClass("DefaultPackageTestCase").getNonNull();
		assertFalse(PackageUtils.getAttribute(classInDefaultPackage, Package::getSpecificationTitle).isPresent());
	}

	@TestFactory
	List<DynamicTest> attributesFromValueWrapperClassArePresent() {
		return List.of( //
			dynamicTest("getName", isPresent(Package::getName)),
			dynamicTest("getImplementationTitle", isPresent(Package::getImplementationTitle)),
			dynamicTest("getImplementationVendor", isPresent(Package::getImplementationVendor)),
			dynamicTest("getImplementationVersion", isPresent(Package::getImplementationVersion)),
			dynamicTest("getSpecificationTitle", isPresent(Package::getSpecificationTitle)),
			dynamicTest("getSpecificationVendor", isPresent(Package::getSpecificationVendor)),
			dynamicTest("getSpecificationVersion", isPresent(Package::getSpecificationVersion)) //
		);
	}

	private Executable isPresent(Function<Package, String> function) {
		return () -> assertTrue(PackageUtils.getAttribute(ValueWrapper.class, function).isPresent());
	}

	@SuppressWarnings("DataFlowIssue")
	@Test
	void getAttributeWithNullTypeAndName() {
		assertPreconditionViolationFor(() -> PackageUtils.getAttribute(null, "foo")).withMessage(
			"type must not be null");
	}

	@SuppressWarnings("DataFlowIssue")
	@Test
	void getAttributeWithNullName() {
		assertPreconditionViolationFor(() -> PackageUtils.getAttribute(getClass(), (String) null)).withMessage(
			"name must not be blank");
	}

	@Test
	void getAttributeWithEmptyName() {
		assertPreconditionViolationFor(() -> PackageUtils.getAttribute(getClass(), "")).withMessage(
			"name must not be blank");
	}
}
