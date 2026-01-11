/*
 * Copyright 2015-2026 the original author or authors.
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v2.0 which
 * accompanies this distribution and is available at
 *
 * https://www.eclipse.org/legal/epl-v20.html
 */

package org.junit.platform.engine.support.descriptor;

import static org.apiguardian.api.API.Status.STABLE;

import org.apiguardian.api.API;
import org.junit.platform.engine.UniqueId;

/**
 * An {@code EngineDescriptor} is a {@link org.junit.platform.engine.TestDescriptor
 * TestDescriptor} for a specific {@link org.junit.platform.engine.TestEngine TestEngine}.
 *
 * @since 1.0
 */
@API(status = STABLE, since = "1.0")
public class EngineDescriptor extends AbstractTestDescriptor {

	/**
	 * Create a new {@code EngineDescriptor} with the supplied {@link UniqueId}
	 * and display name.
	 *
	 * @param uniqueId the {@code UniqueId} for the described {@code TestEngine};
	 * never {@code null}
	 * @param displayName the display name for the described {@code TestEngine};
	 * never {@code null} or blank
	 * @see org.junit.platform.engine.TestEngine#getId()
	 * @see org.junit.platform.engine.TestDescriptor#getDisplayName()
	 */
	public EngineDescriptor(UniqueId uniqueId, String displayName) {
		super(uniqueId, displayName);
	}

	/**
	 * Returns {@link org.junit.platform.engine.TestDescriptor.Type#CONTAINER}.
	 *
	 * @see org.junit.platform.engine.TestDescriptor#isContainer()
	 * @see org.junit.platform.engine.TestDescriptor#isTest()
	 */
	@Override
	public Type getType() {
		return Type.CONTAINER;
	}

}
