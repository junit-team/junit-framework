/*
 * Copyright 2015-2026 the original author or authors.
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v2.0 which
 * accompanies this distribution and is available at
 *
 * https://www.eclipse.org/legal/epl-v20.html
 */

package org.junit.jupiter.params;

import static org.apiguardian.api.API.Status.EXPERIMENTAL;

import org.apiguardian.api.API;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.extension.Extension;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.ExtensionContext.Namespace;
import org.junit.jupiter.params.aggregator.ArgumentsAccessor;
import org.junit.jupiter.params.support.ParameterDeclarations;

/**
 * {@code ParameterInfo} is used to provide information about the current
 * invocation of a parameterized class or test.
 *
 * <p>Registered {@link Extension} implementations may retrieve the current
 * {@code ParameterInfo} instance by calling
 * {@link ExtensionContext#getStore(Namespace)} with {@link #NAMESPACE} and
 * {@link ExtensionContext.Store#get(Object, Class) Store.get(...)} with
 * {@link #KEY}. Alternatively, the {@link #get(ExtensionContext)} method may
 * be used to retrieve the {@code ParameterInfo} instance for the supplied
 * {@code ExtensionContext}. Extensions must not modify any entries in the
 * {@link ExtensionContext.Store Store} for {@link #NAMESPACE}.
 *
 * <p>When a {@link ParameterizedTest @ParameterizedTest} method is declared
 * inside a {@link ParameterizedClass @ParameterizedClass} or a
 * {@link Nested @Nested} {@link ParameterizedClass @ParameterizedClass} is
 * declared inside an enclosing {@link ParameterizedClass @ParameterizedClass},
 * there will be multiple {@code ParameterInfo} instances available on different
 * levels of the {@link ExtensionContext} hierarchy. In such cases, please use
 * {@link ExtensionContext#getParent()} to navigate to the right level before
 * retrieving the {@code ParameterInfo} instance from the
 * {@link ExtensionContext.Store Store}.
 *
 * @since 5.14
 * @see ParameterizedClass
 * @see ParameterizedTest
 */
@API(status = EXPERIMENTAL, since = "5.14")
public interface ParameterInfo {

	/**
	 * The {@link Namespace} for accessing the
	 * {@link ExtensionContext.Store Store} for {@code ParameterInfo}.
	 */
	Namespace NAMESPACE = Namespace.create(ParameterInfo.class);

	/**
	 * The key for retrieving the {@code ParameterInfo} instance from the
	 * {@link ExtensionContext.Store Store}.
	 */
	Object KEY = ParameterInfo.class;

	/**
	 * {@return the closest {@code ParameterInfo} instance for the supplied
	 * {@code ExtensionContext}; potentially {@code null}}
	 */
	static @Nullable ParameterInfo get(ExtensionContext context) {
		return context.getStore(NAMESPACE).get(KEY, ParameterInfo.class);
	}

	/**
	 * {@return the declarations of all <em>indexed</em> parameters}
	 */
	ParameterDeclarations getDeclarations();

	/**
	 * {@return an accessor for the arguments of the current invocation}
	 */
	ArgumentsAccessor getArguments();

}
