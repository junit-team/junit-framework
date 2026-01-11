/*
 * Copyright 2015-2026 the original author or authors.
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v2.0 which
 * accompanies this distribution and is available at
 *
 * https://www.eclipse.org/legal/epl-v20.html
 */

package platform.tooling.support.tests;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.URI;
import java.util.List;

import com.sun.net.httpserver.HttpServer;

public class MavenRepoProxy implements AutoCloseable {

	// Forbid downloading JUnit artifacts since we want to use the local ones
	private static final List<String> FORBIDDEN_PATHS = List.of("/org/junit");

	private final HttpServer httpServer;

	@SuppressWarnings("unused")
	public MavenRepoProxy() throws IOException {
		this(0);
	}

	@SuppressWarnings("unused")
	public MavenRepoProxy(int port) throws IOException {
		this("https://central.sonatype.com/repository/maven-snapshots", port);
	}

	private MavenRepoProxy(String proxiedUrl, int port) throws IOException {
		httpServer = HttpServer.create(new InetSocketAddress(InetAddress.getLoopbackAddress(), port), 0);
		httpServer.createContext("/", exchange -> {
			try (exchange) {
				switch (exchange.getRequestMethod()) {
					case "HEAD", "GET" -> {
						if (FORBIDDEN_PATHS.stream().anyMatch(exchange.getRequestURI().getPath()::startsWith)) {
							exchange.sendResponseHeaders(404, -1);
							break;
						}
						var redirectUrl = proxiedUrl + exchange.getRequestURI().getPath();
						exchange.getResponseHeaders().add("Location", redirectUrl);
						exchange.sendResponseHeaders(302, -1);
					}
					default -> exchange.sendResponseHeaders(405, -1);
				}
			}
			catch (Exception e) {
				e.printStackTrace();
			}
		});
		httpServer.start();
	}

	URI getBaseUri() {
		var address = httpServer.getAddress();
		return URI.create("http://" + address.getAddress().getHostName() + ":" + address.getPort());
	}

	@Override
	public void close() {
		httpServer.stop(0);
	}

}
