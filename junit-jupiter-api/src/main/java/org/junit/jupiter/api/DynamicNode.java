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
import java.util.function.Function;

import org.apiguardian.api.API;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.extension.ConditionEvaluationResult;
import org.junit.jupiter.api.extension.ExtensionContext;
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
	private final @Nullable Function<? super ExtensionContext, ? extends ConditionEvaluationResult> executionCondition;

	DynamicNode(AbstractConfiguration configuration) {
		this.displayName = Preconditions.notBlank(configuration.displayName, "displayName must not be null or blank");
		this.testSourceUri = configuration.testSourceUri;
		this.executionMode = configuration.executionMode;
		this.executionCondition = configuration.executionCondition;
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

	public Optional<Function<? super ExtensionContext, ? extends ConditionEvaluationResult>> getExecutionCondition() {
		return Optional.ofNullable(executionCondition);
	}

	@Override
	public String toString() {
		return new ToStringBuilder(this) //
				.append("displayName", displayName) //
				.append("testSourceUri", testSourceUri) //
				.toString();
	}

	public interface Configuration {

		Configuration displayName(String displayName);

		Configuration source(@Nullable URI testSourceUri);

		Configuration executionCondition(
				Function<? super ExtensionContext, ? extends ConditionEvaluationResult> condition);

		Configuration executionMode(ExecutionMode executionMode);

		Configuration executionMode(ExecutionMode executionMode, String reason);

	}

	abstract static class AbstractConfiguration implements Configuration {

		private @Nullable String displayName;
		private @Nullable URI testSourceUri;
		private @Nullable ExecutionMode executionMode;
		private @Nullable Function<? super ExtensionContext, ? extends ConditionEvaluationResult> executionCondition;

		@Override
		public Configuration displayName(String displayName) {
			this.displayName = displayName;
			return this;
		}

		@Override
		public Configuration source(@Nullable URI testSourceUri) {
			this.testSourceUri = testSourceUri;
			return this;
		}

		@Override
		public Configuration executionCondition(
				Function<? super ExtensionContext, ? extends ConditionEvaluationResult> condition) {
			// TODO Handle multiple calls
			this.executionCondition = condition;
			return this;
		}

		@Override
		public Configuration executionMode(ExecutionMode executionMode) {
			this.executionMode = executionMode;
			return this;
		}

		@Override
		public Configuration executionMode(ExecutionMode executionMode, String reason) {
			return executionMode(executionMode);
		}
	}

}
