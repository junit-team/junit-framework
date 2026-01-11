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

import java.util.Optional;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

import org.jspecify.annotations.Nullable;
import org.junit.platform.commons.util.StringUtils;
import org.junit.platform.engine.TestExecutionResult;
import org.junit.platform.engine.reporting.FileEntry;
import org.junit.platform.engine.reporting.ReportEntry;
import org.junit.platform.launcher.TestIdentifier;

/**
 * @since 1.0
 */
class TreeNode {

	private final String caption;
	private final long creation;
	long duration;

	private @Nullable String reason;

	private @Nullable TestIdentifier identifier;

	private @Nullable TestExecutionResult result;

	final Queue<ReportEntry> reports = new ConcurrentLinkedQueue<>();
	final Queue<FileEntry> files = new ConcurrentLinkedQueue<>();
	final Queue<TreeNode> children = new ConcurrentLinkedQueue<>();
	boolean visible;

	TreeNode(String caption) {
		this.caption = caption;
		this.creation = System.currentTimeMillis();
		this.visible = false;
	}

	TreeNode(TestIdentifier identifier) {
		this(createCaption(identifier.getDisplayName()));
		this.identifier = identifier;
		this.visible = true;
	}

	TreeNode(TestIdentifier identifier, String reason) {
		this(identifier);
		this.reason = reason;
	}

	TreeNode addChild(TreeNode node) {
		children.add(node);
		return this;
	}

	TreeNode addReportEntry(ReportEntry reportEntry) {
		reports.add(reportEntry);
		return this;
	}

	TreeNode addFileEntry(FileEntry file) {
		files.add(file);
		return this;
	}

	TreeNode setResult(TestExecutionResult result) {
		this.result = result;
		this.duration = System.currentTimeMillis() - creation;
		return this;
	}

	public String caption() {
		return caption;
	}

	Optional<String> reason() {
		return Optional.ofNullable(reason);
	}

	Optional<TestExecutionResult> result() {
		return Optional.ofNullable(result);
	}

	Optional<TestIdentifier> identifier() {
		return Optional.ofNullable(identifier);
	}

	@SuppressWarnings("DataFlowIssue")
	static String createCaption(String displayName) {
		boolean normal = displayName.length() <= 80;
		String caption = normal ? displayName : displayName.substring(0, 80) + "...";
		String whites = StringUtils.replaceWhitespaceCharacters(caption, " ");
		return StringUtils.replaceIsoControlCharacters(whites, ".");
	}
}
