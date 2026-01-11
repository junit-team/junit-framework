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

import org.apiguardian.api.API;
import org.jspecify.annotations.Nullable;
import org.junit.runner.Description;

@API(status = API.Status.INTERNAL, since = "5.8")
public class DescriptionUtils {

	private DescriptionUtils() {
	}

	public static @Nullable String getMethodName(Description description) {
		String displayName = description.getDisplayName();
		int i = displayName.indexOf('(');
		if (i >= 0) {
			int j = displayName.lastIndexOf('(');
			if (i == j) {
				char lastChar = displayName.charAt(displayName.length() - 1);
				if (lastChar == ')') {
					return displayName.substring(0, i);
				}
			}
		}
		return description.getMethodName();
	}

}
