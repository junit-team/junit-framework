/*
 * Copyright 2015-2026 the original author or authors.
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v2.0 which
 * accompanies this distribution and is available at
 *
 * https://www.eclipse.org/legal/epl-v20.html
 */

package org.junit.jupiter.engine.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.engine.Constants.DEFAULT_DISPLAY_NAME_GENERATOR_PROPERTY_NAME;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.LogRecord;

import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.fixtures.TrackLogRecords;
import org.junit.jupiter.engine.descriptor.CustomDisplayNameGenerator;
import org.junit.platform.commons.logging.LogRecordListener;
import org.junit.platform.engine.ConfigurationParameters;

/**
 * @since 5.5
 */
@TrackLogRecords
class InstantiatingConfigurationParameterConverterTests {

	private static final String KEY = DEFAULT_DISPLAY_NAME_GENERATOR_PROPERTY_NAME;

	@Test
	void shouldInstantiateConfiguredClass(LogRecordListener listener) {

		ConfigurationParameters configurationParameters = mock();
		when(configurationParameters.get(KEY)).thenReturn(Optional.of(CustomDisplayNameGenerator.class.getName()));

		InstantiatingConfigurationParameterConverter<DisplayNameGenerator> converter = new InstantiatingConfigurationParameterConverter<>(
			DisplayNameGenerator.class, "display name generator");
		DisplayNameGenerator displayNameGenerator = converter.get(configurationParameters, KEY).orElseThrow();

		assertThat(displayNameGenerator).isInstanceOf(CustomDisplayNameGenerator.class);
		assertExpectedLogMessage(listener, Level.CONFIG,
			"Using default display name generator "
					+ "'org.junit.jupiter.engine.descriptor.CustomDisplayNameGenerator' set via the "
					+ "'junit.jupiter.displayname.generator.default' configuration parameter.");
	}

	@Test
	void shouldReturnEmptyOptionalIfNoConfigurationFound() {
		ConfigurationParameters configurationParameters = mock();
		when(configurationParameters.get(KEY)).thenReturn(Optional.empty());

		InstantiatingConfigurationParameterConverter<DisplayNameGenerator> converter = new InstantiatingConfigurationParameterConverter<>(
			DisplayNameGenerator.class, "display name generator");
		Optional<DisplayNameGenerator> displayNameGenerator = converter.get(configurationParameters, KEY);

		assertThat(displayNameGenerator).isEmpty();
	}

	@Test
	void shouldReturnEmptyOptionalIfConfigurationIsBlank() {
		ConfigurationParameters configurationParameters = mock();
		when(configurationParameters.get(KEY)).thenReturn(Optional.of(""));

		InstantiatingConfigurationParameterConverter<DisplayNameGenerator> converter = new InstantiatingConfigurationParameterConverter<>(
			DisplayNameGenerator.class, "display name generator");
		Optional<DisplayNameGenerator> displayNameGenerator = converter.get(configurationParameters, KEY);

		assertThat(displayNameGenerator).isEmpty();
	}

	@Test
	void shouldTrimAndInstantiateConfiguredClass(LogRecordListener listener) {
		ConfigurationParameters configurationParameters = mock();
		String classNameWithSpaces = " " + CustomDisplayNameGenerator.class.getName() + "  ";
		when(configurationParameters.get(KEY)).thenReturn(Optional.of(classNameWithSpaces));

		InstantiatingConfigurationParameterConverter<DisplayNameGenerator> converter = new InstantiatingConfigurationParameterConverter<>(
			DisplayNameGenerator.class, "display name generator");
		DisplayNameGenerator displayNameGenerator = converter.get(configurationParameters, KEY).orElseThrow();

		assertThat(displayNameGenerator).isInstanceOf(CustomDisplayNameGenerator.class);
		assertExpectedLogMessage(listener, Level.CONFIG,
			"Using default display name generator "
					+ "'org.junit.jupiter.engine.descriptor.CustomDisplayNameGenerator' set via the "
					+ "'junit.jupiter.displayname.generator.default' configuration parameter.");
	}

	@Test
	void shouldReturnEmptyOptionalIfNoClassFound(LogRecordListener listener) {
		ConfigurationParameters configurationParameters = mock();
		when(configurationParameters.get(KEY)).thenReturn(Optional.of("random-string"));

		InstantiatingConfigurationParameterConverter<DisplayNameGenerator> converter = new InstantiatingConfigurationParameterConverter<>(
			DisplayNameGenerator.class, "display name generator");
		Optional<DisplayNameGenerator> displayNameGenerator = converter.get(configurationParameters, KEY);

		assertThat(displayNameGenerator).isEmpty();
		assertExpectedLogMessage(listener, Level.WARNING,
			"Failed to load default display name generator "
					+ "class 'random-string' set via the 'junit.jupiter.displayname.generator.default' "
					+ "configuration parameter. Falling back to default behavior.");
	}

	@Test
	void shouldReturnEmptyOptionalIfClassFoundIsNotATypeOfExpectedType(LogRecordListener listener) {
		ConfigurationParameters configurationParameters = mock();
		when(configurationParameters.get(KEY)).thenReturn(Optional.of(Object.class.getName()));

		InstantiatingConfigurationParameterConverter<DisplayNameGenerator> converter = new InstantiatingConfigurationParameterConverter<>(
			DisplayNameGenerator.class, "display name generator");
		Optional<DisplayNameGenerator> displayNameGenerator = converter.get(configurationParameters, KEY);

		assertThat(displayNameGenerator).isEmpty();
		assertExpectedLogMessage(listener, Level.WARNING,
			"Failed to load default display name generator class 'java.lang.Object' "
					+ "set via the 'junit.jupiter.displayname.generator.default' configuration parameter. "
					+ "Falling back to default behavior.");
	}

	@Test
	void shouldReturnEmptyOptionalIfClassNameIsNotFullyQualified(LogRecordListener listener) {
		ConfigurationParameters configurationParameters = mock();
		when(configurationParameters.get(KEY)).thenReturn(
			Optional.of(CustomDisplayNameGenerator.class.getSimpleName()));

		InstantiatingConfigurationParameterConverter<DisplayNameGenerator> converter = new InstantiatingConfigurationParameterConverter<>(
			DisplayNameGenerator.class, "display name generator");
		Optional<DisplayNameGenerator> displayNameGenerator = converter.get(configurationParameters, KEY);

		assertThat(displayNameGenerator).isEmpty();
		assertExpectedLogMessage(listener, Level.WARNING,
			"Failed to load default display name generator class 'CustomDisplayNameGenerator' "
					+ "set via the 'junit.jupiter.displayname.generator.default' configuration parameter. "
					+ "Falling back to default behavior.");
	}

	private void assertExpectedLogMessage(LogRecordListener listener, Level level, String expectedMessage) {
		// @formatter:off
		assertTrue(listener.stream(level)
				.map(LogRecord::getMessage)
				.anyMatch(expectedMessage::equals));
		// @formatter:on
	}

}
