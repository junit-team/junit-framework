/*
 * Copyright 2015-2025 the original author or authors.
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v2.0 which
 * accompanies this distribution and is available at
 *
 * https://www.eclipse.org/legal/epl-v20.html
 */

package org.junit.platform.commons.support;

import static org.apiguardian.api.API.Status.DEPRECATED;

import org.apiguardian.api.API;

/**
 * Search options for finding an annotation within a class hierarchy.
 *
 * @since 1.8
 * @see #DEFAULT
 * @see #INCLUDE_ENCLOSING_CLASSES
 * @deprecated because there is only a single non-deprecated search option left
 */
@Deprecated(since = "1.12")
@API(status = DEPRECATED, since = "1.12")
public enum SearchOption {

	/**
	 * Search the inheritance hierarchy (i.e., the current class, implemented
	 * interfaces, and superclasses), but do not search on enclosing classes.
	 *
	 * @see Class#getSuperclass()
	 * @see Class#getInterfaces()
	 */
	DEFAULT,

	/**
	 * Search the inheritance hierarchy as with the {@link #DEFAULT} search option
	 * but also search the {@linkplain Class#getEnclosingClass() enclosing class}
	 * hierarchy for <em>inner classes</em> (i.e., a non-static member classes).
	 *
	 * @deprecated because it is preferable to inspect the runtime enclosing
	 * types of a class rather than where they are declared.
	 */
	@Deprecated(since = "1.12") //
	@API(status = DEPRECATED, since = "1.12")
	INCLUDE_ENCLOSING_CLASSES

}
