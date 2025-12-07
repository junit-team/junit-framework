/*
 * Copyright 2015-2025 the original author or authors.
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v2.0 which
 * accompanies this distribution and is available at
 *
 * https://www.eclipse.org/legal/epl-v20.html
 */

package org.junit.jupiter.api.util;

import java.util.Locale;
import java.util.Optional;

import org.junit.jupiter.api.extension.AfterEachCallback;
import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.BeforeEachCallback;
import org.junit.jupiter.api.extension.ExtensionConfigurationException;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.ExtensionContext.Namespace;
import org.junit.platform.commons.support.AnnotationSupport;
import org.junit.platform.commons.support.ReflectionSupport;

/**
 * @since 6.1
 */
final class DefaultLocaleExtension implements BeforeAllCallback, BeforeEachCallback, AfterEachCallback {

	private static final Namespace NAMESPACE = Namespace.create(DefaultLocaleExtension.class);

	private static final String CUSTOM_KEY = "CustomLocale";
	private static final String DEFAULT_KEY = "DefaultLocale";

	@Override
	public void beforeAll(ExtensionContext context) {
		createLocaleFromAnnotation(context) //
				.ifPresent(locale -> store(context, CUSTOM_KEY, locale));
	}

	@Override
	public void beforeEach(ExtensionContext context) {
		createLocaleFromAnnotation(context) //
				.or(() -> load(context, CUSTOM_KEY)) //
				.ifPresent(locale -> setDefaultLocale(context, locale));
	}

	private void setDefaultLocale(ExtensionContext context, Locale customLocale) {
		store(context, DEFAULT_KEY, Locale.getDefault());
		Locale.setDefault(customLocale);
	}

	private static Optional<Locale> createLocaleFromAnnotation(ExtensionContext context) {
		return AnnotationSupport.findAnnotation(context.getElement(), DefaultLocale.class) //
				.map(DefaultLocaleExtension::createLocale);
	}

	private static Locale createLocale(DefaultLocale annotation) {
		if (!annotation.value().isEmpty()) {
			return createFromLanguageTag(annotation);
		}
		else if (!annotation.language().isEmpty()) {
			return createFromParts(annotation);
		}
		else {
			return getFromProvider(annotation);
		}
	}

	private static Locale createFromLanguageTag(DefaultLocale annotation) {
		if (!annotation.language().isEmpty() || !annotation.country().isEmpty() || !annotation.variant().isEmpty()
				|| annotation.localeProvider() != NullLocaleProvider.class) {
			throw new ExtensionConfigurationException(
				"@DefaultLocale can only be used with language tag if language, country, variant and provider are not set");
		}
		return Locale.forLanguageTag(annotation.value());
	}

	private static Locale createFromParts(DefaultLocale annotation) {
		if (annotation.localeProvider() != NullLocaleProvider.class)
			throw new ExtensionConfigurationException(
				"@DefaultLocale can only be used with language tag if provider is not set");
		String language = annotation.language();
		String country = annotation.country();
		String variant = annotation.variant();
		if (!language.isEmpty() && !country.isEmpty() && !variant.isEmpty()) {
			return JupiterLocaleUtils.createLocale(language, country, variant);
		}
		else if (!language.isEmpty() && !country.isEmpty()) {
			return JupiterLocaleUtils.createLocale(language, country);
		}
		else if (!language.isEmpty() && variant.isEmpty()) {
			return JupiterLocaleUtils.createLocale(language);
		}
		else {
			throw new ExtensionConfigurationException(
				"@DefaultLocale not configured correctly. When not using a language tag, specify either"
						+ " language, or language and country, or language and country and variant.");
		}
	}

	private static Locale getFromProvider(DefaultLocale annotation) {
		if (!annotation.country().isEmpty() || !annotation.variant().isEmpty())
			throw new ExtensionConfigurationException(
				"@DefaultLocale can only be used with a provider if value, language, country and variant are not set.");
		var providerClass = annotation.localeProvider();
		LocaleProvider provider;
		try {
			provider = ReflectionSupport.newInstance(providerClass);
		}
		catch (Exception exception) {
			throw new ExtensionConfigurationException(
				"LocaleProvider instance could not be constructed because of an exception", exception);
		}
		return invoke(provider);
	}

	@SuppressWarnings("ConstantValue")
	private static Locale invoke(LocaleProvider provider) {
		var locale = provider.get();
		if (locale == null) {
			throw new ExtensionConfigurationException("LocaleProvider instance returned with null");
		}
		return locale;
	}

	@Override
	public void afterEach(ExtensionContext context) {
		load(context, DEFAULT_KEY).ifPresent(Locale::setDefault);
	}

	private static void store(ExtensionContext context, String key, Locale value) {
		getStore(context).put(key, value);
	}

	private static Optional<Locale> load(ExtensionContext context, String key) {
		return Optional.ofNullable(getStore(context).get(key, Locale.class));
	}

	private static ExtensionContext.Store getStore(ExtensionContext context) {
		return context.getStore(NAMESPACE);
	}

}
