/*
 * Copyright 2015-2026 the original author or authors.
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v2.0 which
 * accompanies this distribution and is available at
 *
 * https://www.eclipse.org/legal/epl-v20.html
 */

package org.junit.jupiter.engine.extension;

import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.ExtensionContext;

/**
 * Demo extension for auto-detection of extensions loaded via Java's
 * {@link java.util.ServiceLoader} mechanism.
 *
 * @since 5.11
 */
public class ConfigLoaderExtension implements BeforeAllCallback {

	@Override
	public void beforeAll(ExtensionContext context) {
	}

}
