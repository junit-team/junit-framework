/*
 * Copyright 2015-2025 the original author or authors.
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v2.0 which
 * accompanies this distribution and is available at
 *
 * https://www.eclipse.org/legal/epl-v20.html
 */

package org.junit.jupiter.engine.extension;

import java.lang.reflect.AnnotatedElement;
import java.util.Locale;

import org.junit.jupiter.api.extension.AfterEachCallback;
import org.junit.jupiter.api.extension.BeforeEachCallback;
import org.junit.jupiter.api.extension.ExtensionConfigurationException;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.ExtensionContext.Namespace;
import org.junit.jupiter.api.util.DefaultLocale;
import org.junit.jupiter.api.util.LocaleProvider;
import org.junit.jupiter.api.util.LocaleProvider.NullLocaleProvider;
import org.junit.jupiter.engine.support.JupiterLocaleUtils;
import org.junit.platform.commons.support.AnnotationSupport;
import org.junit.platform.commons.support.ReflectionSupport;

class DefaultLocaleExtension implements BeforeEachCallback, AfterEachCallback {

	private static final Namespace NAMESPACE = Namespace.create(DefaultLocaleExtension.class);

	private static final String KEY = "DefaultLocale";

	@Override
	public void beforeEach(ExtensionContext context) {
		AnnotatedElement element = context.getElement().orElse(null);
		AnnotationSupport.findAnnotation(element, DefaultLocale.class).ifPresent(
			annotation -> setDefaultLocale(context, annotation));
	}

	private void setDefaultLocale(ExtensionContext context, DefaultLocale annotation) {
		Locale configuredLocale = createLocale(annotation);
		// defer storing the current default locale until the new locale could be created from the configuration
		// (this prevents cases where misconfigured extensions store default locale now and restore it later,
		// which leads to race conditions in our tests)
		storeDefaultLocale(context);
		Locale.setDefault(configuredLocale);
	}

	private void storeDefaultLocale(ExtensionContext context) {
		context.getStore(NAMESPACE).put(KEY, Locale.getDefault());
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
		var locale = provider.get();
		if (locale == null)
			throw new NullPointerException("LocaleProvider instance returned with null");
		return locale;
	}

	@Override
	public void afterEach(ExtensionContext context) {
		AnnotatedElement element = context.getElement().orElse(null);
		AnnotationSupport.findAnnotation(element, DefaultLocale.class).ifPresent(__ -> resetDefaultLocale(context));
	}

	private void resetDefaultLocale(ExtensionContext context) {
		Locale defaultLocale = context.getStore(NAMESPACE).get(KEY, Locale.class);
		// default locale is null if the extension was misconfigured and execution failed in "before"
		if (defaultLocale != null)
			Locale.setDefault(defaultLocale);
	}

}
