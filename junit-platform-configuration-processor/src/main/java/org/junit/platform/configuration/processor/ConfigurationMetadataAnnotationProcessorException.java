/*
 * Copyright 2026 the original author or authors.
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v2.0 which
 * accompanies this distribution and is available at
 *
 * https://www.eclipse.org/legal/epl-v20.html
 */

package org.junit.platform.configuration.processor;

import java.io.Serial;

final class ConfigurationMetadataAnnotationProcessorException extends RuntimeException {

	@Serial
	private static final long serialVersionUID = 1L;

	ConfigurationMetadataAnnotationProcessorException(String message, Throwable cause) {
		super(message, cause);
	}
}
