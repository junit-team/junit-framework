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
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;

import com.sun.net.httpserver.HttpServer;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.ParameterContext;
import org.junit.jupiter.api.extension.ParameterResolver;

@ExtendWith(HttpServerParameterResolver.class)
class HttpTests {

	@Test
	void respondsWith204(HttpServer server) throws IOException {
		String host = server.getAddress().getHostString(); // <2>
		int port = server.getAddress().getPort(); // <3>
		URL url = URI.create("http://" + host + ":" + port + "/test").toURL();

		HttpURLConnection connection = (HttpURLConnection) url.openConnection();
		connection.setRequestMethod("GET");
		int responseCode = connection.getResponseCode(); // <4>

		assertEquals(204, responseCode); // <5>
	}
}

class HttpServerParameterResolver implements ParameterResolver {
	@Override
	public boolean supportsParameter(ParameterContext parameterContext, ExtensionContext extensionContext) {
		return HttpServer.class.equals(parameterContext.getParameter().getType());
	}

	//end::user_guide[]
	@SuppressWarnings("DataFlowIssue")
	//tag::user_guide[]
	@Override
	public Object resolveParameter(ParameterContext parameterContext, ExtensionContext extensionContext) {
		return extensionContext
				// tag::custom_line_break[]
				.getStore(ExtensionContext.Namespace.GLOBAL)
				// tag::custom_line_break[]
				.get("httpServer", CloseableHttpServer.class) // <1>
				.getServer();
	}
}
//end::user_guide[]
