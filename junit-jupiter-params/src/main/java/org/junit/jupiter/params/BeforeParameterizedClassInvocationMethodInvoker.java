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

import org.junit.jupiter.api.extension.BeforeClassTemplateInvocationCallback;
import org.junit.jupiter.api.extension.ExtensionContext;

/**
 * @since 5.13
 */
class BeforeParameterizedClassInvocationMethodInvoker extends AbstractParameterizedClassInvocationLifecycleMethodInvoker
		implements BeforeClassTemplateInvocationCallback {

	BeforeParameterizedClassInvocationMethodInvoker(ParameterizedClassContext declarationContext,
			EvaluatedArgumentSet arguments, int invocationIndex, ResolutionCache resolutionCache,
			ArgumentSetLifecycleMethod lifecycleMethod) {
		super(declarationContext, arguments, invocationIndex, resolutionCache, lifecycleMethod);
	}

	@Override
	public void beforeClassTemplateInvocation(ExtensionContext context) {
		invoke(context);
	}

}
