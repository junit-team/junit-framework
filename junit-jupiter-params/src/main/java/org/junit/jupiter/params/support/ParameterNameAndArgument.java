/*
 * Copyright 2015-2026 the original author or authors.
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v2.0 which
 * accompanies this distribution and is available at
 *
 * https://www.eclipse.org/legal/epl-v20.html
 */

package org.junit.jupiter.params.support;

import static org.apiguardian.api.API.Status.INTERNAL;

import org.apiguardian.api.API;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.Named;

/**
 * Customized parameter name and its associated argument value.
 *
 * <p>Although this class implements {@link Named} for technical reasons, it
 * serves a different purpose than {@link Named#of(String, Object)} and is only
 * used for internal display name processing.
 *
 * @since 6.0
 */
@API(status = INTERNAL, since = "6.0")
public class ParameterNameAndArgument implements Named<@Nullable Object> {

	private final String name;

	private final @Nullable Object argument;

	public ParameterNameAndArgument(String name, @Nullable Object argument) {
		this.name = name;
		this.argument = argument;
	}

	/**
	 * Get the customized name of the parameter.
	 */
	@Override
	public String getName() {
		return this.name;
	}

	/**
	 * Get the argument for the parameter.
	 */
	@Override
	public @Nullable Object getPayload() {
		return this.argument;
	}

	@Override
	public String toString() {
		return "ParameterNameAndArgument[name = %s, argument = %s]".formatted(this.name, this.argument);
	}

}
