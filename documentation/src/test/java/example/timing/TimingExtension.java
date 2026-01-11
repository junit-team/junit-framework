/*
 * Copyright 2015-2026 the original author or authors.
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v2.0 which
 * accompanies this distribution and is available at
 *
 * https://www.eclipse.org/legal/epl-v20.html
 */

package example.timing;

// tag::user_guide[]
import java.lang.reflect.Method;
import java.util.logging.Logger;

import org.junit.jupiter.api.extension.AfterTestExecutionCallback;
import org.junit.jupiter.api.extension.BeforeTestExecutionCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.ExtensionContext.Namespace;
import org.junit.jupiter.api.extension.ExtensionContext.Store;

// end::user_guide[]
/**
 * Simple extension that <em>times</em> the execution of test methods and
 * logs the results at {@code INFO} level.
 *
 * @since 5.0
 */
// @formatter:off
// tag::user_guide[]
public class TimingExtension implements BeforeTestExecutionCallback, AfterTestExecutionCallback {

	private static final Logger logger = Logger.getLogger(TimingExtension.class.getName());

	private static final String START_TIME = "start time";

	@Override
	public void beforeTestExecution(ExtensionContext context) {
		getStore(context).put(START_TIME, System.currentTimeMillis());
	}

	//end::user_guide[]
	@SuppressWarnings("DataFlowIssue")
	//tag::user_guide[]
	@Override
	public void afterTestExecution(ExtensionContext context) {
		Method testMethod = context.getRequiredTestMethod();
		long startTime = getStore(context).remove(START_TIME, long.class);
		long duration = System.currentTimeMillis() - startTime;

		logger.info(() ->
			"Method [%s] took %s ms.".formatted(testMethod.getName(), duration));
	}

	private Store getStore(ExtensionContext context) {
		return context.getStore(Namespace.create(getClass(), context.getRequiredTestMethod()));
	}

}
// end::user_guide[]
// @formatter:on
