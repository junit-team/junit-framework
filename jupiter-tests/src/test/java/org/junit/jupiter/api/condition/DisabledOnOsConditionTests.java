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
 * Unit tests for {@link DisabledOnOsCondition}.
 *
 * <p>Note that test method names MUST match the test method names in
 * {@link DisabledOnOsIntegrationTests}.
 *
 * @since 5.1
 */
class DisabledOnOsConditionTests extends AbstractExecutionConditionTests {

	private static final String OS_NAME = System.getProperty("os.name");
	private static final String ARCH = System.getProperty("os.arch");

	@Override
	protected ExecutionCondition getExecutionCondition() {
		return new DisabledOnOsCondition();
	}

	@Override
	protected Class<?> getTestClass() {
		return DisabledOnOsIntegrationTests.class;
	}

	/**
	 * @see DisabledOnOsIntegrationTests#enabledBecauseAnnotationIsNotPresent()
	 */
	@Test
	void enabledBecauseAnnotationIsNotPresent() {
		evaluateCondition();
		assertEnabled();
		assertReasonContains("@DisabledOnOs is not present");
	}

	/**
	 * @see DisabledOnOsIntegrationTests#missingOsAndArchitectureDeclaration()
	 */
	@Test
	void missingOsAndArchitectureDeclaration() {
		assertPreconditionViolationFor(this::evaluateCondition)//
				.withMessageContaining("You must declare at least one OS or architecture");
	}

	/**
	 * @see DisabledOnOsIntegrationTests#disabledOnEveryOs()
	 */
	@Test
	void disabledOnEveryOs() {
		evaluateCondition();
		assertDisabled();
		assertReasonContains("Disabled on operating system: %s ==> Disabled on every OS".formatted(OS_NAME));
	}

	/**
	 * @see DisabledOnOsIntegrationTests#aix()
	 */
	@Test
	void aix() {
		evaluateCondition();
		assertDisabledOnCurrentOsIf(onAix());
	}

	/**
	 * @see DisabledOnOsIntegrationTests#freebsd()
	 */
	@Test
	void freebsd() {
		evaluateCondition();
		assertDisabledOnCurrentOsIf(onFreebsd());
	}

	/**
	 * @see DisabledOnOsIntegrationTests#linux()
	 */
	@Test
	void linux() {
		evaluateCondition();
		assertDisabledOnCurrentOsIf(onLinux());
	}

	/**
	 * @see DisabledOnOsIntegrationTests#macOs()
	 */
	@Test
	void macOs() {
		evaluateCondition();
		assertDisabledOnCurrentOsIf(onMac());
	}

	/**
	 * @see DisabledOnOsIntegrationTests#macOsWithComposedAnnotation()
	 */
	@Test
	void macOsWithComposedAnnotation() {
		evaluateCondition();
		assertDisabledOnCurrentOsIf(onMac());
	}

	/**
	 * @see DisabledOnOsIntegrationTests#openbsd()
	 */
	@Test
	void openbsd() {
		evaluateCondition();
		assertDisabledOnCurrentOsIf(onOpenbsd());
	}

	/**
	 * @see DisabledOnOsIntegrationTests#windows()
	 */
	@Test
	void windows() {
		evaluateCondition();
		assertDisabledOnCurrentOsIf(onWindows());
	}

	/**
	 * @see DisabledOnOsIntegrationTests#solaris()
	 */
	@Test
	void solaris() {
		evaluateCondition();
		assertDisabledOnCurrentOsIf(onSolaris());
	}

	/**
	 * @see DisabledOnOsIntegrationTests#other()
	 */
	@Test
	void other() {
		evaluateCondition();
		assertDisabledOnCurrentOsIf(
			!(onAix() || onFreebsd() || onLinux() || onMac() || onOpenbsd() || onSolaris() || onWindows()));
	}

	/**
	 * @see DisabledOnOsIntegrationTests#architectureX86_64()
	 */
	@Test
	void architectureX86_64() {
		evaluateCondition();
		assertDisabledOnCurrentArchitectureIf(onArchitecture("x86_64"));
	}

	/**
	 * @see DisabledOnOsIntegrationTests#architectureAarch64()
	 */
	@Test
	void architectureAarch64() {
		evaluateCondition();
		assertDisabledOnCurrentArchitectureIf(onArchitecture("aarch64"));
	}

	/**
	 * @see EnabledOnOsIntegrationTests#architectureX86_64WithMacOs()
	 */
	@Test
	void architectureX86_64WithMacOs() {
		evaluateCondition();
		assertDisabledOnCurrentOsAndArchitectureIf(onMac() && onArchitecture("x86_64"));
	}

	/**
	 * @see EnabledOnOsIntegrationTests#architectureX86_64WithWindows()
	 */
	@Test
	void architectureX86_64WithWindows() {
		evaluateCondition();
		assertDisabledOnCurrentOsAndArchitectureIf(onWindows() && onArchitecture("x86_64"));
	}

	/**
	 * @see EnabledOnOsIntegrationTests#architectureX86_64WithLinux()
	 */
	@Test
	void architectureX86_64WithLinux() {
		evaluateCondition();
		assertDisabledOnCurrentOsAndArchitectureIf(onLinux() && onArchitecture("x86_64"));
	}

	/**
	 * @see EnabledOnOsIntegrationTests#aarch64WithMacOs()
	 */
	@Test
	void aarch64WithMacOs() {
		evaluateCondition();
		assertDisabledOnCurrentOsAndArchitectureIf(onMac() && onArchitecture("aarch64"));
	}

	/**
	 * @see EnabledOnOsIntegrationTests#aarch64WithWindows()
	 */
	@Test
	void aarch64WithWindows() {
		evaluateCondition();
		assertDisabledOnCurrentOsAndArchitectureIf(onWindows() && onArchitecture("aarch64"));
	}

	/**
	 * @see EnabledOnOsIntegrationTests#aarch64WithLinux()
	 */
	@Test
	void aarch64WithLinux() {
		evaluateCondition();
		assertDisabledOnCurrentOsAndArchitectureIf(onLinux() && onArchitecture("aarch64"));
	}

	private void assertDisabledOnCurrentOsIf(boolean condition) {
		if (condition) {
			assertDisabled();
			assertReasonContains("Disabled on operating system: %s".formatted(OS_NAME));
		}
		else {
			assertEnabled();
			assertReasonContains("Enabled on operating system: %s".formatted(OS_NAME));
		}
	}

	private void assertDisabledOnCurrentArchitectureIf(boolean condition) {
		if (condition) {
			assertDisabled();
			assertReasonContains("Disabled on architecture: %s".formatted(ARCH));
		}
		else {
			assertEnabled();
			assertReasonContains("Enabled on architecture: %s".formatted(ARCH));
		}
	}

	private void assertDisabledOnCurrentOsAndArchitectureIf(boolean condition) {
		if (condition) {
			assertDisabled();
			assertReasonContains("Disabled on operating system: %s (%s)".formatted(OS_NAME, ARCH));
		}
		else {
			assertEnabled();
			assertReasonContains("Enabled on operating system: %s (%s)".formatted(OS_NAME, ARCH));
		}
	}

}
