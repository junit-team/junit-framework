/*
 * Copyright 2015-2026 the original author or authors.
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v2.0 which
 * accompanies this distribution and is available at
 *
 * https://www.eclipse.org/legal/epl-v20.html
 */

package org.junit.vintage.engine.support;

import static org.apiguardian.api.API.Status.INTERNAL;
import static org.junit.platform.commons.util.ReflectionUtils.tryToReadFieldValue;

import java.io.Serializable;
import java.util.function.Function;

import org.apiguardian.api.API;
import org.junit.platform.commons.logging.Logger;
import org.junit.platform.commons.logging.LoggerFactory;
import org.junit.runner.Description;

/**
 * @since 4.12
 */
@API(status = INTERNAL, since = "4.12")
public class UniqueIdReader implements Function<Description, Serializable> {

	private static final Logger logger = LoggerFactory.getLogger(UniqueIdReader.class);

	private final String fieldName;

	public UniqueIdReader() {
		this("fUniqueId");
	}

	// For tests only
	UniqueIdReader(String fieldName) {
		this.fieldName = fieldName;
	}

	@Override
	public Serializable apply(Description description) {
		// @formatter:off
		return tryToReadFieldValue(Description.class, fieldName, description)
				.andThenTry(Serializable.class::cast)
				.ifFailure(cause -> logger.warn(cause, () ->
						"Could not read unique ID for Description; using display name instead: %s".formatted(description)))
				.toOptional()
				.orElseGet(description::getDisplayName);
		// @formatter:on
	}

}
