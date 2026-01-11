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

import static org.junit.jupiter.api.Assertions.assertIterableEquals;
import static org.junit.jupiter.api.Assertions.assertLinesMatch;
import static org.junit.platform.engine.TestExecutionResult.aborted;
import static org.junit.platform.engine.TestExecutionResult.failed;
import static org.junit.platform.engine.TestExecutionResult.successful;

import java.io.ByteArrayOutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.platform.engine.TestExecutionResult;
import org.junit.platform.engine.UniqueId;
import org.junit.platform.engine.reporting.FileEntry;
import org.junit.platform.engine.reporting.ReportEntry;
import org.junit.platform.fakes.TestDescriptorStub;
import org.junit.platform.launcher.TestIdentifier;

class TreePrinterTests {

	private final Charset charset = StandardCharsets.UTF_8;
	private final ByteArrayOutputStream stream = new ByteArrayOutputStream(1000);
	private final PrintWriter out = new PrintWriter(new OutputStreamWriter(stream, charset));

	private List<String> actual() {
		out.flush();
		return List.of(stream.toString(charset).split("\\R"));
	}

	@Test
	void emptyTree() {
		new TreePrinter(out, Theme.UNICODE, ColorPalette.NONE).print(new TreeNode("<root>"));
		assertIterableEquals(List.of("╷"), actual());
	}

	@Test
	void emptyEngines() {
		var root = new TreeNode("<root>");
		root.addChild(new TreeNode(identifier("e-0", "engine zero"), "none"));
		root.addChild(new TreeNode(identifier("e-1", "engine one")).setResult(successful()));
		root.addChild(new TreeNode(identifier("e-2", "engine two")).setResult(failed(null)));
		root.addChild(new TreeNode(identifier("e-3", "engine three")).setResult(aborted(null)));
		new TreePrinter(out, Theme.UNICODE, ColorPalette.NONE).print(root);
		assertIterableEquals( //
			List.of( //
				"╷", //
				"├─ engine zero ↷ none", //
				"├─ engine one ✔", //
				"├─ engine two ✘", //
				"└─ engine three ■"), //
			actual());
	}

	@Test
	// https://github.com/junit-team/junit-framework/issues/786
	void printNodeHandlesNullMessageThrowableGracefully() {
		var result = TestExecutionResult.failed(new NullPointerException());
		var node = new TreeNode(identifier("NPE", "test()")).setResult(result);
		new TreePrinter(out, Theme.ASCII, ColorPalette.NONE).print(node);
		assertLinesMatch(List.of(".", "+-- test() [X] java.lang.NullPointerException"), actual());
	}

	@Test
	// https://github.com/junit-team/junit-framework/issues/1531
	void reportsAreTabbedCorrectly() {
		var root = new TreeNode("<root>");
		var e1 = new TreeNode(identifier("e-1", "engine one")).setResult(successful());
		e1.addReportEntry(ReportEntry.from("key", "e-1"));
		root.addChild(e1);

		var c1 = new TreeNode(identifier("c-1", "class one")).setResult(successful());
		c1.addReportEntry(ReportEntry.from("key", "c-1"));
		e1.addChild(c1);

		var m1 = new TreeNode(identifier("m-1", "method one")).setResult(successful());
		m1.addReportEntry(ReportEntry.from("key", "m-1"));
		c1.addChild(m1);

		var m2 = new TreeNode(identifier("m-2", "method two")).setResult(successful());
		m2.addFileEntry(FileEntry.from(Path.of("test.txt"), "text/plain"));
		c1.addChild(m2);

		new TreePrinter(out, Theme.UNICODE, ColorPalette.NONE).print(root);
		assertLinesMatch(List.of( //
			"╷", //
			"└─ engine one ✔", //
			"   │  ....-..-..T..:...* key = `e-1`", //
			"   └─ class one ✔", //
			"      │  ....-..-..T..:...* key = `c-1`", //
			"      ├─ method one ✔", //
			"      │     ....-..-..T..:...* key = `m-1`", //
			"      └─ method two ✔", //
			"            ....-..-..T..:...* file:.*" //
		), //
			actual());
	}

	private static TestIdentifier identifier(String id, String displayName) {
		var descriptor = new TestDescriptorStub(UniqueId.forEngine(id), displayName);
		return TestIdentifier.from(descriptor);
	}
}
