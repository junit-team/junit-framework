/*
 * Copyright 2015-2026 the original author or authors.
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v2.0 which
 * accompanies this distribution and is available at
 *
 * https://www.eclipse.org/legal/epl-v20.html
 */

package org.junit.platform.commons.test;

import static java.util.concurrent.TimeUnit.SECONDS;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.jspecify.annotations.Nullable;

public class ConcurrencyTestingUtils {

	public static void executeConcurrently(int threads, Runnable action) throws Exception {
		ConcurrencyTestingUtils.<@Nullable Object> executeConcurrently(threads, () -> {
			action.run();
			return null;
		});
	}

	@SuppressWarnings("Finally")
	public static <T extends @Nullable Object> List<T> executeConcurrently(int threads, Callable<T> action)
			throws Exception {
		ExecutorService executorService = Executors.newFixedThreadPool(threads);
		try {
			CountDownLatch latch = new CountDownLatch(threads);
			List<CompletableFuture<T>> futures = new ArrayList<>();
			for (int i = 0; i < threads; i++) {
				futures.add(CompletableFuture.supplyAsync(() -> {
					try {
						latch.countDown();
						latch.await();
						return action.call();
					}
					catch (InterruptedException e) {
						Thread.currentThread().interrupt();
						throw new CompletionException(e);
					}
					catch (Exception e) {
						throw new CompletionException("Action failed", e);
					}
				}, executorService));
			}
			List<T> list = new ArrayList<>();
			for (CompletableFuture<T> future : futures) {
				list.add(future.get(5, SECONDS));
			}
			return list;
		}
		finally {
			executorService.shutdownNow();
			var terminated = executorService.awaitTermination(5, SECONDS);
			if (!terminated) {
				//noinspection ThrowFromFinallyBlock
				throw new AssertionError("ExecutorService did not cleanly shut down");
			}
		}
	}

	private ConcurrencyTestingUtils() {
	}
}
