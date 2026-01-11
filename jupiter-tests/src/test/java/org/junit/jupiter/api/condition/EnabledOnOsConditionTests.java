/*
 * Copyright 2015-2026 the original author or authors.
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v2.0 which
 * accompanies this distribution and is available at
 *
 * https://www.eclipse.org/legal/epl-v20.html
 */

package org.junit.jupiter.api.condition;

import static org.junit.jupiter.api.condition.EnabledOnOsIntegrationTests.onAix;
import static org.junit.jupiter.api.condition.EnabledOnOsIntegrationTests.onArchitecture;
import static org.junit.jupiter.api.condition.EnabledOnOsIntegrationTests.onFreebsd;
import static org.junit.jupiter.api.condition.EnabledOnOsIntegrationTests.onLinux;
import static org.junit.jupiter.api.condition.EnabledOnOsIntegrationTests.onMac;
import static org.junit.jupiter.api.condition.EnabledOnOsIntegrationTests.onOpenbsd;
import static org.junit.jupiter.api.condition.EnabledOnOsIntegrationTests.onSolaris;
import static org.junit.jupiter.api.condition.EnabledOnOsIntegrationTests.onWindows;
import static org.junit.platform.commons.test.PreconditionAssertions.assertPreconditionViolationFor;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExecutionCondition;

/**
 * Unit tests for {@link EnabledOnOsCondition}.
 *
 * <p>Note that test method names MUST match the test method names in
 * {@link EnabledOnOsIntegrationTests}.
 *
 * @since 5.1
 */
class EnabledOnOsConditionTests extends AbstractExecutionConditionTests {

	private static final String OS_NAME = System.getProperty("os.name");
	private static final String ARCH = System.getProperty("os.arch");

	@Override
	protected ExecutionCondition getExecutionCondition() {
		return new EnabledOnOsCondition();
	}

	@Override
	protected Class<?> getTestClass() {
		return EnabledOnOsIntegrationTests.class;
	}

	/**
	 * @see EnabledOnOsIntegrationTests#enabledBecauseAnnotationIsNotPresent()
	 */
	@Test
	void enabledBecauseAnnotationIsNotPresent() {
		evaluateCondition();
		assertEnabled();
		assertReasonContains("@EnabledOnOs is not present");
	}

	/**
	 * @see EnabledOnOsIntegrationTests#missingOsAndArchitectureDeclaration()
	 */
	@Test
	void missingOsAndArchitectureDeclaration() {
		assertPreconditionViolationFor(this::evaluateCondition)//
				.withMessageContaining("You must declare at least one OS or architecture");
	}

	/**
	 * @see EnabledOnOsIntegrationTests#enabledOnEveryOs()
	 */
	@Test
	void enabledOnEveryOs() {
		evaluateCondition();
		assertEnabledOnCurrentOsIf(true);
	}

	/**
	 * @see EnabledOnOsIntegrationTests#aix()
	 */
	@Test
	void aix() {
		evaluateCondition();
		assertEnabledOnCurrentOsIf(onAix());
	}

	/**
	 * @see EnabledOnOsIntegrationTests#freebsd()
	 */
	@Test
	void freebsd() {
		evaluateCondition();
		assertEnabledOnCurrentOsIf(onFreebsd());
	}

	/**
	 * @see EnabledOnOsIntegrationTests#linux()
	 */
	@Test
	void linux() {
		evaluateCondition();
		assertEnabledOnCurrentOsIf(onLinux());
	}

	/**
	 * @see EnabledOnOsIntegrationTests#macOs()
	 */
	@Test
	void macOs() {
		evaluateCondition();
		assertEnabledOnCurrentOsIf(onMac());
	}

	/**
	 * @see EnabledOnOsIntegrationTests#macOsWithComposedAnnotation()
	 */
	@Test
	void macOsWithComposedAnnotation() {
		evaluateCondition();
		assertEnabledOnCurrentOsIf(onMac());
	}

	/**
	 * @see EnabledOnOsIntegrationTests#openbsd()
	 */
	@Test
	void openbsd() {
		evaluateCondition();
		assertEnabledOnCurrentOsIf(onOpenbsd());
	}

