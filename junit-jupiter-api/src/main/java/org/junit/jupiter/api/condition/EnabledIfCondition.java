/*
 * Copyright 2015-2026 the original author or authors.
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v2.0 which
 * accompanies this distribution and is available at
 *
 * https://www.eclipse.org/legal/epl-v20.html
 */

package org.junit.jupiter.api.condition;

import org.junit.jupiter.api.extension.ExecutionCondition;

/**
 * {@link ExecutionCondition} for {@link EnabledIf @EnabledIf}.
 *
 * @since 5.7
 * @see EnabledIf
 */
class EnabledIfCondition extends MethodBasedCondition<EnabledIf> {

	EnabledIfCondition() {
		super(EnabledIf.class, EnabledIf::value, EnabledIf::disabledReason);
	}

	@Override
	protected boolean isEnabled(boolean methodResult) {
		return methodResult;
	}

}
