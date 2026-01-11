/*
 * Copyright 2015-2026 the original author or authors.
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v2.0 which
 * accompanies this distribution and is available at
 *
 * https://www.eclipse.org/legal/epl-v20.html
 */

package example.extensions;

import static java.nio.charset.StandardCharsets.UTF_8;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.InetSocketAddress;

import com.sun.net.httpserver.HttpServer;

/**
 * Demonstrates an implementation of {@link AutoCloseable} using an {@link HttpServer}.
 */
// tag::user_guide[]
class HttpServerResource implements AutoCloseable {

	private final HttpServer httpServer;

	// end::user_guide[]

	/**
	 * Initializes the Http server resource, using the given port.
	 *
	 * @param port (int) The port number for the server, must be in the range 0-65535.
	 * @throws IOException if an IOException occurs during initialization.
	 */
	// tag::user_guide[]
	HttpServerResource(int port) throws IOException {
		InetAddress loopbackAddress = InetAddress.getLoopbackAddress();
		this.httpServer = HttpServer.create(new InetSocketAddress(loopbackAddress, port), 0);
	}

	HttpServer getHttpServer() {
		return httpServer;
	}

	// end::user_guide[]

	/**
	 * Starts the Http server with an example handler.
	 */
	// tag::user_guide[]
	void start() {
		// Example handler
		httpServer.createContext("/example", exchange -> {
			String body = "This is a test";
			exchange.sendResponseHeaders(200, body.length());
			try (OutputStream os = exchange.getResponseBody()) {
				os.write(body.getBytes(UTF_8));
			}
		});
		httpServer.setExecutor(null);
		httpServer.start();
	}

	@Override
	public void close() {
		httpServer.stop(0);
	}
}
// end::user_guide[]
