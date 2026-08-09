/*
 * Copyright 2015-2026 the original author or authors.
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v2.0 which
 * accompanies this distribution and is available at
 *
 * https://www.eclipse.org/legal/epl-v20.html
 */

package org.junit.platform.fakes;

import org.junit.platform.engine.UniqueId;
import org.junit.platform.engine.support.descriptor.EngineDescriptor;

/**
 * @since 6.2
 */
public class EngineTestDescriptorStub extends EngineDescriptor {

	public EngineTestDescriptorStub(UniqueId uniqueId, String displayName) {
		super(uniqueId, displayName);
	}

}
