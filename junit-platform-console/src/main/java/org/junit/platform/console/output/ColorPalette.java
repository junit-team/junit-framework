/*
 * Copyright 2015-2026 the original author or authors.
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v2.0 which
 * accompanies this distribution and is available at
 *
 * https://www.eclipse.org/legal/epl-v20.html
 */

package org.junit.platform.console.output;

import static org.apiguardian.api.API.Status.INTERNAL;

import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.EnumMap;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.apiguardian.api.API;

/**
 * @since 1.9
 */
@API(status = INTERNAL, since = "1.14")
public class ColorPalette {

	public static final ColorPalette SINGLE_COLOR = new ColorPalette(singleColorPalette(), false);
	public static final ColorPalette DEFAULT = new ColorPalette(defaultPalette(), false);
	public static final ColorPalette NONE = new ColorPalette(new EnumMap<>(Style.class), true);

	private final Map<Style, String> colorsToAnsiSequences;
	private final boolean disableAnsiColors;

	// https://en.wikipedia.org/wiki/ANSI_escape_code#SGR_parameters
	private static Map<Style, String> defaultPalette() {
		Map<Style, String> colorsToAnsiSequences = new EnumMap<>(Style.class);
		colorsToAnsiSequences.put(Style.NONE, "0");
		colorsToAnsiSequences.put(Style.SUCCESSFUL, "32");
		colorsToAnsiSequences.put(Style.ABORTED, "33");
		colorsToAnsiSequences.put(Style.FAILED, "31");
		colorsToAnsiSequences.put(Style.SKIPPED, "35");
		colorsToAnsiSequences.put(Style.CONTAINER, "36");
		colorsToAnsiSequences.put(Style.TEST, "34");
		colorsToAnsiSequences.put(Style.DYNAMIC, "35");
		colorsToAnsiSequences.put(Style.REPORTED, "37");
		return colorsToAnsiSequences;
	}

	private static Map<Style, String> singleColorPalette() {
		Map<Style, String> colorsToAnsiSequences = new EnumMap<>(Style.class);
		colorsToAnsiSequences.put(Style.NONE, "0");
		colorsToAnsiSequences.put(Style.SUCCESSFUL, "1");
		colorsToAnsiSequences.put(Style.ABORTED, "4");
		colorsToAnsiSequences.put(Style.FAILED, "7");
		colorsToAnsiSequences.put(Style.SKIPPED, "9");
		colorsToAnsiSequences.put(Style.CONTAINER, "1");
		colorsToAnsiSequences.put(Style.TEST, "0");
		colorsToAnsiSequences.put(Style.DYNAMIC, "0");
		colorsToAnsiSequences.put(Style.REPORTED, "2");
		return colorsToAnsiSequences;
	}

	ColorPalette(Map<Style, String> overrides) {
		this(defaultPalette(), false);

		if (overrides.containsKey(Style.NONE)) {
			throw new IllegalArgumentException("Cannot override the standard style 'NONE'");
		}
		this.colorsToAnsiSequences.putAll(overrides);
	}

	ColorPalette(Properties properties) {
		this(toOverrideMap(properties));
	}

	ColorPalette(Reader reader) {
		this(getProperties(reader));
	}

	public ColorPalette(Path path) {
		this(getProperties(path));
	}

	private ColorPalette(Map<Style, String> colorsToAnsiSequences, boolean disableAnsiColors) {
		this.colorsToAnsiSequences = colorsToAnsiSequences;
		this.disableAnsiColors = disableAnsiColors;
	}

	private static Map<Style, String> toOverrideMap(Properties properties) {
		Map<String, String> upperCaseProperties = properties.entrySet().stream().collect(Collectors.toMap(
			entry -> ((String) entry.getKey()).toUpperCase(Locale.ROOT), entry -> (String) entry.getValue()));

		return Arrays.stream(Style.values()).filter(style -> upperCaseProperties.containsKey(style.name())).collect(
			Collectors.toMap(Function.identity(), style -> upperCaseProperties.get(style.name())));
	}

	private static Properties getProperties(Reader reader) {
		Properties properties = new Properties();
		try {
			properties.load(reader);
		}
		catch (IOException e) {
			throw new IllegalArgumentException("Could not read color palette properties", e);
		}
		return properties;
	}

	private static Properties getProperties(Path path) {
		try (FileReader fileReader = new FileReader(path.toFile(), StandardCharsets.UTF_8)) {
			return getProperties(fileReader);
		}
		catch (IOException e) {
			throw new IllegalArgumentException("Could not open color palette properties file", e);
		}
	}

	public String paint(Style style, String text) {
		return this.disableAnsiColors || style == Style.NONE ? text
				: getAnsiFormatter(style) + text + getAnsiFormatter(Style.NONE);
	}

	private String getAnsiFormatter(Style style) {
		return "\u001B[%sm".formatted(this.colorsToAnsiSequences.get(style));
	}

}
