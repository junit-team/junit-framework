/*
 * Copyright 2015-2026 the original author or authors.
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v2.0 which
 * accompanies this distribution and is available at
 *
 * https://www.eclipse.org/legal/epl-v20.html
 */

package org.junit.platform.tests.process;

import static org.junit.platform.tests.process.ProcessStarter.OUTPUT_ENCODING;

import java.io.ByteArrayOutputStream;
import java.io.OutputStream;
import java.util.Optional;

record WatchedOutput(Thread thread, ByteArrayOutputStream stream, Optional<OutputStream> fileStream) {

	String streamAsString() {
		return stream.toString(OUTPUT_ENCODING);
	}
}
