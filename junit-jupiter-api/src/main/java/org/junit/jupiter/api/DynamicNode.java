/*
 * Copyright 2015-2025 the original author or authors.
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v2.0 which
 * accompanies this distribution and is available at
 *
 * https://www.eclipse.org/legal/epl-v20.html
 */

package org.junit.jupiter.api;

import static org.apiguardian.api.API.Status.MAINTAINED;

import java.net.URI;
import java.util.Optional;

import org.apiguardian.api.API;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.parallel.ExecutionMode;
import org.junit.platform.commons.util.Preconditions;
import org.junit.platform.commons.util.ToStringBuilder;

/**
 * {@code DynamicNode} serves as the abstract base class for a container or a
 * test case generated at runtime.
 *
 * @since 5.0
 * @see DynamicTest
 * @see DynamicContainer
 */
@API(status = MAINTAINED, since = "5.3")
public abstract class DynamicNode {

	private final String displayName;

	/** Custom test source {@link URI} associated with this node; potentially {@code null}. */
	private final @Nullable URI testSourceUri;

	private final @Nullable ExecutionMode executionMode;

	DynamicNode(AbstractConfiguration<?> configuration) {
		this.displayName = Preconditions.notBlank(configuration.displayName, "displayName must not be null or blank");
		this.testSourceUri = configuration.testSourceUri;
		this.executionMode = configuration.executionMode;
	}

	/**
	 * Get the display name of this {@code DynamicNode}.
	 *
	 * @return the display name
	 */
	public String getDisplayName() {
		return this.displayName;
	}

	/**
	 * Get the custom test source {@link URI} of this {@code DynamicNode}.
	 *
	 * @return an {@code Optional} containing the custom test source {@link URI};
	 * never {@code null} but potentially empty
	 * @since 5.3
	 */
	public Optional<URI> getTestSourceUri() {
		return Optional.ofNullable(testSourceUri);
	}

	public Optional<ExecutionMode> getExecutionMode() {
		return Optional.ofNullable(executionMode);
	}

	@Override
	public String toString() {
		return new ToStringBuilder(this) //
				.append("displayName", displayName) //
				.append("testSourceUri", testSourceUri) //
				.toString();
	}

	public sealed interface Configuration<T extends Configuration<T>>
			permits DynamicTest.Configuration, DynamicContainer.Configuration, AbstractConfiguration {

		T displayName(String displayName);

		T source(@Nullable URI testSourceUri);

		T executionMode(ExecutionMode executionMode);

		T executionMode(ExecutionMode executionMode, String reason);

	}

	abstract static sealed class AbstractConfiguration<T extends Configuration<T>> implements Configuration<T>
			permits DynamicTest.DefaultConfiguration, DynamicContainer.DefaultConfiguration {

		private @Nullable String displayName;
		private @Nullable URI testSourceUri;
		private @Nullable ExecutionMode executionMode;

		@Override
		public T displayName(String displayName) {
			this.displayName = displayName;
			return self();
		}

		@Override
		public T source(@Nullable URI testSourceUri) {
			this.testSourceUri = testSourceUri;
			return self();
		}

		@Override
		public T executionMode(ExecutionMode executionMode) {
			this.executionMode = executionMode;
			return self();
		}

		@Override
		public T executionMode(ExecutionMode executionMode, String reason) {
			return executionMode(executionMode);
		}

		protected abstract T self();
	}

}
