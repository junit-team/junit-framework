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

import java.io.IOException;
import java.io.UncheckedIOException;

import com.sun.net.httpserver.HttpServer;

import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.ExtensionContext.Namespace;
import org.junit.jupiter.api.extension.ParameterContext;
import org.junit.jupiter.api.extension.ParameterResolver;

// tag::user_guide[]
public class HttpServerExtension implements ParameterResolver {

	@Override
	public boolean supportsParameter(ParameterContext parameterContext, ExtensionContext extensionContext) {
		return HttpServer.class.equals(parameterContext.getParameter().getType());
	}

	@Override
	public Object resolveParameter(ParameterContext parameterContext, ExtensionContext extensionContext) {

		ExtensionContext rootContext = extensionContext.getRoot();
		ExtensionContext.Store store = rootContext.getStore(Namespace.GLOBAL);
		Class<HttpServerResource> key = HttpServerResource.class;
		HttpServerResource resource = store.computeIfAbsent(key, __ -> {
			try {
				HttpServerResource serverResource = new HttpServerResource(0);
				serverResource.start();
				return serverResource;
			}
			catch (IOException e) {
				throw new UncheckedIOException("Failed to create HttpServerResource", e);
			}
		}, HttpServerResource.class);
		return resource.getHttpServer();
	}
}
// end::user_guide[]
