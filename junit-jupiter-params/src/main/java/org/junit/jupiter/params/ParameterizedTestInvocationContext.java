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

import java.util.List;

import org.junit.jupiter.api.extension.Extension;
import org.junit.jupiter.api.extension.TestTemplateInvocationContext;
import org.junit.jupiter.params.provider.Arguments;

/**
 * @since 5.0
 */
class ParameterizedTestInvocationContext extends ParameterizedInvocationContext<ParameterizedTestContext>
		implements TestTemplateInvocationContext {

	ParameterizedTestInvocationContext(ParameterizedTestContext methodContext,
			ParameterizedInvocationNameFormatter formatter, Arguments arguments, int invocationIndex) {
		super(methodContext, formatter, arguments, invocationIndex);
	}

	@Override
	public List<Extension> getAdditionalExtensions() {
		return List.of( //
			new ParameterizedTestMethodParameterResolver(this.declarationContext, this.arguments, this.invocationIndex) //
		);
	}

}
