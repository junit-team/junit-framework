/*
 * Copyright 2015-2026 the original author or authors.
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v2.0 which
 * accompanies this distribution and is available at
 *
 * https://www.eclipse.org/legal/epl-v20.html
 */

package org.junit.platform.console.options;

import static org.apiguardian.api.API.Status.INTERNAL;

import java.nio.file.Path;
import java.util.Locale;

import org.apiguardian.api.API;
import org.jspecify.annotations.Nullable;
import org.junit.platform.console.output.Theme;

/**
 * @since 1.10
 */
@API(status = INTERNAL, since = "1.10")
public class TestConsoleOutputOptions {

	static final String DEFAULT_DETAILS_NAME = "tree";
	public static final Details DEFAULT_DETAILS = Details.valueOf(DEFAULT_DETAILS_NAME.toUpperCase(Locale.ROOT));
	static final Theme DEFAULT_THEME = Theme.valueOf(ConsoleUtils.charset());

	private boolean ansiColorOutputDisabled;

	private @Nullable Path colorPalettePath;

	private boolean isSingleColorPalette;
	private Details details = DEFAULT_DETAILS;
	private Theme theme = DEFAULT_THEME;

	private @Nullable Path stdoutPath;

	private @Nullable Path stderrPath;

	public boolean isAnsiColorOutputDisabled() {
		return this.ansiColorOutputDisabled;
	}

	public void setAnsiColorOutputDisabled(boolean ansiColorOutputDisabled) {
		this.ansiColorOutputDisabled = ansiColorOutputDisabled;
	}

	public @Nullable Path getColorPalettePath() {
		return colorPalettePath;
	}

	public void setColorPalettePath(@Nullable Path colorPalettePath) {
		this.colorPalettePath = colorPalettePath;
	}

	public boolean isSingleColorPalette() {
		return isSingleColorPalette;
	}

	public void setSingleColorPalette(boolean singleColorPalette) {
		this.isSingleColorPalette = singleColorPalette;
	}

	public Details getDetails() {
		return this.details;
	}

	public void setDetails(Details details) {
		this.details = details;
	}

	public Theme getTheme() {
		return this.theme;
	}

	public void setTheme(Theme theme) {
		this.theme = theme;
	}

	@API(status = INTERNAL, since = "1.13")
	public @Nullable Path getStdoutPath() {
		return this.stdoutPath;
	}

	@API(status = INTERNAL, since = "1.13")
	public void setStdoutPath(@Nullable Path stdoutPath) {
		this.stdoutPath = stdoutPath;
	}

	@API(status = INTERNAL, since = "1.13")
	public @Nullable Path getStderrPath() {
		return this.stderrPath;
	}

	@API(status = INTERNAL, since = "1.13")
	public void setStderrPath(@Nullable Path stderrPath) {
		this.stderrPath = stderrPath;
	}

}
