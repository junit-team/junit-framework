/*
 * Copyright 2015-2026 the original author or authors.
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v2.0 which
 * accompanies this distribution and is available at
 *
 * https://www.eclipse.org/legal/epl-v20.html
 */

package example.util;

import static java.util.Objects.requireNonNull;

import org.jspecify.annotations.Nullable;

public class StringUtils {

	public static boolean isPalindrome(@Nullable String candidate) {
		int length = requireNonNull(candidate).length();
		for (int i = 0; i < length / 2; i++) {
			if (candidate.charAt(i) != candidate.charAt(length - (i + 1))) {
				return false;
			}
		}
		return true;
	}

	private StringUtils() {
	}

}
