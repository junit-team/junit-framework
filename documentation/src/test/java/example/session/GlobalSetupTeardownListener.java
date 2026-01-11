/*
 * Copyright 2015-2026 the original author or authors.
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v2.0 which
 * accompanies this distribution and is available at
 *
 * https://www.eclipse.org/legal/epl-v20.html
 */

package example.session;

//tag::user_guide[]
import static java.net.InetAddress.getLoopbackAddress;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.InetSocketAddress;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import com.sun.net.httpserver.HttpServer;

import org.junit.platform.engine.support.store.Namespace;
import org.junit.platform.engine.support.store.NamespacedHierarchicalStore;
import org.junit.platform.launcher.LauncherSession;
import org.junit.platform.launcher.LauncherSessionListener;
import org.junit.platform.launcher.TestExecutionListener;
import org.junit.platform.launcher.TestPlan;

public class GlobalSetupTeardownListener implements LauncherSessionListener {

	@Override
	public void launcherSessionOpened(LauncherSession session) {
		// Avoid setup for test discovery by delaying it until tests are about to be executed
		session.getLauncher().registerTestExecutionListeners(new TestExecutionListener() {
			@Override
			public void testPlanExecutionStarted(TestPlan testPlan) {
				//end::user_guide[]
				if (!testPlan.getConfigurationParameters().getBoolean("enableHttpServer").orElse(true)) {
					// avoid starting multiple HTTP servers unnecessarily from UsingTheLauncherDemo
					return;
				}
				//tag::user_guide[]
				NamespacedHierarchicalStore<Namespace> store = session.getStore(); // <1>
				store.computeIfAbsent(Namespace.GLOBAL, "httpServer", key -> { // <2>
					InetSocketAddress address = new InetSocketAddress(getLoopbackAddress(), 0);
					HttpServer server;
					try {
						server = HttpServer.create(address, 0);
					}
					catch (IOException e) {
						throw new UncheckedIOException("Failed to start HTTP server", e);
					}
					server.createContext("/test", exchange -> {
						exchange.sendResponseHeaders(204, -1);
						exchange.close();
					});
					ExecutorService executorService = Executors.newCachedThreadPool();
					server.setExecutor(executorService);
					server.start(); // <3>

					return new CloseableHttpServer(server, executorService);
				});
			}
		});
	}

}
//end::user_guide[]
