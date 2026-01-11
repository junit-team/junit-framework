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

import static org.junit.platform.commons.util.CollectionUtils.getOnlyElement;

import java.io.PrintWriter;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import org.junit.platform.commons.util.StringUtils;
import org.junit.platform.engine.TestExecutionResult;
import org.junit.platform.engine.TestExecutionResult.Status;
import org.junit.platform.engine.reporting.FileEntry;
import org.junit.platform.engine.reporting.ReportEntry;

/**
 * @since 1.0
 */
class TreePrinter {

	private final PrintWriter out;
	private final Theme theme;
	private final ColorPalette colorPalette;

	TreePrinter(PrintWriter out, Theme theme, ColorPalette colorPalette) {
		this.out = out;
		this.theme = theme;
		this.colorPalette = colorPalette;
	}

	void print(TreeNode node) {
		out.println(color(Style.CONTAINER, theme.root()));
		print(node, "", true);
		out.flush();
	}

	private void print(TreeNode node, String indent, boolean continuous) {
		if (node.visible) {
			printVisible(node, indent, continuous);
		}
		if (node.children.isEmpty()) {
			return;
		}
		if (node.visible) {
			indent += continuous ? theme.vertical() : theme.blank();
		}
		Iterator<TreeNode> iterator = node.children.iterator();
		while (iterator.hasNext()) {
			print(iterator.next(), indent, iterator.hasNext());
		}
	}

	private void printVisible(TreeNode node, String indent, boolean continuous) {
		String bullet = continuous ? theme.entry() : theme.end();
		String prefix = color(Style.CONTAINER, indent + bullet);
		String tabbed = color(Style.CONTAINER, indent + tab(node, continuous));
		String caption = colorCaption(node);
		String duration = color(Style.CONTAINER, node.duration + " ms");
		String icon = color(Style.SKIPPED, theme.skipped());
		if (node.result().isPresent()) {
			TestExecutionResult result = node.result().get();
			Style resultStyle = Style.valueOf(result);
			icon = color(resultStyle, theme.status(result));
		}
		out.print(prefix);
		out.print(" ");
		out.print(caption);
		if (node.duration > 10000 && node.children.isEmpty()) {
			out.print(" ");
			out.print(duration);
		}
		boolean nodeIsBeingListed = node.duration == 0 && node.result().isEmpty() && node.reason().isEmpty();
		if (!nodeIsBeingListed) {
			out.print(" ");
			out.print(icon);
		}
		node.result().ifPresent(result -> printThrowable(tabbed, result));
		node.reason().ifPresent(reason -> printMessage(Style.SKIPPED, tabbed, reason));
		node.reports.forEach(e -> printReportEntry(tabbed, e));
		out.println();
		node.files.forEach(e -> printFileEntry(tabbed, e));
	}

	private String tab(TreeNode node, boolean continuous) {
		// We might be the "last" node in this level, that means
		// `continuous == false`, but still need to include a vertical
		// bar for printing stack traces, messages and reports.
		// See https://github.com/junit-team/junit-framework/issues/1531
		if (node.children.size() > 0) {
			return theme.blank() + theme.vertical();
		}
		return (continuous ? theme.vertical() : theme.blank()) + theme.blank();
	}

	private String colorCaption(TreeNode node) {
		String caption = node.caption();
		if (node.result().isPresent()) {
			TestExecutionResult result = node.result().get();
			Style resultStyle = Style.valueOf(result);
			if (result.getStatus() != Status.SUCCESSFUL) {
				return color(resultStyle, caption);
			}
		}
		if (node.reason().isPresent()) {
			return color(Style.SKIPPED, caption);
		}
		Style style = node.identifier().map(Style::valueOf).orElse(Style.NONE);
		return color(style, caption);
	}

	private void printThrowable(String indent, TestExecutionResult result) {
		if (result.getThrowable().isEmpty()) {
			return;
		}
		Throwable throwable = result.getThrowable().get();
		String message = throwable.getMessage();
		if (StringUtils.isBlank(message)) {
			message = throwable.toString();
		}
		printMessage(Style.FAILED, indent, message);
	}

	private void printReportEntry(String indent, ReportEntry reportEntry) {
		out.println();
		out.print(indent);
		out.print(reportEntry.getTimestamp());
		Set<Map.Entry<String, String>> entries = reportEntry.getKeyValuePairs().entrySet();
		if (entries.size() == 1) {
			printReportEntry(" ", getOnlyElement(entries));
			return;
		}
		for (Map.Entry<String, String> entry : entries) {
			out.println();
			printReportEntry(indent + theme.blank(), entry);
		}
	}

	private void printReportEntry(String indent, Map.Entry<String, String> mapEntry) {
		out.print(indent);
		out.print(color(Style.ABORTED, mapEntry.getKey()));
		out.print(" = `");
		out.print(color(Style.SUCCESSFUL, mapEntry.getValue()));
		out.print("`");
	}

	private void printFileEntry(String indent, FileEntry fileEntry) {
		out.print(indent);
		out.print(fileEntry.getTimestamp());
		out.print(" ");
		out.print(color(Style.SUCCESSFUL, fileEntry.getPath().toUri().toString()));
		out.println();
	}

	private void printMessage(Style style, String indent, String message) {
		String[] lines = message.split("\\R");
		out.print(" ");
		out.print(color(style, lines[0]));
		if (lines.length > 1) {
			for (int i = 1; i < lines.length; i++) {
				out.println();
				out.print(indent);
				if (StringUtils.isNotBlank(lines[i])) {
					String extra = theme.blank();
					out.print(color(style, extra + lines[i]));
				}
			}
		}
	}

	private String color(Style style, String text) {
		return colorPalette.paint(style, text);
	}

}