	/**
	 * @see EnabledOnOsIntegrationTests#windows()
	 */
	@Test
	void windows() {
		evaluateCondition();
		assertEnabledOnCurrentOsIf(onWindows());
	}

	/**
	 * @see EnabledOnOsIntegrationTests#solaris()
	 */
	@Test
	void solaris() {
		evaluateCondition();
		assertEnabledOnCurrentOsIf(onSolaris());
	}

	/**
	 * @see EnabledOnOsIntegrationTests#other()
	 */
	@Test
	void other() {
		evaluateCondition();
		assertEnabledOnCurrentOsIf(
			!(onAix() || onFreebsd() || onLinux() || onMac() || onOpenbsd() || onSolaris() || onWindows()));
		assertCustomDisabledReasonIs("Disabled on almost every OS");
	}

	/**
	 * @see EnabledOnOsIntegrationTests#architectureX86_64()
	 */
	@Test
	void architectureX86_64() {
		evaluateCondition();
		assertEnabledOnCurrentArchitectureIf(onArchitecture("x86_64"));
	}

	/**
	 * @see EnabledOnOsIntegrationTests#architectureAarch64()
	 */
	@Test
	void architectureAarch64() {
		evaluateCondition();
		assertEnabledOnCurrentArchitectureIf(onArchitecture("aarch64"));
	}

	/**
	 * @see EnabledOnOsIntegrationTests#architectureX86_64WithMacOs()
	 */
	@Test
	void architectureX86_64WithMacOs() {
		evaluateCondition();
		assertEnabledOnCurrentOsAndArchitectureIf(onMac() && onArchitecture("x86_64"));
	}

	/**
	 * @see EnabledOnOsIntegrationTests#architectureX86_64WithWindows()
	 */
	@Test
	void architectureX86_64WithWindows() {
		evaluateCondition();
		assertEnabledOnCurrentOsAndArchitectureIf(onWindows() && onArchitecture("x86_64"));
	}

	/**
	 * @see EnabledOnOsIntegrationTests#architectureX86_64WithLinux()
	 */
	@Test
	void architectureX86_64WithLinux() {
		evaluateCondition();
		assertEnabledOnCurrentOsAndArchitectureIf(onLinux() && onArchitecture("x86_64"));
	}

	/**
	 * @see EnabledOnOsIntegrationTests#aarch64WithMacOs()
	 */
	@Test
	void aarch64WithMacOs() {
		evaluateCondition();
		assertEnabledOnCurrentOsAndArchitectureIf(onMac() && onArchitecture("aarch64"));
	}

	/**
	 * @see EnabledOnOsIntegrationTests#aarch64WithWindows()
	 */
	@Test
	void aarch64WithWindows() {
		evaluateCondition();
		assertEnabledOnCurrentOsAndArchitectureIf(onWindows() && onArchitecture("aarch64"));
	}

	/**
	 * @see EnabledOnOsIntegrationTests#aarch64WithLinux()
	 */
	@Test
	void aarch64WithLinux() {
		evaluateCondition();
		assertEnabledOnCurrentOsAndArchitectureIf(onLinux() && onArchitecture("aarch64"));
	}

	private void assertEnabledOnCurrentOsIf(boolean condition) {
		if (condition) {
			assertEnabled();
			assertReasonContains("Enabled on operating system: %s".formatted(OS_NAME));
		}
		else {
			assertDisabled();
			assertReasonContains("Disabled on operating system: %s".formatted(OS_NAME));
		}
	}

	private void assertEnabledOnCurrentArchitectureIf(boolean condition) {
		if (condition) {
			assertEnabled();
			assertReasonContains("Enabled on architecture: %s".formatted(ARCH));
		}
		else {
			assertDisabled();
			assertReasonContains("Disabled on architecture: %s".formatted(ARCH));
		}
	}

	private void assertEnabledOnCurrentOsAndArchitectureIf(boolean condition) {
		if (condition) {
			assertEnabled();
			assertReasonContains("Enabled on operating system: %s (%s)".formatted(OS_NAME, ARCH));
		}
		else {
			assertDisabled();
			assertReasonContains("Disabled on operating system: %s (%s)".formatted(OS_NAME, ARCH));
		}
	}

}
