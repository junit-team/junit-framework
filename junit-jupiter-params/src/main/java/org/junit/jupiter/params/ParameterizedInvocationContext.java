/*
 * Copyright 2015-2025 the original author or authors.
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v2.0 which
 * accompanies this distribution and is available at
 *
 * https://www.eclipse.org/legal/epl-v20.html
 */

package org.junit.jupiter.params;

import static org.junit.platform.commons.util.ClassLoaderUtils.getClassLoader;

import java.util.Arrays;
import java.util.concurrent.atomic.AtomicInteger;

import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.ExtensionContext.Namespace;
import org.junit.jupiter.params.aggregator.ArgumentsAccessor;
import org.junit.jupiter.params.aggregator.DefaultArgumentsAccessor;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.support.ParameterDeclarations;

class ParameterizedInvocationContext<T extends ParameterizedDeclarationContext<?>> {

	private static final Namespace NAMESPACE = Namespace.create(ParameterizedTestInvocationContext.class);

	protected final T declarationContext;
	private final ParameterizedInvocationNameFormatter formatter;
	protected final EvaluatedArgumentSet arguments;
	protected final int invocationIndex;

	ParameterizedInvocationContext(T declarationContext, ParameterizedInvocationNameFormatter formatter,
			Arguments arguments, int invocationIndex) {

		this.declarationContext = declarationContext;
		this.formatter = formatter;
		ResolverFacade resolverFacade = this.declarationContext.getResolverFacade();
		this.arguments = EvaluatedArgumentSet.of(arguments, resolverFacade::determineConsumedArgumentLength);
		this.invocationIndex = invocationIndex;
	}

	public String getDisplayName(int invocationIndex) {
		return this.formatter.format(invocationIndex, this.arguments, this.declarationContext.quoteTextArguments());
	}

	public void prepareInvocation(ExtensionContext context) {
		if (this.declarationContext.isAutoClosingArguments()) {
			registerAutoCloseableArgumentsInStoreForClosing(context);
		}
		validateArgumentCount(context);
		storeParameterInfo(context);
	}

	private void registerAutoCloseableArgumentsInStoreForClosing(ExtensionContext context) {
		ExtensionContext.Store store = context.getStore(NAMESPACE);
		AtomicInteger argumentIndex = new AtomicInteger();

		Arrays.stream(this.arguments.getAllPayloads()) //
				.filter(AutoCloseable.class::isInstance) //
				.map(AutoCloseable.class::cast) //
				.map(CloseableArgument::new) //
				.forEach(closeable -> store.put(argumentIndex.incrementAndGet(), closeable));
	}

	private void validateArgumentCount(ExtensionContext context) {
		new ArgumentCountValidator(this.declarationContext, this.arguments).validate(context);
	}

	private void storeParameterInfo(ExtensionContext context) {
		ParameterDeclarations declarations = this.declarationContext.getResolverFacade().getIndexedParameterDeclarations();
		ClassLoader classLoader = getClassLoader(this.declarationContext.getTestClass());
		@Nullable
		Object[] arguments = this.arguments.getConsumedPayloads();
		ArgumentsAccessor accessor = DefaultArgumentsAccessor.create(invocationIndex, classLoader, arguments);
		new DefaultParameterInfo(declarations, accessor).store(context);
	}

	@SuppressWarnings({ "deprecation", "try" })
	private record CloseableArgument(AutoCloseable autoCloseable)
			implements ExtensionContext.Store.CloseableResource, AutoCloseable {

		@Override
		public void close() throws Exception {
			this.autoCloseable.close();
		}

	}
}
