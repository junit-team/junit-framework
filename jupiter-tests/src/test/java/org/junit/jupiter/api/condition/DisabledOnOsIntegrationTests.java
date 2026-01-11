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

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;
import static org.junit.jupiter.api.condition.EnabledOnOsIntegrationTests.onAix;
import static org.junit.jupiter.api.condition.EnabledOnOsIntegrationTests.onArchitecture;
import static org.junit.jupiter.api.condition.EnabledOnOsIntegrationTests.onFreebsd;
import static org.junit.jupiter.api.condition.EnabledOnOsIntegrationTests.onLinux;
import static org.junit.jupiter.api.condition.EnabledOnOsIntegrationTests.onMac;
import static org.junit.jupiter.api.condition.EnabledOnOsIntegrationTests.onOpenbsd;
import static org.junit.jupiter.api.condition.EnabledOnOsIntegrationTests.onSolaris;
import static org.junit.jupiter.api.condition.EnabledOnOsIntegrationTests.onWindows;
import static org.junit.jupiter.api.condition.OS.AIX;
import static org.junit.jupiter.api.condition.OS.FREEBSD;
import static org.junit.jupiter.api.condition.OS.LINUX;
import static org.junit.jupiter.api.condition.OS.MAC;
import static org.junit.jupiter.api.condition.OS.OPENBSD;
import static org.junit.jupiter.api.condition.OS.OTHER;
import static org.junit.jupiter.api.condition.OS.SOLARIS;
import static org.junit.jupiter.api.condition.OS.WINDOWS;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

/**
 * Integration tests for {@link DisabledOnOs}.
 *
 * @since 5.1
 */
class DisabledOnOsIntegrationTests {

	@Test
	@Disabled("Only used in a unit test via reflection")
	void enabledBecauseAnnotationIsNotPresent() {
	}

	@Test
	@Disabled("Only used in a unit test via reflection")
	@DisabledOnOs({})
	void missingOsAndArchitectureDeclaration() {
	}

	@Test
	@DisabledOnOs(value = { AIX, FREEBSD, LINUX, MAC, OPENBSD, WINDOWS, SOLARIS,
			OTHER }, disabledReason = "Disabled on every OS")
	void disabledOnEveryOs() {
		fail("should be disabled");
	}

	@Test
	@DisabledOnOs(AIX)
	void aix() {
		assertFalse(onAix());
	}

	@Test
	@DisabledOnOs(FREEBSD)
	void freebsd() {
		assertFalse(onFreebsd());
	}

	@Test
	@DisabledOnOs(LINUX)
	void linux() {
		assertFalse(onLinux());
	}

	@Test
	@DisabledOnOs(MAC)
	void macOs() {
		assertFalse(onMac());
	}

	@Test
	@DisabledOnMac
	void macOsWithComposedAnnotation() {
		assertFalse(onMac());
	}

	@Test
	@DisabledOnOs(OPENBSD)
	void openbsd() {
		assertFalse(onOpenbsd());
	}

	@Test
	@DisabledOnOs(WINDOWS)
	void windows() {
		assertFalse(onWindows());
	}

	@Test
	@DisabledOnOs(SOLARIS)
	void solaris() {
		assertFalse(onSolaris());
	}

	@Test
	@DisabledOnOs(OTHER)
	void other() {
		assertTrue(onAix() || onFreebsd() || onLinux() || onMac() || onOpenbsd() || onSolaris() || onWindows());
	}

	@Test
	@DisabledOnOs(architectures = "x86_64")
	void architectureX86_64() {
		assertFalse(onArchitecture("x_86_64"));
	}

	@Test
	@DisabledOnOs(architectures = "aarch64")
	void architectureAarch64() {
		assertFalse(onArchitecture("aarch64"));
	}

	@Test
	@DisabledOnOs(value = MAC, architectures = "x86_64")
	void architectureX86_64WithMacOs() {
		assertFalse(onMac() && onArchitecture("x_86_64"));
	}

	@Test
	@DisabledOnOs(value = WINDOWS, architectures = "x86_64")
	void architectureX86_64WithWindows() {
		assertFalse(onWindows() && onArchitecture("x86_64"));
	}

	@Test
	@DisabledOnOs(value = LINUX, architectures = "x86_64")
	void architectureX86_64WithLinux() {
		assertFalse(onLinux() && onArchitecture("x86_64"));
	}

	@Test
	@DisabledOnOs(value = MAC, architectures = "aarch64")
	void aarch64WithMacOs() {
		assertFalse(onMac() && onArchitecture("aarch64"));
	}

	@Test
	@DisabledOnOs(value = WINDOWS, architectures = "aarch64")
	void aarch64WithWindows() {
		assertFalse(onWindows() && onArchitecture("aarch64"));
	}

	@Test
	@DisabledOnOs(value = LINUX, architectures = "aarch64")
	void aarch64WithLinux() {
		assertFalse(onLinux() && onArchitecture("aarch64"));
	}
	// -------------------------------------------------------------------------

	@Target(ElementType.METHOD)
	@Retention(RetentionPolicy.RUNTIME)
	@DisabledOnOs(MAC)
	@interface DisabledOnMac {
	}

}
