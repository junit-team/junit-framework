/*
 * Copyright 2015-2026 the original author or authors.
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v2.0 which
 * accompanies this distribution and is available at
 *
 * https://www.eclipse.org/legal/epl-v20.html
 */

package org.junit.jupiter.params.provider;

import java.util.Arrays;

import org.jspecify.annotations.Nullable;
import org.junit.platform.commons.support.ReflectionSupport;

public interface RecordArguments extends Arguments {

	@Override
	default @Nullable Object[] get() {
		return Arrays.stream(getClass().getRecordComponents()) //
				.map(component -> ReflectionSupport.invokeMethod(component.getAccessor(), this)) //
				.toArray();
	}

}
