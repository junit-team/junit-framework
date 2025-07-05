/*
 * Copyright 2015-2025 the original author or authors.
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v2.0 which
 * accompanies this distribution and is available at
 *
 * https://www.eclipse.org/legal/epl-v20.html
 */

package org.junit.jupiter.params.converter;

import java.util.Locale;

import org.jspecify.annotations.Nullable;
import org.junit.platform.commons.support.conversion.TypedConverter;

// FIXME move to ConversionSupportIntegrationTests
public class LocaleConverter extends TypedConverter<String, Locale> {

	public LocaleConverter() {
		super(String.class, Locale.class);
	}

	@Override
	protected @Nullable Locale convert(@Nullable String source) {
		return source != null ? Locale.forLanguageTag(source) : null;
	}

}
