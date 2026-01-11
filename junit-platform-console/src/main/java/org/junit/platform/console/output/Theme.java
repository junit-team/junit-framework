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

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Locale;

import org.apiguardian.api.API;
import org.junit.platform.engine.TestExecutionResult;

/**
 * @since 1.0
 */
@API(status = INTERNAL, since = "1.0")
public enum Theme {

	/**
	 * ASCII 7-bit characters form the tree branch.
	 *
	 * <p>Example test plan execution tree:
	 * <pre class="code">
	 * +-- engine alpha
	 * | '-- container BEGIN
	 * |   +-- test 00 [OK]
	 * |   '-- test 01 [OK]
	 * '-- engine omega
	 *   +-- container END
	 *   | +-- test 10 [OK]
	 *   | '-- test 11 [A] aborted
	 *   '-- container FINAL
	 *     +-- skipped [S] because
	 *     '-- failing [X] BäMM
	 * </pre>
	 */
	ASCII(".", "| ", "+--", "'--", "[OK]", "[A]", "[X]", "[S]"),

	/**
	 * Unicode (extended ASCII) characters are used to display the test execution tree.
	 *
	 * <p>Example test plan execution tree:
	 * <pre class="code">
	 * ├─ engine alpha ✔
	 * │  └─ container BEGIN ✔
	 * │     ├─ test 00 ✔
	 * │     └─ test 01 ✔
	 * └─ engine omega ✔
	 *    ├─ container END ✔
	 *    │  ├─ test 10 ✔
	 *    │  └─ test 11 ■ aborted
	 *    └─ container FINAL ✔
	 *       ├─ skipped ↷ because
	 *       └─ failing ✘ BäMM
	 * </pre>
	 */
	UNICODE("╷", "│  ", "├─", "└─", "✔", "■", "✘", "↷");

	public static Theme valueOf(Charset charset) {
		if (StandardCharsets.UTF_8.equals(charset)) {
			return UNICODE;
		}
		return ASCII;
	}

	private final String[] tiles;
	private final String blank;

	Theme(String... tiles) {
		this.tiles = tiles;
		this.blank = new String(new char[vertical().length()]).replace('\0', ' ');
	}

	public final String root() {
		return tiles[0];
	}

	public final String vertical() {
		return tiles[1];
	}

	public final String blank() {
		return blank;
	}

	public final String entry() {
		return tiles[2];
	}

	public final String end() {
		return tiles[3];
	}

	public final String successful() {
		return tiles[4];
	}

	public final String aborted() {
		return tiles[5];
	}

	public final String failed() {
		return tiles[6];
	}

	public final String skipped() {
		return tiles[7];
	}

	public final String status(TestExecutionResult result) {
		return switch (result.getStatus()) {
			case SUCCESSFUL -> successful();
			case ABORTED -> aborted();
			case FAILED -> failed();
		};
	}

	/**
	 * Return lower case {@link #name()} for easier usage in help text for
	 * available options.
	 */
	@Override
	public final String toString() {
		return name().toLowerCase(Locale.ROOT);
	}

}
