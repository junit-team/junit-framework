/*
 * Copyright 2015-2026 the original author or authors.
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v2.0 which
 * accompanies this distribution and is available at
 *
 * https://www.eclipse.org/legal/epl-v20.html
 */

package example;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;

import com.sun.net.httpserver.HttpServer;

import example.extensions.HttpServerExtension;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

// tag::user_guide[]
@ExtendWith(HttpServerExtension.class)
public class HttpServerDemo {

	// end::user_guide[]
	@SuppressWarnings("HttpUrlsUsage")
	// tag::user_guide[]
	@Test
	void httpCall(HttpServer server) throws Exception {
		String hostName = server.getAddress().getHostName();
		int port = server.getAddress().getPort();
		String rawUrl = "http://%s:%d/example".formatted(hostName, port);
		URL requestUrl = URI.create(rawUrl).toURL();

		String responseBody = sendRequest(requestUrl);

		assertEquals("This is a test", responseBody);
	}

	private static String sendRequest(URL url) throws IOException {
		HttpURLConnection connection = (HttpURLConnection) url.openConnection();
		int contentLength = connection.getContentLength();
		try (InputStream response = url.openStream()) {
			byte[] content = new byte[contentLength];
			assertEquals(contentLength, response.read(content));
			return new String(content, UTF_8);
		}
	}
}
// end::user_guide[]
