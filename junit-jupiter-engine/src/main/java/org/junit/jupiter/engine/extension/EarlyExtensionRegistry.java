/*
 * Copyright 2015-2026 the original author or authors.
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v2.0 which
 * accompanies this distribution and is available at
 *
 * https://www.eclipse.org/legal/epl-v20.html
 */

package org.junit.jupiter.engine.extension;

import static java.util.Collections.emptyList;
import static org.apiguardian.api.API.Status.INTERNAL;

import java.util.List;
import java.util.ServiceLoader;
import java.util.function.Predicate;

import org.apiguardian.api.API;
import org.junit.jupiter.api.extension.AsyncReturnValueHandler;
import org.junit.jupiter.api.extension.EarlyExtension;
import org.junit.jupiter.api.extension.Extension;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.engine.config.JupiterConfiguration;
import org.junit.platform.commons.util.ClassLoaderUtils;
import org.junit.platform.commons.util.ServiceLoaderUtils;

/**
 * Registry of {@link EarlyExtension EarlyExtensions} that are made available
 * during <em>discovery</em>, before any {@link ExtensionContext} exists.
 *
 * <p>Unlike the regular {@link ExtensionRegistry}, which is only populated when
 * test execution starts, this registry is loaded eagerly so that the discovery
 * phase can consult it. It is created once per engine discovery and cached on
 * the engine descriptor for the duration of the corresponding discovery +
 * execution session.
 *
 * @since 6.2
 */
@API(status = INTERNAL, since = "6.2")
public final class EarlyExtensionRegistry {

	/**
	 * Creates a new registry, loading the {@link EarlyExtension EarlyExtensions}
	 * that apply globally via the {@link ServiceLoader}.
	 *
	 * <p>Automatic (the {@code ServiceLoader}) loading only happens when
	 * auto-detection is enabled in the supplied configuration, mirroring the
	 * behavior of {@link ExtensionRegistry}. Extensions that are registered
	 * declaratively via {@link org.junit.jupiter.api.extension.ExtendWith
	 * @ExtendWith} or {@link org.junit.jupiter.api.extension.RegisterExtension
	 * @RegisterExtension} are <em>not</em> part of this registry; they are
	 * resolved at runtime via the regular {@link ExtensionRegistry}.
	 *
	 * @param configuration the engine configuration; never {@code null}
	 * @return a new registry; never {@code null}
	 */
	public static EarlyExtensionRegistry create(JupiterConfiguration configuration) {
		if (!configuration.isExtensionAutoDetectionEnabled()) {
			return new EarlyExtensionRegistry(emptyList());
		}

		Predicate<Class<? extends Extension>> filter = configuration.getFilterForAutoDetectedExtensions().and(
			EarlyExtension.class::isAssignableFrom);

		ServiceLoader<Extension> serviceLoader = ServiceLoader.load(Extension.class,
			ClassLoaderUtils.getDefaultClassLoader());
		List<AsyncReturnValueHandler> handlers = ServiceLoaderUtils.filter(serviceLoader, filter) //
				.filter(AsyncReturnValueHandler.class::isInstance) //
				.map(AsyncReturnValueHandler.class::cast) //
				.toList();
		return new EarlyExtensionRegistry(handlers);
	}

	/**
	 * Creates an empty registry with no globally loaded early extensions.
	 *
	 * @return a new empty registry; never {@code null}
	 */
	public static EarlyExtensionRegistry empty() {
		return new EarlyExtensionRegistry(emptyList());
	}

	private final List<AsyncReturnValueHandler> asyncReturnValueHandlers;

	private EarlyExtensionRegistry(List<AsyncReturnValueHandler> asyncReturnValueHandlers) {
		this.asyncReturnValueHandlers = asyncReturnValueHandlers;
	}

	/**
	 * Returns the globally loaded {@link AsyncReturnValueHandler
	 * AsyncReturnValueHandlers}, in {@code ServiceLoader} order.
	 *
	 * @return an immutable list; never {@code null}
	 */
	public List<AsyncReturnValueHandler> getAsyncReturnValueHandlers() {
		return asyncReturnValueHandlers;
	}

}
