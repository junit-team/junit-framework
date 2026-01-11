/*
 * Copyright 2015-2026 the original author or authors.
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v2.0 which
 * accompanies this distribution and is available at
 *
 * https://www.eclipse.org/legal/epl-v20.html
 */

package example.sharedresources;

import static org.junit.jupiter.api.parallel.ExecutionMode.CONCURRENT;
import static org.junit.jupiter.api.parallel.ResourceAccessMode.READ;
import static org.junit.jupiter.api.parallel.ResourceAccessMode.READ_WRITE;
import static org.junit.jupiter.api.parallel.ResourceLockTarget.CHILDREN;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ResourceLock;

// tag::user_guide[]
@Execution(CONCURRENT)
@ResourceLock(value = "a", mode = READ, target = CHILDREN)
public class ChildrenSharedResourcesDemo {

	@ResourceLock(value = "a", mode = READ_WRITE)
	@Test
	void test1() throws InterruptedException {
		Thread.sleep(2000L);
	}

	@Test
	void test2() throws InterruptedException {
		Thread.sleep(2000L);
	}

	@Test
	void test3() throws InterruptedException {
		Thread.sleep(2000L);
	}

	@Test
	void test4() throws InterruptedException {
		Thread.sleep(2000L);
	}

	@Test
	void test5() throws InterruptedException {
		Thread.sleep(2000L);
	}

}
// end::user_guide[]
