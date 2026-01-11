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

class StringToCharacterConverter implements StringToObjectConverter {

	@Override
	public boolean canConvertTo(Class<?> targetType) {
		return targetType == Character.class;
	}

	@Override
	public Object convert(String source, Class<?> targetType) {
		Preconditions.condition(source.length() == 1, () -> "String must have length of 1: " + source);
		return source.charAt(0);
	}

}
