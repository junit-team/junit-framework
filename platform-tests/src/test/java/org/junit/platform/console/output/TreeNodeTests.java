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

import static java.util.concurrent.TimeUnit.SECONDS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ThreadPoolExecutor;

import org.junit.jupiter.api.Test;
import org.junit.platform.engine.reporting.ReportEntry;

class TreeNodeTests {

	private static final int NUM_THREADS = 2;
	private static final int ITEMS_PER_THREAD = 1000;

	@Test
	void caption() {
		assertEquals("", TreeNode.createCaption(""));
		assertEquals("[@@]", TreeNode.createCaption("[@@]"));
		assertEquals("[@ @]", TreeNode.createCaption("[@ @]"));
		assertEquals("[@ @]", TreeNode.createCaption("[@\u000B@]"));
		assertEquals("[@ @]", TreeNode.createCaption("[@\t@]"));
		assertEquals("[@  @]", TreeNode.createCaption("[@\t\n@]"));
		assertEquals("[@   @]", TreeNode.createCaption("[@\t\n\r@]"));
		assertEquals("[@    @]", TreeNode.createCaption("[@\t\n\r\f@]"));
		assertEquals("@".repeat(80) + "...", TreeNode.createCaption("@".repeat(1000) + "!"));
	}

	@Test
	void childrenCanBeAddedConcurrently() throws Exception {
		var treeNode = new TreeNode("root");

		runConcurrently(() -> {
			for (long i = 0; i < ITEMS_PER_THREAD; i++) {
				treeNode.addChild(new TreeNode(String.valueOf(i)));
			}
		});

		assertThat(treeNode.children).hasSize(NUM_THREADS * ITEMS_PER_THREAD);
	}

	@Test
	void reportEntriesCanBeAddedConcurrently() throws Exception {
		var treeNode = new TreeNode("root");

		runConcurrently(() -> {
			for (long i = 0; i < ITEMS_PER_THREAD; i++) {
				treeNode.addReportEntry(ReportEntry.from("index", String.valueOf(i)));
			}
		});

		assertThat(treeNode.reports).hasSize(NUM_THREADS * ITEMS_PER_THREAD);
	}

	@SuppressWarnings("resource")
	private void runConcurrently(Runnable action) throws InterruptedException {
		ExecutorService executor = new ThreadPoolExecutor(NUM_THREADS, NUM_THREADS, 10, SECONDS,
			new ArrayBlockingQueue<>(NUM_THREADS));
		try {
			var barrier = new CyclicBarrier(NUM_THREADS);
			for (long i = 0; i < NUM_THREADS; i++) {
				executor.submit(() -> {
					await(barrier);
					action.run();
				});
			}
		}
		finally {
			executor.shutdown();
			var terminated = executor.awaitTermination(10, SECONDS);
			assertTrue(terminated, "Executor was not terminated");
		}
	}

	private void await(CyclicBarrier barrier) {
		try {
			barrier.await();
		}
		catch (Exception e) {
			throw new RuntimeException(e);
		}
	}
}
