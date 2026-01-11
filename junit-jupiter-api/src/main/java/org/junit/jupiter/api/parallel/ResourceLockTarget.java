/*
 * Copyright 2015-2026 the original author or authors.
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v2.0 which
 * accompanies this distribution and is available at
 *
 * https://www.eclipse.org/legal/epl-v20.html
 */

package org.junit.jupiter.api.parallel;

import static org.apiguardian.api.API.Status.MAINTAINED;

import org.apiguardian.api.API;

/**
 * {@code ResourceLockTarget} is used to define the target of a shared resource.
 *
 * @since 5.12
 * @see ResourceLock#target()
 */
@API(status = MAINTAINED, since = "5.13.3")
public enum ResourceLockTarget {

	/**
	 * Add a shared resource to the current node.
	 */
	SELF,

	/**
	 * Add a shared resource to the <em>direct</em> children of the current node.
	 *
	 * <p>Examples of "parent - child" relationships in the context of
	 * {@code ResourceLockTarget}:
	 * <ul>
	 *   <li><strong>test class</strong>: test methods and nested test classes
	 *   declared in the test class are children of the test class.</li>
	 *   <li><strong>nested test class</strong>: test methods and nested test
	 *   classes declared in the nested class are children of the nested test class.
	 *   </li>
	 *   <li><strong>test method</strong>: test methods are not considered to have
	 *   children. Using {@code CHILDREN} for a test method results in an
	 *   exception.</li>
	 * </ul>
	 */
	CHILDREN

}
