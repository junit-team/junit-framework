/*
 * Copyright 2015-2026 the original author or authors.
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v2.0 which
 * accompanies this distribution and is available at
 *
 * https://www.eclipse.org/legal/epl-v20.html
 */

package org.junit.platform.engine.support.store;

import static org.apiguardian.api.API.Status.MAINTAINED;

import java.io.Serial;

import org.apiguardian.api.API;
import org.junit.platform.commons.JUnitException;

/**
 * Exception thrown by failed {@link NamespacedHierarchicalStore} operations.
 *
 * @since 1.10
 */
@API(status = MAINTAINED, since = "1.13.3")
public class NamespacedHierarchicalStoreException extends JUnitException {

	@Serial
	private static final long serialVersionUID = 1L;

	public NamespacedHierarchicalStoreException(String message) {
		super(message);
	}

	public NamespacedHierarchicalStoreException(String message, Throwable cause) {
		super(message, cause);
	}

}
