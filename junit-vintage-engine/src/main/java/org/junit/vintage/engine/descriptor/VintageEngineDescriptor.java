/*
 * Copyright 2015-2026 the original author or authors.
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v2.0 which
 * accompanies this distribution and is available at
 *
 * https://www.eclipse.org/legal/epl-v20.html
 */

package org.junit.vintage.engine.descriptor;

import static org.apiguardian.api.API.Status.INTERNAL;

import java.util.Set;

import org.apiguardian.api.API;
import org.junit.platform.engine.TestDescriptor;
import org.junit.platform.engine.UniqueId;
import org.junit.platform.engine.support.descriptor.EngineDescriptor;

/**
 * @since 5.6
 */
@API(status = INTERNAL, since = "5.6")
public class VintageEngineDescriptor extends EngineDescriptor {

	public VintageEngineDescriptor(UniqueId uniqueId) {
		super(uniqueId, "JUnit Vintage");
	}

	public Set<TestDescriptor> getModifiableChildren() {
		return children;
	}

}
