/*
 * Copyright 2015-2026 the original author or authors.
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v2.0 which
 * accompanies this distribution and is available at
 *
 * https://www.eclipse.org/legal/epl-v20.html
 */

package org.junit.jupiter.api.util;

import java.util.Locale;

/**
 * Utility class to create {@code Locale}.
 *
 * @since 6.1
 */
final class JupiterLocaleUtils {

	private JupiterLocaleUtils() {
		// private constructor to prevent instantiation of utility class
	}

	public static Locale createLocale(String language, String country, String variant) {
		return new Locale.Builder().setLanguage(language).setRegion(country).setVariant(variant).build();
	}

	public static Locale createLocale(String language, String country) {
		return new Locale.Builder().setLanguage(language).setRegion(country).build();
	}

	public static Locale createLocale(String language) {
		return new Locale.Builder().setLanguage(language).build();
	}

}
