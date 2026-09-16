/*
 * Copyright 2015-2026 the original author or authors.
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v2.0 which
 * accompanies this distribution and is available at
 *
 * https://www.eclipse.org/legal/epl-v20.html
 */

package org.junit.jupiter.engine.descriptor;

import static org.apiguardian.api.API.Status.INTERNAL;
import static org.junit.jupiter.engine.descriptor.JupiterTestDescriptor.toExecutionMode;

import org.apiguardian.api.API;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.engine.config.JupiterConfiguration;
import org.junit.jupiter.engine.execution.JupiterEngineExecutionContext;
import org.junit.jupiter.engine.extension.EarlyExtensionRegistry;
import org.junit.jupiter.engine.extension.MutableExtensionRegistry;
import org.junit.platform.engine.EngineExecutionListener;
import org.junit.platform.engine.UniqueId;
import org.junit.platform.engine.support.descriptor.EngineDescriptor;
import org.junit.platform.engine.support.hierarchical.Node;

/**
 * @since 5.0
 */
@API(status = INTERNAL, since = "5.0")
public class JupiterEngineDescriptor extends EngineDescriptor implements Node<JupiterEngineExecutionContext> {

	public static final String ENGINE_ID = "junit-jupiter";
	private final JupiterConfiguration configuration;
	private final EarlyExtensionRegistry earlyExtensionRegistry;

	public JupiterEngineDescriptor(UniqueId uniqueId, JupiterConfiguration configuration) {
		this(uniqueId, configuration, EarlyExtensionRegistry.empty());
	}

	public JupiterEngineDescriptor(UniqueId uniqueId, JupiterConfiguration configuration,
			EarlyExtensionRegistry earlyExtensionRegistry) {
		super(uniqueId, "JUnit Jupiter");
		this.configuration = configuration;
		this.earlyExtensionRegistry = earlyExtensionRegistry;
	}

	public JupiterConfiguration getConfiguration() {
		return configuration;
	}

	/**
	 * Returns the {@link EarlyExtensionRegistry} created during discovery.
	 *
	 * @return the early extension registry; never {@code null}
	 */
	public EarlyExtensionRegistry getEarlyExtensionRegistry() {
		return earlyExtensionRegistry;
	}

	@Override
	public ExecutionMode getExecutionMode() {
		return toExecutionMode(configuration.getDefaultExecutionMode());
	}

	@Override
	public JupiterEngineExecutionContext prepare(JupiterEngineExecutionContext context) {
		MutableExtensionRegistry extensionRegistry = MutableExtensionRegistry.createRegistryWithDefaultExtensions(
			context.getConfiguration());
		EngineExecutionListener executionListener = context.getExecutionListener();
		ExtensionContext extensionContext = new JupiterEngineExtensionContext(executionListener, this,
			context.getConfiguration(), extensionRegistry, context.getLauncherStoreFacade());

		// @formatter:off
		return context.extend()
				.withExtensionRegistry(extensionRegistry)
				.withExtensionContext(extensionContext)
				.build();
		// @formatter:on
	}

	@Override
	public void cleanUp(JupiterEngineExecutionContext context) throws Exception {
		context.close();
	}

}
