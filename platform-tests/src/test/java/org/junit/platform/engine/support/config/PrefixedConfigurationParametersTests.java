/*
 * Copyright 2015-2025 the original author or authors.
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v2.0 which
 * accompanies this distribution and is available at
 *
 * https://www.eclipse.org/legal/epl-v20.html
 */

package org.junit.platform.engine.support.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.platform.commons.test.PreconditionAssertions.assertPreconditionViolationFor;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;
import java.util.function.Function;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.platform.engine.ConfigurationParameters;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Unit tests for {@link PrefixedConfigurationParameters}.
 *
 * @since 1.3
 */
@ExtendWith(MockitoExtension.class)
class PrefixedConfigurationParametersTests {

	@Mock
	private ConfigurationParameters delegate;

	@SuppressWarnings("DataFlowIssue")
	@Test
	void preconditions() {
		assertPreconditionViolationFor(() -> new PrefixedConfigurationParameters(null, "example."));
		assertPreconditionViolationFor(() -> new PrefixedConfigurationParameters(delegate, null));
		assertPreconditionViolationFor(() -> new PrefixedConfigurationParameters(delegate, ""));
		assertPreconditionViolationFor(() -> new PrefixedConfigurationParameters(delegate, "    "));
	}

	@Test
	void delegatesGetCalls() {
		when(delegate.get(any())).thenReturn(Optional.of("result"));
		var parameters = new PrefixedConfigurationParameters(delegate, "foo.bar.");

		assertThat(parameters.get("qux")).contains("result");

		verify(delegate).get("foo.bar.qux");
	}

	@Test
	void delegatesGetBooleanCalls() {
		when(delegate.getBoolean(any())).thenReturn(Optional.of(true));
		var parameters = new PrefixedConfigurationParameters(delegate, "foo.bar.");

		assertThat(parameters.getBoolean("qux")).contains(true);

		verify(delegate).getBoolean("foo.bar.qux");
	}

	@Test
	void delegatesGetWithTransformerCalls() {
		when(delegate.get(any(), any())).thenReturn(Optional.of("QUX"));
		var parameters = new PrefixedConfigurationParameters(delegate, "foo.bar.");

		Function<String, String> transformer = String::toUpperCase;
		assertThat(parameters.get("qux", transformer)).contains("QUX");

		verify(delegate).get("foo.bar.qux", transformer);
	}

}
