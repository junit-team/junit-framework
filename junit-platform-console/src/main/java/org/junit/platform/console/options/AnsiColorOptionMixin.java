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
import static picocli.CommandLine.Help.defaultColorScheme;
import static picocli.CommandLine.Spec.Target.MIXEE;

import org.apiguardian.api.API;

import picocli.CommandLine.Help.Ansi;
import picocli.CommandLine.Model.CommandSpec;
import picocli.CommandLine.Option;
import picocli.CommandLine.Spec;

@API(status = INTERNAL, since = "1.14")
public class AnsiColorOptionMixin {

	@Spec(MIXEE)
	CommandSpec commandSpec;

	// https://no-color.org
	// ANSI is disabled when environment variable NO_COLOR is defined (regardless of its value).
	private boolean disableAnsiColors = System.getenv("NO_COLOR") != null;

	public boolean isDisableAnsiColors() {
		return this.disableAnsiColors;
	}

	@Option(names = "--disable-ansi-colors", description = "Disable ANSI colors in output (not supported by all terminals).")
	public void setDisableAnsiColors(boolean disableAnsiColors) {
		if (disableAnsiColors) {
			this.commandSpec.commandLine().setColorScheme(defaultColorScheme(Ansi.OFF));
		}
		this.disableAnsiColors = disableAnsiColors;
	}

}
