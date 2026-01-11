/*
 * Copyright 2015-2026 the original author or authors.
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v2.0 which
 * accompanies this distribution and is available at
 *
 * https://www.eclipse.org/legal/epl-v20.html
 */

package org.junit.platform.suite.engine;

import org.junit.platform.engine.UniqueId;
import org.junit.platform.engine.support.descriptor.EngineDescriptor;

/**
 * @since 1.8
 */
final class SuiteEngineDescriptor extends EngineDescriptor {

	static final String ENGINE_ID = "junit-platform-suite";

	SuiteEngineDescriptor(UniqueId uniqueId) {
		super(uniqueId, "JUnit Platform Suite");
	}

	@Override
	public Type getType() {
		return Type.CONTAINER;
	}

}
