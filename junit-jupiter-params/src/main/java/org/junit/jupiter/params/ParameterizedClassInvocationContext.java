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

import static java.util.Collections.singletonList;
import static org.junit.jupiter.api.TestInstance.Lifecycle.PER_METHOD;

import java.util.List;

import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.extension.ContainerTemplateInvocationContext;
import org.junit.jupiter.api.extension.Extension;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.params.ParameterizedClassContext.InjectionType;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.platform.commons.JUnitException;
import org.junit.platform.commons.util.Preconditions;

class ParameterizedClassInvocationContext extends ParameterizedInvocationContext<ParameterizedClassContext>
		implements ContainerTemplateInvocationContext {

	private final ResolutionCache resolutionCache = ResolutionCache.enabled();

	ParameterizedClassInvocationContext(ParameterizedClassContext classContext,
			ParameterizedInvocationNameFormatter formatter, Arguments arguments, int invocationIndex) {
		super(classContext, formatter, arguments, invocationIndex);
	}

	@Override
	public String getDisplayName(int invocationIndex) {
		return super.getDisplayName(invocationIndex);
	}

	@Override
	public List<Extension> getAdditionalExtensions() {
		InjectionType injectionType = this.declarationContext.getInjectionType();
		switch (injectionType) {
			case CONSTRUCTOR:
				return singletonList(createExtensionForConstructorInjection());
			case FIELDS:
				return singletonList(createExtensionForFieldInjection());
		}
		throw new JUnitException("Unsupported injection type: " + injectionType);
	}

	private ContainerTemplateConstructorParameterResolver createExtensionForConstructorInjection() {
		Preconditions.condition(this.declarationContext.getTestInstanceLifecycle() == PER_METHOD,
			"Constructor injection is only supported for lifecycle PER_METHOD");
		return new ContainerTemplateConstructorParameterResolver(this.declarationContext, this.arguments,
			this.invocationIndex, this.resolutionCache);
	}

	private Extension createExtensionForFieldInjection() {
		ResolverFacade resolverFacade = this.declarationContext.getResolverFacade();
		TestInstance.Lifecycle lifecycle = this.declarationContext.getTestInstanceLifecycle();
		switch (lifecycle) {
			case PER_CLASS:
				return new ContainerTemplateInstanceFieldInjectingBeforeEachCallback(resolverFacade, this.arguments,
					this.invocationIndex, this.resolutionCache);
			case PER_METHOD:
				return new ContainerTemplateInstanceFieldInjectingPostProcessor(resolverFacade, this.arguments,
					this.invocationIndex, this.resolutionCache);
		}
		throw new JUnitException("Unsupported lifecycle: " + lifecycle);
	}

	@Override
	public void prepareInvocation(ExtensionContext context) {
		super.prepareInvocation(context);
	}

}
