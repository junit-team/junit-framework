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

import static java.util.Objects.requireNonNull;
import static java.util.Objects.requireNonNullElse;
import static org.apiguardian.api.API.Status.INTERNAL;

import java.io.PrintWriter;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.apiguardian.api.API;
import org.jspecify.annotations.Nullable;
import org.junit.platform.engine.TestExecutionResult;
import org.junit.platform.engine.UniqueId;
import org.junit.platform.engine.reporting.FileEntry;
import org.junit.platform.engine.reporting.ReportEntry;
import org.junit.platform.launcher.TestIdentifier;
import org.junit.platform.launcher.TestPlan;

/**
 * @since 1.0
 */
@API(status = INTERNAL, since = "1.14")
public class TreePrintingListener implements DetailsPrintingListener {

	private final Map<UniqueId, TreeNode> nodesByUniqueId = new ConcurrentHashMap<>();
	private final TreePrinter treePrinter;

	private @Nullable TreeNode root;

	public TreePrintingListener(PrintWriter out, ColorPalette colorPalette, Theme theme) {
		this.treePrinter = new TreePrinter(out, theme, colorPalette);
	}

	private void addNode(TestIdentifier testIdentifier, TreeNode node) {
		nodesByUniqueId.put(testIdentifier.getUniqueIdObject(), node);
		TreeNode parent = testIdentifier.getParentIdObject().map(nodesByUniqueId::get).orElse(null);
		requireNonNullElse(parent, root).addChild(node);
	}

	private TreeNode getNode(TestIdentifier testIdentifier) {
		return requireNonNull(nodesByUniqueId.get(testIdentifier.getUniqueIdObject()));
	}

	@Override
	public void testPlanExecutionStarted(TestPlan testPlan) {
		root = new TreeNode(testPlan.toString());
	}

	@Override
	public void testPlanExecutionFinished(TestPlan testPlan) {
		treePrinter.print(requireNonNull(root));
	}

	@Override
	public void executionStarted(TestIdentifier testIdentifier) {
		addNode(testIdentifier, new TreeNode(testIdentifier));
	}

	@Override
	public void executionFinished(TestIdentifier testIdentifier, TestExecutionResult testExecutionResult) {
		getNode(testIdentifier).setResult(testExecutionResult);
	}

	@Override
	public void executionSkipped(TestIdentifier testIdentifier, String reason) {
		addNode(testIdentifier, new TreeNode(testIdentifier, reason));
	}

	@Override
	public void reportingEntryPublished(TestIdentifier testIdentifier, ReportEntry entry) {
		getNode(testIdentifier).addReportEntry(entry);
	}

	@Override
	public void fileEntryPublished(TestIdentifier testIdentifier, FileEntry file) {
		getNode(testIdentifier).addFileEntry(file);
	}

	@Override
	public void listTests(TestPlan testPlan) {
		root = new TreeNode(testPlan.toString());
		testPlan.accept(new TestPlan.Visitor() {
			@Override
			public void visit(TestIdentifier testIdentifier) {
				addNode(testIdentifier, new TreeNode(testIdentifier));
			}
		});
		treePrinter.print(root);
	}
}
