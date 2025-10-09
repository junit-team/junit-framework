/*
 * Copyright 2015-2025 the original author or authors.
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v2.0 which
 * accompanies this distribution and is available at
 *
 * https://www.eclipse.org/legal/epl-v20.html
 */

package org.junit.platform.engine.support.hierarchical;

import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.locks.Lock;
import java.util.stream.IntStream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;
import org.junit.platform.engine.support.hierarchical.ExclusiveResource.LockMode;

/**
 * @since 1.3
 */
class CompositeLockTests {

	@Test
	@SuppressWarnings({ "resource", "ResultOfMethodCallIgnored" })
	void triesToAcquireAllLocksInOrder() {
		var lock1 = mock(Lock.class, "lock1");
		var lock2 = mock(Lock.class, "lock2");

		when(lock1.tryLock()).thenReturn(true);
		when(lock2.tryLock()).thenReturn(true);

		new CompositeLock(anyResources(2), List.of(lock1, lock2)).tryAcquire();

		var inOrder = inOrder(lock1, lock2);
		inOrder.verify(lock1).tryLock();
		inOrder.verify(lock2).tryLock();
	}

	@Test
	@SuppressWarnings("resource")
	void acquiresAllLocksInOrder() throws Exception {
		var lock1 = mock(Lock.class, "lock1");
		var lock2 = mock(Lock.class, "lock2");

		new CompositeLock(anyResources(2), List.of(lock1, lock2)).acquire();

		var inOrder = inOrder(lock1, lock2);
		inOrder.verify(lock1).lockInterruptibly();
		inOrder.verify(lock2).lockInterruptibly();
	}

	@Test
	@SuppressWarnings("resource")
	void releasesAllLocksInReverseOrder() throws Exception {
		var lock1 = mock(Lock.class, "lock1");
		var lock2 = mock(Lock.class, "lock2");

		new CompositeLock(anyResources(2), List.of(lock1, lock2)).acquire().close();

		var inOrder = inOrder(lock1, lock2);
		inOrder.verify(lock2).unlock();
		inOrder.verify(lock1).unlock();
	}

	@Test
	@SuppressWarnings("resource")
	void releasesLocksInReverseOrderWhenInterruptedDuringAcquire() throws Exception {
		var firstTwoLocksWereLocked = new CountDownLatch(2);
		var firstLock = mockLock("firstLock", firstTwoLocksWereLocked::countDown);
		var secondLock = mockLock("secondLock", firstTwoLocksWereLocked::countDown);
		var unavailableLock = mockLock("unavailableLock", new CountDownLatch(1)::await);

		var thread = new Thread(() -> {
			try {
				new CompositeLock(anyResources(3), List.of(firstLock, secondLock, unavailableLock)).acquire();
			}
			catch (InterruptedException e) {
				Thread.currentThread().interrupt();
			}
		});
		thread.start();
		firstTwoLocksWereLocked.await();
		thread.interrupt();
		thread.join();

		var inOrder = inOrder(firstLock, secondLock);
		inOrder.verify(secondLock).unlock();
		inOrder.verify(firstLock).unlock();
		verify(unavailableLock, never()).unlock();
	}

	@Test
	@SuppressWarnings("resource")
	void releasesLocksInReverseOrderOnUnsuccessfulAttempt() {
		var firstLock = mock(Lock.class, "firstLock");
		var secondLock = mock(Lock.class, "secondLock");
		var unavailableLock = mock(Lock.class, "unavailableLock");

		when(firstLock.tryLock()).thenReturn(true);
		when(secondLock.tryLock()).thenReturn(true);
		when(unavailableLock.tryLock()).thenReturn(false);

		new CompositeLock(anyResources(3), List.of(firstLock, secondLock, unavailableLock)).tryAcquire();

		var inOrder = inOrder(firstLock, secondLock);
		inOrder.verify(secondLock).unlock();
		inOrder.verify(firstLock).unlock();
		verify(unavailableLock, never()).unlock();
	}

	private Lock mockLock(String name, Executable lockAction) throws InterruptedException {
		var lock = mock(Lock.class, name);
		doAnswer(invocation -> {
			lockAction.execute();
			return null;
		}).when(lock).lockInterruptibly();
		return lock;
	}

	private List<ExclusiveResource> anyResources(int n) {
		return IntStream.range(0, n) //
				.mapToObj(j -> new ExclusiveResource("key" + j, LockMode.READ)) //
				.toList();
	}

}
