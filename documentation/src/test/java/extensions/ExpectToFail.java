/*
 * Copyright 2015-2026 the original author or authors.
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v2.0 which
 * accompanies this distribution and is available at
 *
 * https://www.eclipse.org/legal/epl-v20.html
 */

package extensions;

import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.junit.jupiter.api.extension.AfterEachCallback;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.ExtensionContext.Namespace;
import org.junit.jupiter.api.extension.ExtensionContext.Store;
import org.junit.jupiter.api.extension.TestExecutionExceptionHandler;

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@ExtendWith(ExpectToFail.Extension.class)
public @interface ExpectToFail {

	class Extension implements TestExecutionExceptionHandler, AfterEachCallback {

		private static final String KEY = "exception";

		@Override
		public void handleTestExecutionException(ExtensionContext context, Throwable throwable) throws Throwable {
			getExceptionStore(context).put(KEY, throwable);
		}

		@Override
		public void afterEach(ExtensionContext context) throws Exception {
			assertNotNull(getExceptionStore(context).get(KEY), "Test should have failed");
		}

		private Store getExceptionStore(ExtensionContext context) {
			return context.getStore(Namespace.create(getClass(), context.getRequiredTestMethod()));
		}
	}

}
