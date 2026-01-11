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
import java.util.concurrent.ExecutorService;

import com.sun.net.httpserver.HttpServer;

public class CloseableHttpServer implements AutoCloseable {

	private final HttpServer server;
	private final ExecutorService executorService;

	CloseableHttpServer(HttpServer server, ExecutorService executorService) {
		this.server = server;
		this.executorService = executorService;
	}

	public HttpServer getServer() {
		return server;
	}

	@Override
	public void close() { // <1>
		server.stop(0); // <2>
		executorService.shutdownNow();
	}
}
//end::user_guide[]
