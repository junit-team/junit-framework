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

import static org.junit.jupiter.params.provider.Arguments.arguments;

import org.junit.platform.commons.util.ReflectionUtils;

/**
 * Collection of utilities for working with {@link Arguments}.
 *
 * @since 5.11, when it was extracted from {@link MethodArgumentsProvider}
 */
final class ArgumentsUtils {

	private ArgumentsUtils() {
		/* no-op */
	}

	/**
	 * Convert the supplied object into an {@link Arguments} instance.
	 */
	static Arguments toArguments(Object item) {
		// Nothing to do except cast.
		if (item instanceof Arguments arguments) {
			return arguments;
		}

		// Pass all multidimensional arrays "as is", in contrast to Object[].
		// See https://github.com/junit-team/junit-framework/issues/1665
		if (ReflectionUtils.isMultidimensionalArray(item)) {
			return arguments(item);
		}

		// Special treatment for one-dimensional reference arrays.
		// See https://github.com/junit-team/junit-framework/issues/1665
		if (item instanceof Object[] objects) {
			return arguments(objects);
		}

		// Pass everything else "as is".
		return arguments(item);
	}

}
