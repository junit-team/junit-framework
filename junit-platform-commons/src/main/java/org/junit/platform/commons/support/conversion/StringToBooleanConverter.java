/*
 * Copyright 2015-2026 the original author or authors.
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v2.0 which
 * accompanies this distribution and is available at
 *
 * https://www.eclipse.org/legal/epl-v20.html
 */

package org.junit.platform.commons.support.conversion;

import org.junit.platform.commons.util.Preconditions;

class StringToBooleanConverter implements StringToObjectConverter {

	@Override
	public boolean canConvertTo(Class<?> targetType) {
		return targetType == Boolean.class;
	}

	@Override
	public Object convert(String source, Class<?> targetType) {
		boolean isTrue = "true".equalsIgnoreCase(source);
		Preconditions.condition(isTrue || "false".equalsIgnoreCase(source),
			() -> "String must be 'true' or 'false' (ignoring case): " + source);
		return isTrue;
	}

}
