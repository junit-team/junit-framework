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

import java.util.Optional;
import java.util.TimeZone;

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
final class DefaultTimeZoneExtension implements BeforeAllCallback, BeforeEachCallback, AfterEachCallback {

	private static final Namespace NAMESPACE = Namespace.create(DefaultTimeZoneExtension.class);

	private static final String CUSTOM_KEY = "CustomTimeZone";
	private static final String DEFAULT_KEY = "DefaultTimeZone";

	@Override
	public void beforeAll(ExtensionContext context) {
		createTimeZoneFromAnnotation(context) //
				.ifPresent(timeZone -> store(context, CUSTOM_KEY, timeZone));
	}

	@Override
	public void beforeEach(ExtensionContext context) {
		createTimeZoneFromAnnotation(context) //
				.or(() -> load(context, CUSTOM_KEY)) //
				.ifPresent(timeZone -> setDefaultTimeZone(context, timeZone));
	}

	private void setDefaultTimeZone(ExtensionContext context, TimeZone customTimeZone) {
		store(context, DEFAULT_KEY, TimeZone.getDefault());
		TimeZone.setDefault(customTimeZone);
	}

	private static Optional<TimeZone> createTimeZoneFromAnnotation(ExtensionContext context) {
		return AnnotationSupport.findAnnotation(context.getElement(), DefaultTimeZone.class) //
				.map(DefaultTimeZoneExtension::createTimeZone);
	}

	private static TimeZone createTimeZone(DefaultTimeZone annotation) {
		validateCorrectConfiguration(annotation);

		if (!annotation.value().isEmpty()) {
			return createTimeZoneFromZoneId(annotation.value());
		}
		else {
			return createTimeZoneFromProvider(annotation.timeZoneProvider());
		}
	}

	private static void validateCorrectConfiguration(DefaultTimeZone annotation) {
		boolean noValue = annotation.value().isEmpty();
		boolean noProvider = annotation.timeZoneProvider() == NullTimeZoneProvider.class;
		if (noValue == noProvider) {
			throw new ExtensionConfigurationException(
				"Either a valid time zone id or a TimeZoneProvider must be provided to "
						+ DefaultTimeZone.class.getSimpleName());
		}
	}

	private static TimeZone createTimeZoneFromZoneId(String timeZoneId) {
		TimeZone configuredTimeZone = TimeZone.getTimeZone(timeZoneId);
		// TimeZone::getTimeZone returns with GMT as fallback if the given ID cannot be understood
		if (configuredTimeZone.equals(TimeZone.getTimeZone("GMT")) && !"GMT".equals(timeZoneId)) {
			throw new ExtensionConfigurationException("""
					@DefaultTimeZone not configured correctly.
					Could not find the specified time zone + '%s'.
					Please use correct identifiers, e.g. "GMT" for Greenwich Mean Time.
					""".formatted(timeZoneId));
		}
		return configuredTimeZone;
	}

	private static TimeZone createTimeZoneFromProvider(Class<? extends TimeZoneProvider> providerClass) {
		try {
			TimeZoneProvider provider = ReflectionSupport.newInstance(providerClass);
			return Optional.ofNullable(provider.get()).orElse(TimeZone.getTimeZone("GMT"));
		}
		catch (Exception exception) {
			throw new ExtensionConfigurationException("Could not instantiate TimeZoneProvider because of exception",
				exception);
		}
	}

	@Override
	public void afterEach(ExtensionContext context) {
		load(context, DEFAULT_KEY).ifPresent(TimeZone::setDefault);
	}

	private static void store(ExtensionContext context, String key, TimeZone value) {
		getStore(context).put(key, value);
	}

	private static Optional<TimeZone> load(ExtensionContext context, String key) {
		return Optional.ofNullable(getStore(context).get(key, TimeZone.class));
	}

	private static ExtensionContext.Store getStore(ExtensionContext context) {
		return context.getStore(NAMESPACE);
	}

}
