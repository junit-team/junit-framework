/*
 * Copyright 2015-2025 the original author or authors.
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v2.0 which
 * accompanies this distribution and is available at
 *
 * https://www.eclipse.org/legal/epl-v20.html
 */

package example;

import static java.net.InetAddress.getLoopbackAddress;
import static org.junit.platform.engine.TestExecutionResult.successful;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.ServerSocket;

import org.jspecify.annotations.Nullable;
import org.junit.platform.engine.EngineDiscoveryRequest;
import org.junit.platform.engine.ExecutionRequest;
import org.junit.platform.engine.TestDescriptor;
import org.junit.platform.engine.TestEngine;
import org.junit.platform.engine.UniqueId;
import org.junit.platform.engine.support.descriptor.EngineDescriptor;
import org.junit.platform.engine.support.store.Namespace;
import org.junit.platform.engine.support.store.NamespacedHierarchicalStore;

//tag::user_guide[]
/**
 * Second custom test engine implementation.
 */
public class SecondCustomEngine implements TestEngine {

	//end::user_guide[]
	@Nullable
	//tag::user_guide[]
	public ServerSocket socket;

	@Override
	public String getId() {
		return "second-custom-test-engine";
	}

	//end::user_guide[]
	@Nullable
	//tag::user_guide[]
	public ServerSocket getSocket() {
		return this.socket;
	}

	@Override
	public TestDescriptor discover(EngineDiscoveryRequest discoveryRequest, UniqueId uniqueId) {
		return new EngineDescriptor(uniqueId, "Second Custom Test Engine");
	}

	@Override
	public void execute(ExecutionRequest request) {
		request.getEngineExecutionListener()
				// tag::custom_line_break[]
				.executionStarted(request.getRootTestDescriptor());

		NamespacedHierarchicalStore<Namespace> store = request.getStore();
		socket = store.computeIfAbsent(Namespace.GLOBAL, "serverSocket", key -> {
			try {
				return new ServerSocket(0, 50, getLoopbackAddress());
			}
			catch (IOException e) {
				throw new UncheckedIOException("Failed to start ServerSocket", e);
			}
		}, ServerSocket.class);

		request.getEngineExecutionListener()
				// tag::custom_line_break[]
				.executionFinished(request.getRootTestDescriptor(), successful());
	}

}
//end::user_guide[]
