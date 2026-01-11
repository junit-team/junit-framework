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

import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.TestInstancePostProcessor;

class InstancePostProcessingClassTemplateFieldInjector implements TestInstancePostProcessor {

	private final ResolverFacade resolverFacade;
	private final EvaluatedArgumentSet arguments;
	private final int invocationIndex;
	private final ResolutionCache resolutionCache;

	InstancePostProcessingClassTemplateFieldInjector(ResolverFacade resolverFacade, EvaluatedArgumentSet arguments,
			int invocationIndex, ResolutionCache resolutionCache) {
		this.resolverFacade = resolverFacade;
		this.arguments = arguments;
		this.invocationIndex = invocationIndex;
		this.resolutionCache = resolutionCache;
	}

	@Override
	public ExtensionContextScope getTestInstantiationExtensionContextScope(ExtensionContext rootContext) {
		return ExtensionContextScope.TEST_METHOD;
	}

	@Override
	public void postProcessTestInstance(Object testInstance, ExtensionContext extensionContext) {
		this.resolverFacade.resolveAndInjectFields(testInstance, extensionContext, this.arguments, this.invocationIndex,
			this.resolutionCache);
	}
}
