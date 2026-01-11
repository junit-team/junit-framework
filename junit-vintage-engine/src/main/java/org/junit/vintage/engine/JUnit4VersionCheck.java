/*
 * Copyright 2015-2026 the original author or authors.
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v2.0 which
 * accompanies this distribution and is available at
 *
 * https://www.eclipse.org/legal/epl-v20.html
 */

package org.junit.vintage.engine;

import static org.junit.vintage.engine.descriptor.VintageTestDescriptor.ENGINE_ID;

import java.math.BigDecimal;
import java.util.function.Supplier;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import junit.runner.Version;

import org.junit.platform.commons.JUnitException;
import org.junit.platform.commons.util.UnrecoverableExceptions;

/**
 * @since 5.4
 */
class JUnit4VersionCheck {

	private static final Pattern versionPattern = Pattern.compile("^(\\d+\\.\\d+).*");
	private static final BigDecimal minVersion = new BigDecimal("4.12");

	static void checkSupported() {
		try {
			checkSupported(Version::id);
		}
		catch (NoClassDefFoundError e) {
			throw new JUnitException(
				"Invalid class/module path: junit-vintage-engine is present but junit:junit is not. "
						+ "Please either remove junit-vintage-engine or add junit:junit, or "
						+ "alternatively use an excludeEngines(\"" + ENGINE_ID + "\") filter.");
		}
	}

	static void checkSupported(Supplier<String> versionSupplier) {
		String versionString = readVersion(versionSupplier);
		BigDecimal version = parseVersion(versionString);
		if (version.compareTo(minVersion) < 0) {
			throw new JUnitException("Unsupported version of junit:junit: " + versionString
					+ ". Please upgrade to version " + minVersion + " or later.");
		}
	}

	static BigDecimal parseVersion(String versionString) {
		try {
			Matcher matcher = versionPattern.matcher(versionString);
			if (matcher.matches()) {
				return new BigDecimal(matcher.group(1));
			}
		}
		catch (Exception e) {
			throw new JUnitException("Failed to parse version of junit:junit: " + versionString, e);
		}
		throw new JUnitException("Failed to parse version of junit:junit: " + versionString);
	}

	private static String readVersion(Supplier<String> versionSupplier) {
		try {
			return versionSupplier.get();
		}
		catch (Throwable t) {
			UnrecoverableExceptions.rethrowIfUnrecoverable(t);
			throw new JUnitException("Failed to read version of junit:junit", t);
		}
	}

	private JUnit4VersionCheck() {
	}

}
