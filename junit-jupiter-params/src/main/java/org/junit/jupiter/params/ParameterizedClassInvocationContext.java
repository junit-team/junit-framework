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

import static org.junit.jupiter.api.TestInstance.Lifecycle.PER_METHOD;

import java.util.List;
import java.util.stream.Stream;

import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.extension.ClassTemplateInvocationContext;
import org.junit.jupiter.api.extension.Extension;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.params.ParameterizedClassContext.InjectionType;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.platform.commons.util.Preconditions;

class ParameterizedClassInvocationContext extends ParameterizedInvocationContext<ParameterizedClassContext>
		implements ClassTemplateInvocationContext {

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
		return Stream.concat(Stream.of(createParameterInjector()), createLifecycleMethodInvokers()) //
				.toList();
	}

	@Override
	public void prepareInvocation(ExtensionContext context) {
		super.prepareInvocation(context);
	}

	private Extension createParameterInjector() {
		InjectionType injectionType = this.declarationContext.getInjectionType();
		return switch (injectionType) {
			case CONSTRUCTOR -> createExtensionForConstructorInjection();
			case FIELDS -> createExtensionForFieldInjection();
		};
	}

	private ClassTemplateConstructorParameterResolver createExtensionForConstructorInjection() {
		Preconditions.condition(this.declarationContext.getTestInstanceLifecycle() == PER_METHOD,
			"Constructor injection is only supported for lifecycle PER_METHOD");
		return new ClassTemplateConstructorParameterResolver(this.declarationContext, this.arguments,
			this.invocationIndex, this.resolutionCache);
	}

	private Extension createExtensionForFieldInjection() {
		ResolverFacade resolverFacade = this.declarationContext.getResolverFacade();
		TestInstance.Lifecycle lifecycle = this.declarationContext.getTestInstanceLifecycle();
		return switch (lifecycle) {
			case PER_CLASS -> new BeforeClassTemplateInvocationFieldInjector(resolverFacade, this.arguments,
				this.invocationIndex, this.resolutionCache);
			case PER_METHOD -> new InstancePostProcessingClassTemplateFieldInjector(resolverFacade, this.arguments,
				this.invocationIndex, this.resolutionCache);
		};
	}

	private Stream<Extension> createLifecycleMethodInvokers() {
		return Stream.concat( //
			this.declarationContext.getBeforeMethods().stream().map(
				this::createBeforeParameterizedClassInvocationMethodInvoker), //
			this.declarationContext.getAfterMethods().stream().map(
				this::createAfterParameterizedClassInvocationMethodInvoker) //
		);
	}

	private BeforeParameterizedClassInvocationMethodInvoker createBeforeParameterizedClassInvocationMethodInvoker(
			ArgumentSetLifecycleMethod method) {
		return new BeforeParameterizedClassInvocationMethodInvoker(this.declarationContext, this.arguments,
			this.invocationIndex, this.resolutionCache, method);
	}

	private AfterParameterizedClassInvocationMethodInvoker createAfterParameterizedClassInvocationMethodInvoker(
			ArgumentSetLifecycleMethod method) {
		return new AfterParameterizedClassInvocationMethodInvoker(this.declarationContext, this.arguments,
			this.invocationIndex, this.resolutionCache, method);
	}

}
